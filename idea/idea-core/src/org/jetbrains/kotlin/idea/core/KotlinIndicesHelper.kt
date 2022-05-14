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

package org.jetbrains.kotlin.idea.core

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.idea.stubindex.*
import org.jetbrains.kotlin.idea.util.CallType
import org.jetbrains.kotlin.idea.util.getImplicitReceiversWithInstance
import org.jetbrains.kotlin.idea.util.substituteExtensionIfCallable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver.LookupMode
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastUtils
import org.jetbrains.kotlin.resolve.lazy.ResolveSessionUtils
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.util.HashSet
import java.util.LinkedHashSet

public class KotlinIndicesHelper(
        private val project: Project,
        private val resolutionFacade: ResolutionFacade,
        private val bindingContext: BindingContext,
        private val scope: GlobalSearchScope,
        private val moduleDescriptor: ModuleDescriptor,
        private val visibilityFilter: (DeclarationDescriptor) -> Boolean
) {
    public fun getTopLevelCallablesByName(name: String): Collection<CallableDescriptor> {
        val declarations = HashSet<JetNamedDeclaration>()
        declarations.addTopLevelNonExtensionCallablesByName(JetFunctionShortNameIndex.getInstance(), name)
        declarations.addTopLevelNonExtensionCallablesByName(JetPropertyShortNameIndex.getInstance(), name)
        return declarations.flatMap {
            if (it.getContainingJetFile().isCompiled()) { //TODO: it's temporary while resolveToDescriptor does not work for compiled declarations
                analyzeImportReference(it.getFqName()!!).filterIsInstance<CallableDescriptor>()
            }
            else {
                (resolutionFacade.resolveToDescriptor(it) as? CallableDescriptor).singletonOrEmptyList()
            }
        }.filter { it.getExtensionReceiverParameter() == null && visibilityFilter(it) }
    }

    private fun MutableSet<JetNamedDeclaration>.addTopLevelNonExtensionCallablesByName(
            index: StringStubIndexExtension<out JetCallableDeclaration>,
            name: String
    ) {
        index.get(name, project, scope).filterTo(this) { it.getParent() is JetFile && it.getReceiverTypeReference() == null }
    }

    public fun getTopLevelCallables(nameFilter: (String) -> Boolean): Collection<CallableDescriptor> {
        return (JetTopLevelFunctionFqnNameIndex.getInstance().getAllKeys(project).sequence() +
                    JetTopLevelPropertyFqnNameIndex.getInstance().getAllKeys(project).sequence())
                .map { FqName(it) }
                .filter { nameFilter(it.shortName().asString()) }
                .toSet()
                .flatMap { findTopLevelCallables(it).filter(visibilityFilter) }
    }

    public fun getCallableTopLevelExtensions(nameFilter: (String) -> Boolean, expression: JetSimpleNameExpression): Collection<CallableDescriptor> {
        val receiverValues = receiverValues(expression)
        if (receiverValues.isEmpty()) return emptyList()

        val dataFlowInfo = bindingContext.getDataFlowInfo(expression)

        val receiverTypeNames = possibleReceiverTypeNames(receiverValues.map { it.first }, dataFlowInfo)

        val index = JetTopLevelExtensionsByReceiverTypeIndex.INSTANCE

        val declarations = index.getAllKeys(project)
                .sequence()
                .filter {
                    JetTopLevelExtensionsByReceiverTypeIndex.receiverTypeNameFromKey(it) in receiverTypeNames
                    && nameFilter(JetTopLevelExtensionsByReceiverTypeIndex.callableNameFromKey(it))
                }
                .flatMap { index.get(it, project, scope).sequence() }

        return findSuitableExtensions(declarations, receiverValues, dataFlowInfo, bindingContext)
    }

    private fun possibleReceiverTypeNames(receiverValues: Collection<ReceiverValue>, dataFlowInfo: DataFlowInfo): Set<String> {
        val result = HashSet<String>()
        for (receiverValue in receiverValues) {
            for (type in SmartCastUtils.getSmartCastVariants(receiverValue, bindingContext, moduleDescriptor, dataFlowInfo)) {
                result.addTypeNames(type)
            }
        }
        return result
    }

    private fun MutableCollection<String>.addTypeNames(type: JetType) {
        val constructor = type.getConstructor()
        addIfNotNull(constructor.getDeclarationDescriptor()?.getName()?.asString())
        constructor.getSupertypes().forEach { addTypeNames(it) }
    }

    private fun receiverValues(expression: JetSimpleNameExpression): Collection<Pair<ReceiverValue, CallType>> {
        val receiverPair = ReferenceVariantsHelper.getExplicitReceiverData(expression)
        if (receiverPair != null) {
            val (receiverExpression, callType) = receiverPair

            val expressionType = bindingContext.getType(receiverExpression)
            if (expressionType == null || expressionType.isError()) return emptyList()

            val receiverValue = ExpressionReceiver(receiverExpression, expressionType)

            return listOf(receiverValue to callType)
        }
        else {
            val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, expression] ?: return emptyList()
            return resolutionScope.getImplicitReceiversWithInstance().map { it.getValue() to CallType.NORMAL }
        }
    }

    /**
     * Check that function or property with the given qualified name can be resolved in given scope and called on given receiver
     */
    private fun findSuitableExtensions(
            declarations: Sequence<JetCallableDeclaration>,
            receiverValues: Collection<Pair<ReceiverValue, CallType>>,
            dataFlowInfo: DataFlowInfo,
            bindingContext: BindingContext
    ): Collection<CallableDescriptor> {
        val result = LinkedHashSet<CallableDescriptor>()

        fun processDescriptor(descriptor: CallableDescriptor) {
            if (visibilityFilter(descriptor)) {
                for ((receiverValue, callType) in receiverValues) {
                    result.addAll(descriptor.substituteExtensionIfCallable(receiverValue, callType, bindingContext, dataFlowInfo, moduleDescriptor))
                }
            }
        }

        for (declaration in declarations) {
            if (declaration.getContainingJetFile().isCompiled()) {
                //TODO: it's temporary while resolveToDescriptor does not work for compiled declarations
                for (descriptor in analyzeImportReference(declaration.getFqName()!!)) {
                    if (descriptor is CallableDescriptor && descriptor.getExtensionReceiverParameter() != null) {
                        processDescriptor(descriptor)
                    }
                }
            }
            else {
                processDescriptor(resolutionFacade.resolveToDescriptor(declaration) as CallableDescriptor)
            }
        }

        return result
    }

    public fun getClassDescriptors(nameFilter: (String) -> Boolean, kindFilter: (ClassKind) -> Boolean): Collection<ClassDescriptor> {
        return JetFullClassNameIndex.getInstance().getAllKeys(project).sequence()
                .map { FqName(it) }
                .filter { nameFilter(it.shortName().asString()) }
                .toList()
                .flatMap { getClassDescriptorsByFQName(it, kindFilter) }
    }

    private fun getClassDescriptorsByFQName(classFQName: FqName, kindFilter: (ClassKind) -> Boolean): Collection<ClassDescriptor> {
        val declarations = JetFullClassNameIndex.getInstance()[classFQName.asString(), project, scope]

        if (declarations.isEmpty()) {
            // This fqn is absent in caches, dead or not in scope
            return emptyList()
        }

        // Note: Can't search with psi element as analyzer could be built over temp files
        return ResolveSessionUtils.getClassOrObjectDescriptorsByFqName(moduleDescriptor, classFQName) { kindFilter(it.getKind()) }
                .filter(visibilityFilter)
    }

    private fun findTopLevelCallables(fqName: FqName): Collection<CallableDescriptor> {
        return analyzeImportReference(fqName)
                .filterIsInstance<CallableDescriptor>()
                .filter { it.getExtensionReceiverParameter() == null }
    }

    private fun analyzeImportReference(fqName: FqName): Collection<DeclarationDescriptor> {
        val importDirective = JetPsiFactory(project).createImportDirective(ImportPath(fqName, false))
        val scope = JetModuleUtil.getSubpackagesOfRootScope(moduleDescriptor)
        return QualifiedExpressionResolver().processImportReference(importDirective, scope, scope, BindingTraceContext(), LookupMode.EVERYTHING).getAllDescriptors()
    }
}
