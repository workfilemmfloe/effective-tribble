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

package org.jetbrains.jet.lang.resolve.scopes

import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.utils.Printer
import java.util.ArrayList
import kotlin.properties.Delegates
import org.jetbrains.jet.lang.resolve.DescriptorFactory.*

public class StaticScopeForKotlinClass(
        private val containingClass: ClassDescriptor
) : JetScope {
    override fun getClassifier(name: Name) = null // TODO

    private val functions: List<FunctionDescriptor> by Delegates.lazy {
        if (containingClass.getKind() != ClassKind.ENUM_CLASS) {
            listOf<FunctionDescriptor>()
        }
        else {
            listOf(createEnumValueOfMethod(containingClass), createEnumValuesMethod(containingClass))
        }
    }

    override fun getAllDescriptors() = functions

    override fun getOwnDeclaredDescriptors() = functions

    override fun getFunctions(name: Name) = functions.filterTo(ArrayList<FunctionDescriptor>(2)) { it.getName() == name }

    override fun getPackage(name: Name) = null
    override fun getProperties(name: Name) = listOf<VariableDescriptor>()
    override fun getLocalVariable(name: Name) = null
    override fun getContainingDeclaration() = containingClass
    override fun getDeclarationsByLabel(labelName: Name) = listOf<DeclarationDescriptor>()
    override fun getImplicitReceiversHierarchy() = listOf<ReceiverParameterDescriptor>()

    override fun printScopeStructure(p: Printer) {
        p.println("Static scope for $containingClass")
    }
}
