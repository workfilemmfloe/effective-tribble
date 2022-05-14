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

package org.jetbrains.kotlin.resolve.lazy.descriptors

import com.google.common.collect.Sets
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.record
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.ScriptNameUtil
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.data.JetScriptInfo
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProvider
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.JetScopeImpl
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.storage.MemoizedFunctionToNotNull
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.toReadOnlyList
import java.util.*

public abstract class AbstractLazyMemberScope<D : DeclarationDescriptor, DP : DeclarationProvider>
protected constructor(
        protected val c: LazyClassContext,
        protected val declarationProvider: DP,
        protected val thisDescriptor: D,
        protected val trace: BindingTrace
) : JetScopeImpl() {

    protected val storageManager: StorageManager = c.storageManager
    private val classDescriptors: MemoizedFunctionToNotNull<Name, List<ClassDescriptor>> = storageManager.createMemoizedFunction { resolveClassDescriptor(it) }
    private val functionDescriptors: MemoizedFunctionToNotNull<Name, Collection<FunctionDescriptor>> = storageManager.createMemoizedFunction { doGetFunctions(it) }
    private val propertyDescriptors: MemoizedFunctionToNotNull<Name, Collection<VariableDescriptor>> = storageManager.createMemoizedFunction { doGetProperties(it) }

    private fun resolveClassDescriptor(name: Name): List<ClassDescriptor> {
        return declarationProvider.getClassOrObjectDeclarations(name).map {
            // SCRIPT: Creating a script class
            if (it is JetScriptInfo)
                LazyScriptClassDescriptor(c as ResolveSession, thisDescriptor, name, it)
            else
                LazyClassDescriptor(c, thisDescriptor, name, it)
        }.toReadOnlyList()
    }

    override fun getContainingDeclaration() = thisDescriptor

    override fun getClassifier(name: Name, location: LookupLocation): ClassDescriptor? {
        recordLookup(name, location)
        return classDescriptors(name).firstOrNull()
    }

    override fun getFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        recordLookup(name, location)
        return functionDescriptors(name)
    }

    private fun doGetFunctions(name: Name): Collection<FunctionDescriptor> {
        val result = Sets.newLinkedHashSet<FunctionDescriptor>()

        val declarations = declarationProvider.getFunctionDeclarations(name)
        for (functionDeclaration in declarations) {
            val resolutionScope = getScopeForMemberDeclarationResolution(functionDeclaration)
            result.add(c.functionDescriptorResolver.resolveFunctionDescriptor(
                    thisDescriptor,
                    resolutionScope,
                    functionDeclaration,
                    trace,
                    c.declarationScopeProvider.getOuterDataFlowInfoForDeclaration(functionDeclaration)))
        }

        getNonDeclaredFunctions(name, result)

        return result.toReadOnlyList()
    }

    protected abstract fun getScopeForMemberDeclarationResolution(declaration: JetDeclaration): LexicalScope

    protected abstract fun getNonDeclaredFunctions(name: Name, result: MutableSet<FunctionDescriptor>)

    override fun getProperties(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        recordLookup(name, location)
        return propertyDescriptors(name)
    }

    public fun doGetProperties(name: Name): Collection<VariableDescriptor> {
        val result = LinkedHashSet<VariableDescriptor>()

        val declarations = declarationProvider.getPropertyDeclarations(name)
        for (propertyDeclaration in declarations) {
            val resolutionScope = getScopeForMemberDeclarationResolution(propertyDeclaration)
            val propertyDescriptor = c.descriptorResolver.resolvePropertyDescriptor(
                    thisDescriptor,
                    resolutionScope,
                    propertyDeclaration,
                    trace,
                    c.declarationScopeProvider.getOuterDataFlowInfoForDeclaration(propertyDeclaration))
            result.add(propertyDescriptor)
        }

        getNonDeclaredProperties(name, result)

        return result.toReadOnlyList()
    }

    protected abstract fun getNonDeclaredProperties(name: Name, result: MutableSet<VariableDescriptor>)

    protected fun computeDescriptorsFromDeclaredElements(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            location: LookupLocation
    ): List<DeclarationDescriptor> {
        val declarations = declarationProvider.getDeclarations(kindFilter, nameFilter)
        val result = LinkedHashSet<DeclarationDescriptor>(declarations.size())
        for (declaration in declarations) {
            if (declaration is JetClassOrObject) {
                val name = declaration.nameAsSafeName
                if (nameFilter(name)) {
                    result.addAll(classDescriptors(name))
                }
            }
            else if (declaration is JetFunction) {
                val name = declaration.nameAsSafeName
                if (nameFilter(name)) {
                    result.addAll(getFunctions(name, location))
                }
            }
            else if (declaration is JetProperty) {
                val name = declaration.nameAsSafeName
                if (nameFilter(name)) {
                    result.addAll(getProperties(name, location))
                }
            }
            else if (declaration is JetParameter) {
                val name = declaration.nameAsSafeName
                if (nameFilter(name)) {
                    result.addAll(getProperties(name, location))
                }
            }
            else if (declaration is JetScript) {
                val name = ScriptNameUtil.classNameForScript(declaration).shortName()
                if (nameFilter(name)) {
                    result.addAll(classDescriptors(name))
                }
            }
            else if (declaration is JetTypedef || declaration is JetMultiDeclaration) {
                // Do nothing for typedefs as they are not supported.
                // MultiDeclarations are not supported on global level too.
            }
            else {
                throw IllegalArgumentException("Unsupported declaration kind: " + declaration)
            }
        }
        return result.toReadOnlyList()
    }

    // Do not change this, override in concrete subclasses:
    // it is very easy to compromise laziness of this class, and fail all the debugging
    // a generic implementation can't do this properly
    abstract override fun toString(): String

    override fun getOwnDeclaredDescriptors() = getDescriptors()

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.getSimpleName(), " {")
        p.pushIndent()

        p.println("thisDescriptor = ", thisDescriptor)

        p.popIndent()
        p.println("}")
    }

    private fun recordLookup(name: Name, from: LookupLocation) {
        c.lookupTracker.record(from, this, name)
    }
}
