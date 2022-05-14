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

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import java.util.ArrayList
import java.util.HashMap

public interface WritableScopeStorage {

    // must be protected, but KT-3029 Protected does not work in traits: IllegalAccessError
    val addedDescriptors: MutableList<DeclarationDescriptor>
    val redeclarationHandler: RedeclarationHandler

    var functionsByName: MutableMap<Name, IntList>? // = null
    var variablesAndClassifiersByName: MutableMap<Name, IntList>? // = null

    // Effectively protected: not to be used outside subclasses
    fun addVariableOrClassDescriptor(descriptor: DeclarationDescriptor) {
        val name = descriptor.getName()

        val originalDescriptor = variableOrClassDescriptorByName(name)
        if (originalDescriptor != null) {
            redeclarationHandler.handleRedeclaration(originalDescriptor, descriptor)
        }

        val descriptorIndex = addDescriptor(descriptor)

        if (variablesAndClassifiersByName == null) {
            variablesAndClassifiersByName = HashMap()
        }
        //TODO: could not use += because of KT-8050
        variablesAndClassifiersByName!![name] = variablesAndClassifiersByName!![name] + descriptorIndex

    }

    // Effectively protected: not to be used outside subclasses
    fun addFunctionDescriptor(functionDescriptor: FunctionDescriptor) {
        val descriptorIndex = addDescriptor(functionDescriptor)

        if (functionsByName == null) {
            functionsByName = HashMap(1)
        }
        val name = functionDescriptor.getName()
        //TODO: could not use += because of KT-8050
        functionsByName!![name] = functionsByName!![name] + descriptorIndex
    }

    // Effectively protected: not to be used outside subclasses
    fun variableOrClassDescriptorByName(name: Name, descriptorLimit: Int = addedDescriptors.size()): DeclarationDescriptor? {
        if (descriptorLimit == 0) return null

        var list = variablesAndClassifiersByName?.get(name)
        while (list != null) {
            val descriptorIndex = list.last
            if (descriptorIndex < descriptorLimit) {
                return descriptorIndex.descriptorByIndex()
            }
            list = list.prev
        }
        return null
    }

    // Effectively protected: not to be used outside subclasses
    fun functionsByName(name: Name, descriptorLimit: Int = addedDescriptors.size()): List<FunctionDescriptor>? {
        if (descriptorLimit == 0) return null

        var list = functionsByName?.get(name)
        while (list != null) {
            if (list.last < descriptorLimit) {
                return list.toDescriptors<FunctionDescriptor>()
            }
            list = list.prev
        }
        return null
    }

    private fun addDescriptor(descriptor: DeclarationDescriptor): Int {
        addedDescriptors.add(descriptor)
        return addedDescriptors.size() - 1
    }

    // Effectively protected: not to be used outside subclasses
    class IntList(val last: Int, val prev: IntList?)

    private fun Int.descriptorByIndex() = addedDescriptors[this]

    private fun IntList?.plus(value: Int) = IntList(value, this)

    private fun <TDescriptor: DeclarationDescriptor> IntList.toDescriptors(): List<TDescriptor> {
        val result = ArrayList<TDescriptor>(1)
        var rest: IntList? = this
        do {
            result.add(rest!!.last.descriptorByIndex() as TDescriptor)
            rest = rest.prev
        } while (rest != null)
        return result
    }

    fun getDeclaredClassifier(name: Name, descriptorLimit: Int = addedDescriptors.size())
            = variableOrClassDescriptorByName(name, descriptorLimit) as? ClassifierDescriptor

    fun getDeclaredVariables(name: Name, descriptorLimit: Int = addedDescriptors.size()): Collection<VariableDescriptor>
            = listOfNotNull(variableOrClassDescriptorByName(name, descriptorLimit) as? VariableDescriptor)

    fun getDeclaredFunctions(name: Name, descriptorLimit: Int = addedDescriptors.size()): Collection<FunctionDescriptor>
            = functionsByName(name, descriptorLimit) ?: emptyList()

}