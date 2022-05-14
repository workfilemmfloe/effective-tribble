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

package org.jetbrains.kotlin.resolve.scopes

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.collectionUtils.getFirstMatch
import org.jetbrains.kotlin.util.collectionUtils.getFromAllScopes
import org.jetbrains.kotlin.utils.Printer

public class ChainedScope(
        private val debugName: String,
        vararg scopes: MemberScope
) : MemberScope {
    private val scopeChain = scopes.clone()

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor?
            = getFirstMatch(scopeChain) { it.getContributedClassifier(name, location) }

    override fun getPackage(name: Name): PackageViewDescriptor?
            = getFirstMatch(scopeChain) { it.getPackage(name) }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor>
            = getFromAllScopes(scopeChain) { it.getContributedVariables(name, location) }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor>
            = getFromAllScopes(scopeChain) { it.getContributedFunctions(name, location) }

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean)
            = getFromAllScopes(scopeChain) { it.getContributedDescriptors(kindFilter, nameFilter) }

    override fun toString() = debugName

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.getSimpleName(), ": ", debugName, " {")
        p.pushIndent()

        for (scope in scopeChain) {
            scope.printScopeStructure(p)
        }

        p.popIndent()
        p.println("}")
    }
}
