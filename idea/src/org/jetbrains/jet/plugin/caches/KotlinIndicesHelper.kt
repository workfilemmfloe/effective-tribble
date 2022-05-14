/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.caches

import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.plugin.stubindex.*
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.psi.psiUtil.getReceiverExpression
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import com.intellij.openapi.project.Project
import java.util.HashSet
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.jet.lang.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.jet.lang.resolve.QualifiedExpressionResolver.LookupMode

public class KotlinIndicesHelper(private val project: Project) {
    public fun getTopLevelObjects(nameFilter: (String) -> Boolean, resolveSession: ResolveSessionForBodies, scope: GlobalSearchScope): Collection<ClassDescriptor> {
        val allObjectNames = JetTopLevelObjectShortNameIndex.getInstance().getAllKeys(project).stream() +
                JetFromJavaDescriptorHelper.getPossiblePackageDeclarationsNames(project, scope).stream()
        return allObjectNames
                .filter(nameFilter)
                .toSet()
                .flatMap { getTopLevelObjectsByName(it, resolveSession, scope) }
    }

    private fun getTopLevelObjectsByName(name: String, resolveSession: ResolveSessionForBodies, scope: GlobalSearchScope): Collection<ClassDescriptor> {
        val result = hashSetOf<ClassDescriptor>()

        val topObjects = JetTopLevelObjectShortNameIndex.getInstance().get(name, project, scope)
        for (objectDeclaration in topObjects) {
            val fqName = objectDeclaration.getFqName() ?: error("Local object declaration in JetTopLevelShortObjectNameIndex:${objectDeclaration.getText()}")
            result.addAll(ResolveSessionUtils.getClassOrObjectDescriptorsByFqName(resolveSession.getModuleDescriptor(), fqName, ResolveSessionUtils.SINGLETON_FILTER))
        }

        for (psiClass in JetFromJavaDescriptorHelper.getCompiledClassesForTopLevelObjects(project, scope)) {
            val qualifiedName = psiClass.getQualifiedName()
            if (qualifiedName != null) {
                result.addAll(ResolveSessionUtils.getClassOrObjectDescriptorsByFqName(resolveSession.getModuleDescriptor(), FqName(qualifiedName), ResolveSessionUtils.SINGLETON_FILTER))
            }
        }

        return result
    }

    public fun getTopLevelCallablesByName(name: String, context: JetExpression /*TODO: to be dropped*/, resolveSession: ResolveSessionForBodies, scope: GlobalSearchScope): Collection<CallableDescriptor> {
        val jetScope = resolveSession.resolveToElement(context).get(BindingContext.RESOLUTION_SCOPE, context) ?: return listOf()

        val result = HashSet<CallableDescriptor>()

        //TODO: this code is temporary and is to be dropped when compiled top level functions are indexed
        val identifier = Name.identifier(name)
        for (fqName in JetFromJavaDescriptorHelper.getTopLevelCallableFqNames(project, scope, false)) {
            if (fqName.lastSegmentIs(identifier)) {
                result.addAll(findTopLevelCallables(fqName, context, jetScope, resolveSession))
            }
        }

        result.addSourceTopLevelFunctions(name, resolveSession, scope)
        result.addSourceTopLevelProperties(name, resolveSession, scope)

        return result
    }

    private fun MutableCollection<in FunctionDescriptor>.addSourceTopLevelFunctions(name: String, resolveSession: ResolveSessionForBodies, scope: GlobalSearchScope) {
        val identifier = Name.identifier(name)
        val affectedPackages = JetTopLevelNonExtensionFunctionShortNameIndex.getInstance().get(name, project, scope)
                .stream()
                .map { it.getContainingFile() }
                .filterIsInstance(javaClass<JetFile>())
                .map { it.getPackageFqName() }
                .toSet()

        for (affectedPackage in affectedPackages) {
            val packageDescriptor = resolveSession.getModuleDescriptor().getPackage(affectedPackage)
                    ?: error("There's a function in stub index with invalid package: $affectedPackage")
            addAll(packageDescriptor.getMemberScope().getFunctions(identifier))
        }
    }

    private fun MutableCollection<in PropertyDescriptor>.addSourceTopLevelProperties(name: String, resolveSession: ResolveSessionForBodies, scope: GlobalSearchScope) {
        val identifier = Name.identifier(name)
        val affectedPackages = JetTopLevelNonExtensionPropertyShortNameIndex.getInstance().get(name, project, scope)
                .stream()
                .map { it.getContainingFile() }
                .filterIsInstance(javaClass<JetFile>())
                .map { it.getPackageFqName() }
                .toSet()

        for (affectedPackage in affectedPackages) {
            val packageDescriptor = resolveSession.getModuleDescriptor().getPackage(affectedPackage)
                    ?: error("There's a property in stub index with invalid package: $affectedPackage")
            addAll(packageDescriptor.getMemberScope().getProperties(identifier))
        }
    }

    public fun getTopLevelCallables(nameFilter: (String) -> Boolean, context: JetExpression /*TODO: to be dropped*/, resolveSession: ResolveSessionForBodies, scope: GlobalSearchScope): Collection<CallableDescriptor> {
        val sourceNames = JetTopLevelFunctionsFqnNameIndex.getInstance().getAllKeys(project).stream() + JetTopLevelPropertiesFqnNameIndex.getInstance().getAllKeys(project).stream()
        val allFqNames = sourceNames.map { FqName(it) } + JetFromJavaDescriptorHelper.getTopLevelCallableFqNames(project, scope, false).stream()

        val jetScope = resolveSession.resolveToElement(context).get(BindingContext.RESOLUTION_SCOPE, context) ?: return listOf()

        return allFqNames.filter { nameFilter(it.shortName().asString()) }
                .toSet()
                .flatMap { findTopLevelCallables(it, context, jetScope, resolveSession) }
    }

    public fun getCallableExtensions(nameFilter: (String) -> Boolean,
                                     expression: JetSimpleNameExpression,
                                     resolveSession: ResolveSessionForBodies,
                                     scope: GlobalSearchScope): Collection<CallableDescriptor> {
        val context = resolveSession.resolveToElement(expression)
        val receiverExpression = expression.getReceiverExpression() ?: return listOf()
        val expressionType = context.get<JetExpression, JetType>(BindingContext.EXPRESSION_TYPE, receiverExpression)
        val jetScope = context.get(BindingContext.RESOLUTION_SCOPE, receiverExpression)

        if (expressionType == null || jetScope == null || expressionType.isError()) {
            return listOf()
        }

        val sourceNames = JetTopLevelFunctionsFqnNameIndex.getInstance().getAllKeys(project).stream() + JetTopLevelPropertiesFqnNameIndex.getInstance().getAllKeys(project).stream()
        val allFqNames = sourceNames.map { FqName(it) } + JetFromJavaDescriptorHelper.getTopLevelCallableFqNames(project, scope, true).stream()

        // Iterate through the function with attempt to resolve found functions
        return allFqNames
                .filter { nameFilter(it.shortName().asString()) }
                .toSet()
                .flatMap { findSuitableExtensions(it, receiverExpression, expressionType, jetScope, resolveSession.getModuleDescriptor(), context) }
    }

    /**
     * Check that function or property with the given qualified name can be resolved in given scope and called on given receiver
     */
    private fun findSuitableExtensions(callableFQN: FqName,
                                    receiverExpression: JetExpression,
                                    receiverType: JetType,
                                    scope: JetScope,
                                    module: ModuleDescriptor,
                                    bindingContext: BindingContext): List<CallableDescriptor> {
        val importDirective = JetPsiFactory(receiverExpression).createImportDirective(callableFQN.asString())
        val declarationDescriptors = analyzeImportReference(importDirective, scope, BindingTraceContext(), module)

        val receiverValue = ExpressionReceiver(receiverExpression, receiverType)
        val dataFlowInfo = bindingContext.getDataFlowInfo(receiverExpression)

        return declarationDescriptors
                .filterIsInstance(javaClass<CallableDescriptor>())
                .filter { it.getReceiverParameter() != null && ExpressionTypingUtils.checkIsExtensionCallable(receiverValue, it, bindingContext, dataFlowInfo) }
    }

    public fun getClassDescriptors(nameFilter: (String) -> Boolean, analyzer: KotlinCodeAnalyzer, scope: GlobalSearchScope): Collection<ClassDescriptor> {
        return JetFullClassNameIndex.getInstance().getAllKeys(project).stream()
                .map { FqName(it) }
                .filter { nameFilter(it.shortName().asString()) }
                .toList()
                .flatMap { getClassDescriptorsByFQName(analyzer, it, scope) }
    }

    private fun getClassDescriptorsByFQName(analyzer: KotlinCodeAnalyzer, classFQName: FqName, scope: GlobalSearchScope): Collection<ClassDescriptor> {
        val jetClassOrObjects = JetFullClassNameIndex.getInstance().get(classFQName.asString(), project, scope)

        if (jetClassOrObjects.isEmpty()) {
            // This fqn is absent in caches, dead or not in scope
            return listOf()
        }

        // Note: Can't search with psi element as analyzer could be built over temp files
        return ResolveSessionUtils.getClassDescriptorsByFqName(analyzer.getModuleDescriptor(), classFQName)
    }

    private fun findTopLevelCallables(fqName: FqName, context: JetExpression, jetScope: JetScope, resolveSession: ResolveSessionForBodies): Collection<CallableDescriptor> {
        val importDirective = JetPsiFactory(context.getProject()).createImportDirective(ImportPath(fqName, false))
        val allDescriptors = analyzeImportReference(importDirective, jetScope, BindingTraceContext(), resolveSession.getModuleDescriptor())
        return allDescriptors.filterIsInstance(javaClass<CallableDescriptor>()).filter { it.getReceiverParameter() == null }
    }

    private fun analyzeImportReference(
            importDirective: JetImportDirective, scope: JetScope, trace: BindingTrace, module: ModuleDescriptor
    ): Collection<DeclarationDescriptor> {
        return QualifiedExpressionResolver().processImportReference(importDirective, scope, scope, Importer.DO_NOTHING, trace,
                                                                    module, LookupMode.EVERYTHING)
    }
}