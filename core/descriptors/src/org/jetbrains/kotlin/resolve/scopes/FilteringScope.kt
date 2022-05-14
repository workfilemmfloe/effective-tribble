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

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.Printer

public class FilteringScope(private val workerScope: KtScope, private val predicate: (DeclarationDescriptor) -> Boolean) : KtScope {

    override fun getFunctions(name: Name, location: LookupLocation) = workerScope.getFunctions(name, location).filter(predicate)

    override fun getContainingDeclaration() = workerScope.getContainingDeclaration()

    private fun <D : DeclarationDescriptor> filterDescriptor(descriptor: D?): D?
            = if (descriptor != null && predicate(descriptor)) descriptor else null

    override fun getPackage(name: Name) = filterDescriptor(workerScope.getPackage(name))

    override fun getClassifier(name: Name, location: LookupLocation) = filterDescriptor(workerScope.getClassifier(name, location))

    override fun getProperties(name: Name, location: LookupLocation) = workerScope.getProperties(name, location).filter(predicate)

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation): Collection<PropertyDescriptor>
            = workerScope.getSyntheticExtensionProperties(receiverTypes, name, location).filter(predicate)

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>, name: Name, location: LookupLocation): Collection<FunctionDescriptor>
            = workerScope.getSyntheticExtensionFunctions(receiverTypes, name, location).filter(predicate)

    override fun getSyntheticExtensionProperties(receiverTypes: Collection<KotlinType>): Collection<PropertyDescriptor>
            = workerScope.getSyntheticExtensionProperties(receiverTypes).filter(predicate)

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<KotlinType>): Collection<FunctionDescriptor>
            = workerScope.getSyntheticExtensionFunctions(receiverTypes).filter(predicate)

    override fun getLocalVariable(name: Name) = filterDescriptor(workerScope.getLocalVariable(name))

    override fun getDescriptors(kindFilter: DescriptorKindFilter,
                                nameFilter: (Name) -> Boolean) = workerScope.getDescriptors(kindFilter, nameFilter).filter(predicate)

    override fun getImplicitReceiversHierarchy() = workerScope.getImplicitReceiversHierarchy()

    override fun getDeclarationsByLabel(labelName: Name) = workerScope.getDeclarationsByLabel(labelName).filter(predicate)

    override fun getOwnDeclaredDescriptors() = workerScope.getOwnDeclaredDescriptors().filter(predicate)

    override fun printScopeStructure(p: Printer) {
        p.println(javaClass.getSimpleName(), " {")
        p.pushIndent()

        p.print("workerScope = ")
        workerScope.printScopeStructure(p.withholdIndentOnce())

        p.popIndent()
        p.println("}")
    }
}
