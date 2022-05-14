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

@file:JvmName("FuzzyTypeUtils")

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.CallHandle
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilderImpl
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*
import java.util.*

fun CallableDescriptor.fuzzyReturnType(): FuzzyType? {
    val returnType = returnType ?: return null
    return FuzzyType(returnType, typeParameters)
}

fun CallableDescriptor.fuzzyExtensionReceiverType(): FuzzyType? {
    val receiverParameter = extensionReceiverParameter
    return if (receiverParameter != null) FuzzyType(receiverParameter.type, typeParameters) else null
}

fun FuzzyType.makeNotNullable() = FuzzyType(type.makeNotNullable(), freeParameters)
fun FuzzyType.makeNullable() = FuzzyType(type.makeNullable(), freeParameters)
fun FuzzyType.nullability() = type.nullability()

fun FuzzyType.isAlmostEverything(): Boolean {
    if (freeParameters.isEmpty()) return false
    val typeParameter = type.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return false
    if (typeParameter !in freeParameters) return false
    return typeParameter.upperBounds.singleOrNull()?.isAnyOrNullableAny() ?: false
}

class FuzzyType(
        val type: KotlinType,
        freeParameters: Collection<TypeParameterDescriptor>
) {
    val freeParameters: Set<TypeParameterDescriptor>

    init {
        if (freeParameters.isNotEmpty()) {
            val usedTypeParameters = HashSet<TypeParameterDescriptor>()
            usedTypeParameters.addUsedTypeParameters(type)
            this.freeParameters = freeParameters.filter { it in usedTypeParameters }.toSet()
        }
        else {
            this.freeParameters = emptySet()
        }
    }

    override fun equals(other: Any?) = other is FuzzyType && other.type == type && other.freeParameters == freeParameters

    override fun hashCode() = type.hashCode()

    private fun MutableSet<TypeParameterDescriptor>.addUsedTypeParameters(type: KotlinType) {
        val typeParameter = type.constructor.declarationDescriptor as? TypeParameterDescriptor
        if (typeParameter != null && add(typeParameter)) {
            typeParameter.upperBounds.forEach { addUsedTypeParameters(it) }
        }

        for (argument in type.arguments) {
            if (!argument.isStarProjection) { // otherwise we can fall into infinite recursion
                addUsedTypeParameters(argument.type)
            }
        }
    }

    fun checkIsSubtypeOf(otherType: FuzzyType): TypeSubstitutor?
            = matchedSubstitutor(otherType, MatchKind.IS_SUBTYPE)

    fun checkIsSuperTypeOf(otherType: FuzzyType): TypeSubstitutor?
            = matchedSubstitutor(otherType, MatchKind.IS_SUPERTYPE)

    fun checkIsSubtypeOf(otherType: KotlinType): TypeSubstitutor?
            = checkIsSubtypeOf(FuzzyType(otherType, emptyList()))

    fun checkIsSuperTypeOf(otherType: KotlinType): TypeSubstitutor?
            = checkIsSuperTypeOf(FuzzyType(otherType, emptyList()))

    private enum class MatchKind {
        IS_SUBTYPE,
        IS_SUPERTYPE
    }

    private fun matchedSubstitutor(otherType: FuzzyType, matchKind: MatchKind): TypeSubstitutor? {
        if (type.isError) return null
        if (otherType.type.isError) return null

        fun KotlinType.checkInheritance(otherType: KotlinType): Boolean {
            return when (matchKind) {
                MatchKind.IS_SUBTYPE -> this.isSubtypeOf(otherType)
                MatchKind.IS_SUPERTYPE -> otherType.isSubtypeOf(this)
            }
        }

        if (freeParameters.isEmpty() && otherType.freeParameters.isEmpty()) {
            return if (type.checkInheritance(otherType.type)) TypeSubstitutor.EMPTY else null
        }

        val builder = ConstraintSystemBuilderImpl()
        val typeVariableSubstitutor = builder.registerTypeVariables(CallHandle.NONE, freeParameters + otherType.freeParameters)

        val typeInSystem = typeVariableSubstitutor.substitute(type, Variance.INVARIANT)
        val otherTypeInSystem = typeVariableSubstitutor.substitute(otherType.type, Variance.INVARIANT)

        when (matchKind) {
            MatchKind.IS_SUBTYPE ->
                builder.addSubtypeConstraint(typeInSystem, otherTypeInSystem, ConstraintPositionKind.RECEIVER_POSITION.position())
            MatchKind.IS_SUPERTYPE ->
                builder.addSubtypeConstraint(otherTypeInSystem, typeInSystem, ConstraintPositionKind.RECEIVER_POSITION.position())
        }

        builder.fixVariables()

        val constraintSystem = builder.build()

        if (constraintSystem.status.hasContradiction()) return null

        // currently ConstraintSystem return successful status in case there are problems with nullability
        // that's why we have to check subtyping manually
        val substitutor = constraintSystem.resultingSubstitutor
        val substitutedType = substitutor.substitute(type, Variance.INVARIANT) ?: return null
        if (substitutedType.isError) return null
        val otherSubstitutedType = substitutor.substitute(otherType.type, Variance.INVARIANT) ?: return null
        if (otherSubstitutedType.isError) return null
        if (!substitutedType.checkInheritance(otherSubstitutedType)) return null

        val substitution = constraintSystem.typeVariables.map { it.originalTypeParameter }.toMapBy({ it.typeConstructor }) {
            val type = it.defaultType
            val solution = substitutor.substitute(type, Variance.INVARIANT)
            TypeProjectionImpl(if (solution != null && !ErrorUtils.containsUninferredParameter(solution)) solution else type)
        }

        return TypeSubstitutor.create(TypeConstructorSubstitution.createByConstructorsMap(substitution))
    }
}
