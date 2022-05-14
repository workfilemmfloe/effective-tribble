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

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.isSubpackageOf
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.types.*
import java.util.ArrayList
import kotlin.properties.Delegates

val KOTLIN_REFLECT_FQ_NAME = FqName("kotlin.reflect")

public class ReflectionTypes(private val module: ModuleDescriptor) {
    private val kotlinReflectScope: JetScope by Delegates.lazy {
        module.getPackage(KOTLIN_REFLECT_FQ_NAME).memberScope
    }

    fun find(className: String): ClassDescriptor {
        val name = Name.identifier(className)
        return kotlinReflectScope.getClassifier(name) as? ClassDescriptor
                ?: ErrorUtils.createErrorClass(KOTLIN_REFLECT_FQ_NAME.child(name).asString())
    }

    private object ClassLookup {
        fun get(types: ReflectionTypes, property: PropertyMetadata): ClassDescriptor {
            return types.find(property.name.capitalize())
        }
    }

    public fun getKFunction(n: Int): ClassDescriptor = find("KFunction$n")
    public fun getKExtensionFunction(n: Int): ClassDescriptor = find("KExtensionFunction$n")
    public fun getKMemberFunction(n: Int): ClassDescriptor = find("KMemberFunction$n")

    public val kClass: ClassDescriptor by ClassLookup
    public val kTopLevelVariable: ClassDescriptor by ClassLookup
    public val kMutableTopLevelVariable: ClassDescriptor by ClassLookup
    public val kMemberProperty: ClassDescriptor by ClassLookup
    public val kMutableMemberProperty: ClassDescriptor by ClassLookup
    public val kTopLevelExtensionProperty: ClassDescriptor by ClassLookup
    public val kMutableTopLevelExtensionProperty: ClassDescriptor by ClassLookup

    public fun getKClassType(annotations: Annotations, type: JetType): JetType {
        val descriptor = kClass
        if (ErrorUtils.isError(descriptor)) {
            return descriptor.getDefaultType()
        }

        val arguments = listOf(TypeProjectionImpl(Variance.INVARIANT, type))
        return JetTypeImpl(annotations, descriptor.getTypeConstructor(), false, arguments, descriptor.getMemberScope(arguments))
    }

    public fun getKFunctionType(
            annotations: Annotations,
            receiverType: JetType?,
            parameterTypes: List<JetType>,
            returnType: JetType,
            extensionFunction: Boolean
    ): JetType {
        val arity = parameterTypes.size()
        val classDescriptor =
                if (extensionFunction) getKExtensionFunction(arity)
                else if (receiverType != null) getKMemberFunction(arity)
                else getKFunction(arity)

        if (ErrorUtils.isError(classDescriptor)) {
            return classDescriptor.getDefaultType()
        }

        val arguments = KotlinBuiltIns.getFunctionTypeArgumentProjections(receiverType, parameterTypes, returnType)
        return JetTypeImpl(annotations, classDescriptor.getTypeConstructor(), false, arguments, classDescriptor.getMemberScope(arguments))
    }

    public fun getKPropertyType(
            annotations: Annotations,
            receiverType: JetType?,
            returnType: JetType,
            extensionProperty: Boolean,
            mutable: Boolean
    ): JetType {
        val classDescriptor = if (mutable) when {
            extensionProperty -> kMutableTopLevelExtensionProperty
            receiverType != null -> kMutableMemberProperty
            else -> kMutableTopLevelVariable
        }
        else when {
            extensionProperty -> kTopLevelExtensionProperty
            receiverType != null -> kMemberProperty
            else -> kTopLevelVariable
        }

        if (ErrorUtils.isError(classDescriptor)) {
            return classDescriptor.getDefaultType()
        }

        val arguments = ArrayList<TypeProjection>(2)
        if (receiverType != null) {
            arguments.add(TypeProjectionImpl(receiverType))
        }
        arguments.add(TypeProjectionImpl(returnType))
        return JetTypeImpl(annotations, classDescriptor.getTypeConstructor(), false, arguments, classDescriptor.getMemberScope(arguments))
    }

    companion object {
        public fun isReflectionType(type: JetType): Boolean {
            val descriptor = type.getConstructor().getDeclarationDescriptor() ?: return false
            val fqName = DescriptorUtils.getFqName(descriptor)
            return fqName.isSafe() && fqName.toSafe().isSubpackageOf(KOTLIN_REFLECT_FQ_NAME)
        }
    }
}
