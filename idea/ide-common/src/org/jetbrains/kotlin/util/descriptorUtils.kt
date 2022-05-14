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

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.equalTypesOrNulls

public fun descriptorsEqualWithSubstitution(descriptor1: DeclarationDescriptor?, descriptor2: DeclarationDescriptor?): Boolean {
    if (descriptor1 == descriptor2) return true
    if (descriptor1 == null || descriptor2 == null) return false
    if (descriptor1.getOriginal() != descriptor2.getOriginal()) return false
    if (descriptor1 !is CallableDescriptor) return true
    descriptor2 as CallableDescriptor

    val typeChecker = KotlinTypeChecker.withAxioms(object: KotlinTypeChecker.TypeConstructorEquality {
        override fun equals(a: TypeConstructor, b: TypeConstructor): Boolean {
            val typeParam1 = a.getDeclarationDescriptor() as? TypeParameterDescriptor
            val typeParam2 = b.getDeclarationDescriptor() as? TypeParameterDescriptor
            if (typeParam1 != null
                && typeParam2 != null
                && typeParam1.getContainingDeclaration() == descriptor1
                && typeParam2.getContainingDeclaration() == descriptor2) {
                return typeParam1.getIndex() == typeParam2.getIndex()
            }

            return a == b
        }
    })

    if (!typeChecker.equalTypesOrNulls(descriptor1.getReturnType(), descriptor2.getReturnType())) return false

    val parameters1 = descriptor1.getValueParameters()
    val parameters2 = descriptor2.getValueParameters()
    if (parameters1.size() != parameters2.size()) return false
    for ((param1, param2) in parameters1.zip(parameters2)) {
        if (!typeChecker.equalTypes(param1.getType(), param2.getType())) return false
    }
    return true
}

public fun ClassDescriptor.findCallableMemberBySignature(signature: CallableMemberDescriptor): CallableMemberDescriptor? {
    val descriptorKind = if (signature is FunctionDescriptor) DescriptorKindFilter.FUNCTIONS else DescriptorKindFilter.VARIABLES
    return getDefaultType().getMemberScope()
            .getContributedDescriptors(descriptorKind)
            .filterIsInstance<CallableMemberDescriptor>()
            .firstOrNull {
                it.getContainingDeclaration() == this
                && OverridingUtil.DEFAULT.isOverridableBy(it as CallableDescriptor, signature, null).getResult() == OVERRIDABLE
            } as? CallableMemberDescriptor
}

public fun TypeConstructor.supertypesWithAny(): Collection<KotlinType> {
    val supertypes = supertypes
    val noSuperClass = supertypes
            .map { it.constructor.declarationDescriptor as? ClassDescriptor }
            .all  { it == null || it.kind == ClassKind.INTERFACE }
    return if (noSuperClass) supertypes + builtIns.anyType else supertypes
}