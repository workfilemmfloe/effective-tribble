/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.NoDataException
import com.intellij.debugger.PositionManager
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.requests.ClassPrepareRequestor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.libraries.LibraryUtil
import com.intellij.openapi.util.Pair
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.*
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.Location
import com.sun.jdi.ReferenceType
import com.sun.jdi.request.ClassPrepareRequest
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.JetTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.*
import org.jetbrains.kotlin.resolve.extension.InlineAnalyzerExtension
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.builtins.InlineUtil
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.util.DebuggerUtils

import java.util.*

import org.jetbrains.kotlin.codegen.binding.CodegenBinding.asmTypeForAnonymousClass
import org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil
import org.jetbrains.kotlin.idea.util.application.runReadAction

public class JetPositionManager(private val myDebugProcess: DebugProcess) : PositionManager {
    private val myTypeMappers = WeakHashMap<Pair<FqName, IdeaModuleInfo>, CachedValue<JetTypeMapper>>()

    override fun getSourcePosition(location: Location?): SourcePosition? {
        if (location == null) {
            throw NoDataException()
        }
        val psiFile = getPsiFileByLocation(location)
        if (psiFile == null) {
            throw NoDataException()
        }

        val lineNumber = try {
            location.lineNumber() - 1
        }
        catch (e: InternalError) {
            -1
        }


        if (lineNumber >= 0) {
            val lambdaIfInside = getLambdaIfInside(location, psiFile as JetFile, lineNumber)
            if (lambdaIfInside != null) {
                return SourcePosition.createFromElement(lambdaIfInside.getBodyExpression().getStatements().get(0))
            }
            return SourcePosition.createFromLine(psiFile, lineNumber)
        }

        throw NoDataException()
    }

    private fun getLambdaIfInside(location: Location, file: JetFile, lineNumber: Int): JetFunctionLiteral? {
        val currentLocationFqName = location.declaringType().name()
        if (currentLocationFqName == null) return null

        val start = CodeInsightUtils.getStartLineOffset(file, lineNumber)
        val end = CodeInsightUtils.getEndLineOffset(file, lineNumber)
        if (start == null || end == null) return null

        val literals = CodeInsightUtils.findElementsOfClassInRange(file, start, end, javaClass<JetFunctionLiteral>())
        if (literals == null || literals.size() == 0) return null

        val isInLibrary = LibraryUtil.findLibraryEntry(file.getVirtualFile(), file.getProject()) != null
        val typeMapper = if (!isInLibrary)
            prepareTypeMapper(file)
        else
            createTypeMapperForLibraryFile(file.findElementAt(start), file)

        val currentLocationClassName = JvmClassName.byFqNameWithoutInnerClasses(FqName(currentLocationFqName)).getInternalName()
        for (literal in literals) {
            val functionLiteral = literal as JetFunctionLiteral
            if (isInlinedLambda(functionLiteral, typeMapper.getBindingContext())) {
                continue
            }

            val internalClassName = getClassNameForElement(literal.getFirstChild(), typeMapper, file, isInLibrary)
            if (internalClassName == currentLocationClassName) {
                return functionLiteral
            }
        }

        return null
    }

    private fun getPsiFileByLocation(location: Location): PsiFile? {
        val sourceName: String
        try {
            sourceName = location.sourceName()
        }
        catch (e: AbsentInformationException) {
            return null
        }


        // JDI names are of form "package.Class$InnerClass"
        val referenceFqName = location.declaringType().name()
        val referenceInternalName = referenceFqName.replace('.', '/')
        val className = JvmClassName.byInternalName(referenceInternalName)

        val project = myDebugProcess.getProject()

        if (DumbService.getInstance(project).isDumb()) return null

        return DebuggerUtils.findSourceFileForClass(project, GlobalSearchScope.allScope(project), className, sourceName, location.lineNumber() - 1)
    }

    override fun getAllClasses(sourcePosition: SourcePosition): List<ReferenceType> {
        if (sourcePosition.getFile() !is JetFile) {
            throw NoDataException()
        }
        val name = classNameForPosition(sourcePosition)
        val result = ArrayList<ReferenceType>()
        if (name != null) {
            result.addAll(myDebugProcess.getVirtualMachineProxy().classesByName(name))
        }
        return result
    }

    private fun classNameForPosition(sourcePosition: SourcePosition): String? {
        return runReadAction {
            val file = sourcePosition.getFile() as JetFile
            val isInLibrary = LibraryUtil.findLibraryEntry(file.getVirtualFile(), file.getProject()) != null
            val typeMapper = if (!isInLibrary) prepareTypeMapper(file) else createTypeMapperForLibraryFile(sourcePosition.getElementAt(), file)
            getClassNameForElement(sourcePosition.getElementAt(), typeMapper, file, isInLibrary)
        }
    }

    private fun prepareTypeMapper(file: JetFile): JetTypeMapper {
        val key = createKeyForTypeMapper(file)

        var value: CachedValue<JetTypeMapper>? = myTypeMappers.get(key)
        if (value == null) {
            value = CachedValuesManager.getManager(file.getProject()).createCachedValue<JetTypeMapper>(
                    {() ->
                        val typeMapper = createTypeMapper(file, key.second)
                        CachedValueProvider.Result<JetTypeMapper>(typeMapper, PsiModificationTracker.MODIFICATION_COUNT)
                    }, false)

            myTypeMappers.put(key, value)
        }

        return value!!.getValue()
    }

    override fun locationsOfLine(type: ReferenceType, position: SourcePosition): List<Location> {
        if (position.getFile() !is JetFile) {
            throw NoDataException()
        }
        try {
            val line = position.getLine() + 1
            val locations = if (myDebugProcess.getVirtualMachineProxy().versionHigher("1.4"))
                type.locationsOfLine(DebugProcess.JAVA_STRATUM, null, line)
            else
                type.locationsOfLine(line)
            if (locations == null || locations.isEmpty()) throw NoDataException()
            return locations
        }
        catch (e: AbsentInformationException) {
            throw NoDataException()
        }
    }

    override fun createPrepareRequest(classPrepareRequestor: ClassPrepareRequestor, sourcePosition: SourcePosition): ClassPrepareRequest? {
        if (sourcePosition.getFile() !is JetFile) {
            throw NoDataException()
        }
        val className = classNameForPosition(sourcePosition)
        if (className == null) {
            return null
        }
        return myDebugProcess.getRequestsManager().createClassPrepareRequest(classPrepareRequestor, className.replace('/', '.'))
    }

    TestOnly
    public fun addTypeMapper(file: JetFile, typeMapper: JetTypeMapper) {
        val value = CachedValuesManager.getManager(file.getProject()).createCachedValue<JetTypeMapper>(
                { () -> CachedValueProvider.Result<JetTypeMapper>(typeMapper, PsiModificationTracker.MODIFICATION_COUNT) }, false)
        val key = createKeyForTypeMapper(file)
        myTypeMappers.put(key, value)
    }

    class object {
        public fun createTypeMapper(file: JetFile, moduleInfo: IdeaModuleInfo): JetTypeMapper {
            val project = file.getProject()
            val packageFacadeScope = moduleInfo.contentScope()
            val packageFiles = PackageIndexUtil.findFilesWithExactPackage(file.getPackageFqName(), packageFacadeScope, project)

            val analysisResult = KotlinCacheService.getInstance(project).getAnalysisResults(packageFiles)
            analysisResult.throwIfError()

            val state = GenerationState(project, ClassBuilderFactories.THROW_EXCEPTION, analysisResult.moduleDescriptor,
                                        analysisResult.bindingContext, packageFiles.toList())
            state.beforeCompile()
            return state.getTypeMapper()
        }

        public fun getClassNameForElement(notPositionedElement: PsiElement?, typeMapper: JetTypeMapper, file: JetFile, isInLibrary: Boolean): String? {
            val element = getElementToCalculateClassName(notPositionedElement)
            when {
                element is JetClassOrObject -> return getJvmInternalNameForImpl(typeMapper, element)
                element is JetFunctionLiteral -> {
                    if (isInlinedLambda(element, typeMapper.getBindingContext())) {
                        return getClassNameForElement(element.getParent(), typeMapper, file, isInLibrary)
                    }
                    else {
                        val asmType = asmTypeForAnonymousClass(typeMapper.getBindingContext(), element)
                        return asmType.getInternalName()
                    }
                }
                element is JetClassInitializer -> {
                    val parent = getElementToCalculateClassName(element.getParent())
                    // Class-object initializer
                    if (parent is JetObjectDeclaration && parent.isClassObject()) {
                        return getClassNameForElement(parent.getParent(), typeMapper, file, isInLibrary)
                    }
                    return getClassNameForElement(element, typeMapper, file, isInLibrary)
                }
                element is JetProperty && (!element.isTopLevel() || !isInLibrary) -> {
                    if (isInPropertyAccessor(notPositionedElement)) {
                        val classOrObject = PsiTreeUtil.getParentOfType(element, javaClass<JetClassOrObject>())
                        if (classOrObject != null) {
                            return getJvmInternalNameForImpl(typeMapper, classOrObject)
                        }
                    }

                    val descriptor = typeMapper.getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, element)
                    if (descriptor !is PropertyDescriptor) {
                        return getClassNameForElement(element.getParent(), typeMapper, file, isInLibrary)
                    }

                    return getJvmInternalNameForPropertyOwner(typeMapper, descriptor)
                }
                element is JetNamedFunction -> {
                    val parent = getElementToCalculateClassName(element)
                    if (parent is JetClassOrObject) {
                        return getJvmInternalNameForImpl(typeMapper, parent)
                    }
                    else if (parent != null) {
                        val asmType = asmTypeForAnonymousClass(typeMapper.getBindingContext(), element)
                        return asmType.getInternalName()
                    }
                }
            }

            if (isInLibrary) {
                val elementAtForLibraryFile = getElementToCreateTypeMapperForLibraryFile(notPositionedElement)
                assert(elementAtForLibraryFile != null) {
                    "Couldn't find element at breakpoint for library file " + file.getName() +
                         (if (notPositionedElement == null) "" else ", notPositionedElement = " + JetPsiUtil.getElementTextWithContext(notPositionedElement))
                }
                return findPackagePartInternalNameForLibraryFile(elementAtForLibraryFile!!)
            }

            return PackagePartClassUtils.getPackagePartInternalName(file)
        }

        private fun getElementToCalculateClassName(notPositionedElement: PsiElement?) =
            PsiTreeUtil.getParentOfType(notPositionedElement,
                                        javaClass<JetClassOrObject>(),
                                        javaClass<JetFunctionLiteral>(),
                                        javaClass<JetNamedFunction>(),
                                        javaClass<JetProperty>(),
                                        javaClass<JetClassInitializer>())

        public fun getJvmInternalNameForPropertyOwner(typeMapper: JetTypeMapper, descriptor: PropertyDescriptor): String {
            return typeMapper.mapOwner(
                    if (AsmUtil.isPropertyWithBackingFieldInOuterClass(descriptor)) descriptor.getContainingDeclaration() else descriptor,
                    true).getInternalName()
        }

        private fun isInPropertyAccessor(element: PsiElement?) =
                element is JetPropertyAccessor ||
                PsiTreeUtil.getParentOfType(element, javaClass<JetProperty>(), javaClass<JetPropertyAccessor>()) is JetPropertyAccessor

        private fun getElementToCreateTypeMapperForLibraryFile(element: PsiElement?) =
                if (element is JetElement) element else PsiTreeUtil.getParentOfType(element, javaClass<JetElement>())

        private fun getJvmInternalNameForImpl(typeMapper: JetTypeMapper, jetClass: JetClassOrObject): String? {
            val classDescriptor = typeMapper.getBindingContext().get<PsiElement, ClassDescriptor>(BindingContext.CLASS, jetClass)
            if (classDescriptor == null) {
                return null
            }

            if (jetClass is JetClass && jetClass.isTrait()) {
                return typeMapper.mapTraitImpl(classDescriptor).getInternalName()
            }

            return typeMapper.mapClass(classDescriptor).getInternalName()
        }

        private fun createTypeMapperForLibraryFile(notPositionedElement: PsiElement?, file: JetFile): JetTypeMapper {
            val element = getElementToCreateTypeMapperForLibraryFile(notPositionedElement)
            val analysisResult = element!!.analyzeAndGetResult()

            val state = GenerationState(file.getProject(), ClassBuilderFactories.THROW_EXCEPTION,
                                        analysisResult.moduleDescriptor, analysisResult.bindingContext, listOf(file))
            state.beforeCompile()
            return state.getTypeMapper()
        }

        public fun isInlinedLambda(functionLiteral: JetFunctionLiteral, context: BindingContext): Boolean {
            val functionLiteralExpression = functionLiteral.getParent()
            if (functionLiteralExpression == null) return false

            var parent = functionLiteralExpression.getParent()

            var valueArgument: PsiElement = functionLiteralExpression
            while (parent is JetParenthesizedExpression || parent is JetBinaryExpressionWithTypeRHS || parent is JetLabeledExpression) {
                valueArgument = parent
                parent = parent.getParent()
            }

            while (parent is ValueArgument || parent is JetValueArgumentList) {
                parent = parent.getParent()
            }

            if (parent !is JetElement) return false

            val call = (parent as JetElement).getResolvedCall(context)
            if (call == null) return false

            val inlineType = InlineUtil.getInlineType(call.getResultingDescriptor())
            if (!inlineType.isInline()) return false

            for ((valueParameterDescriptor, resolvedValueArgument) in call.getValueArguments()) {
                for (next in resolvedValueArgument.getArguments()) {
                    val expression = next.getArgumentExpression()
                    if (valueArgument == expression) {
                        return InlineAnalyzerExtension.checkInlinableParameter(valueParameterDescriptor, expression, call.getResultingDescriptor(), null)
                    }
                }
            }
            return false
        }

        private fun createKeyForTypeMapper(file: JetFile) = Pair(file.getPackageFqName(), file.getModuleInfo())
    }
}
