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

package org.jetbrains.kotlin.idea.util

import com.google.common.collect.Sets
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil.findFilesWithExactPackage
import org.jetbrains.kotlin.idea.stubindex.StaticFacadeIndexUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CompositeBindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.jetbrains.kotlin.utils.addToStdlib.check
import java.util.*

object DebuggerUtils {

    private val KOTLIN_EXTENSIONS = Sets.newHashSet("kt", "kts")

    fun findSourceFileForClass(
            project: Project,
            searchScope: GlobalSearchScope,
            className: JvmClassName,
            fileName: String): KtFile? {
        val extension = FileUtilRt.getExtension(fileName)
        if (!KOTLIN_EXTENSIONS.contains(extension)) return null
        if (DumbService.getInstance(project).isDumb) return null

        val filesWithExactName = findFilesByNameInPackage(className, fileName, project, searchScope).check { it.isNotEmpty() }
                                 // Source files for libraries aren't included into ModuleWithDependencies scope
                                 ?: findFilesByNameInPackage(
                                            className, fileName, project,
                                            KotlinSourceFilterScope.librarySources(GlobalSearchScope.allScope(project), project))

        if (filesWithExactName.isEmpty()) return null

        if (filesWithExactName.size == 1) {
            return filesWithExactName.single()
        }

        // Static facade or inner class of such facade?
        val partFqName = className.fqNameForClassNameWithoutDollars
        val filesForPart = StaticFacadeIndexUtil.findFilesForFilePart(partFqName, searchScope, project)
        if (!filesForPart.isEmpty()) {
            for (file in filesForPart) {
                if (file.name == fileName) {
                    return file
                }
            }
            // Do not fall back to decompiled files (which have different name).
            return null
        }

        return filesWithExactName.first()
    }

    private fun findFilesByNameInPackage(className: JvmClassName, fileName: String, project: Project, searchScope: GlobalSearchScope)
            = findFilesWithExactPackage(className.packageFqName, searchScope, project).filter { it.name == fileName }

    fun analyzeInlinedFunctions(
            resolutionFacadeForFile: ResolutionFacade,
            bindingContextForFile: BindingContext,
            file: KtFile,
            analyzeOnlyReifiedInlineFunctions: Boolean
    ): Pair<BindingContext, List<KtFile>> {
        val analyzedElements = HashSet<KtElement>()
        val context = analyzeElementWithInline(
                resolutionFacadeForFile,
                bindingContextForFile,
                file,
                1,
                analyzedElements,
                !analyzeOnlyReifiedInlineFunctions)

        //We processing another files just to annotate anonymous classes within their inline functions
        //Bytecode not produced for them cause of filtering via generateClassFilter
        val toProcess = LinkedHashSet<KtFile>()
        toProcess.add(file)

        for (collectedElement in analyzedElements) {
            val containingFile = collectedElement.getContainingKtFile()
            toProcess.add(containingFile)
        }

        return Pair<BindingContext, List<KtFile>>(context, ArrayList(toProcess))
    }

    fun analyzeElementWithInline(
            resolutionFacade: ResolutionFacade,
            bindingContext: BindingContext,
            function: KtNamedFunction,
            analyzeInlineFunctions: Boolean): Collection<KtElement> {
        val analyzedElements = HashSet<KtElement>()
        analyzeElementWithInline(resolutionFacade, bindingContext, function, 1, analyzedElements, !analyzeInlineFunctions)
        return analyzedElements
    }

    private fun analyzeElementWithInline(
            resolutionFacade: ResolutionFacade,
            bindingContext: BindingContext,
            element: KtElement,
            deep: Int,
            analyzedElements: MutableSet<KtElement>,
            analyzeInlineFunctions: Boolean): BindingContext {
        val project = element.project
        val collectedElements = HashSet<KtNamedFunction>()

        element.accept(object : KtTreeVisitorVoid() {
            override fun visitExpression(expression: KtExpression) {
                super.visitExpression(expression)

                val call = bindingContext.get(BindingContext.CALL, expression) ?: return

                val resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, call)
                checkResolveCall(resolvedCall)
            }

            override fun visitMultiDeclaration(multiDeclaration: KtMultiDeclaration) {
                super.visitMultiDeclaration(multiDeclaration)

                for (entry in multiDeclaration.entries) {
                    val resolvedCall = bindingContext.get(BindingContext.COMPONENT_RESOLVED_CALL, entry)
                    checkResolveCall(resolvedCall)
                }
            }

            override fun visitForExpression(expression: KtForExpression) {
                super.visitForExpression(expression)

                checkResolveCall(bindingContext.get(BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL, expression.loopRange))
                checkResolveCall(bindingContext.get(BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL, expression.loopRange))
                checkResolveCall(bindingContext.get(BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL, expression.loopRange))
            }

            private fun checkResolveCall(resolvedCall: ResolvedCall<*>?) {
                if (resolvedCall == null) return

                val descriptor = resolvedCall.resultingDescriptor
                if (descriptor is DeserializedSimpleFunctionDescriptor) return

                if (InlineUtil.isInline(descriptor) && (analyzeInlineFunctions || hasReifiedTypeParameters(descriptor))) {
                    val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor)
                    if (declaration != null && declaration is KtNamedFunction && !analyzedElements.contains(declaration)) {
                        collectedElements.add(declaration)
                    }
                }
            }
        })

        analyzedElements.add(element)

        if (!collectedElements.isEmpty() && deep < 10) {
            val innerContexts = ArrayList<BindingContext>()
            for (inlineFunctions in collectedElements) {
                val body = inlineFunctions.bodyExpression
                if (body != null) {
                    val bindingContextForFunction = resolutionFacade.analyze(body, BodyResolveMode.FULL)
                    innerContexts.add(analyzeElementWithInline(resolutionFacade, bindingContextForFunction, inlineFunctions, deep + 1,
                                                               analyzedElements, analyzeInlineFunctions))
                }
            }

            innerContexts.add(bindingContext)

            analyzedElements.addAll(collectedElements)
            return CompositeBindingContext.create(innerContexts)
        }

        return bindingContext
    }

    private fun hasReifiedTypeParameters(descriptor: CallableDescriptor): Boolean {
        return descriptor.typeParameters.any() { it.isReified }
    }
}
