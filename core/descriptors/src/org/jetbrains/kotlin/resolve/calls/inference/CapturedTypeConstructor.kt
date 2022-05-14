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

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.Variance.IN_VARIANCE
import org.jetbrains.kotlin.types.Variance.OUT_VARIANCE
import org.jetbrains.kotlin.types.typeUtil.builtIns

public class CapturedTypeConstructor(
        public val typeProjection: TypeProjection
): TypeConstructor {
    init {
        assert(typeProjection.getProjectionKind() != Variance.INVARIANT) {
            "Only nontrivial projections can be captured, not: $typeProjection"
        }
    }

    override fun getParameters(): List<TypeParameterDescriptor> = listOf()

    override fun getSupertypes(): Collection<KotlinType> {
        val superType = if (typeProjection.getProjectionKind() == Variance.OUT_VARIANCE)
            typeProjection.getType()
        else
            builtIns.nullableAnyType
        return listOf(superType)
    }

    override fun isFinal() = true

    override fun isDenotable() = false

    override fun getDeclarationDescriptor() = null

    override fun getAnnotations() = Annotations.EMPTY

    override fun toString() = "CapturedTypeConstructor($typeProjection)"

    override fun getBuiltIns(): KotlinBuiltIns = typeProjection.type.constructor.builtIns
}

public class CapturedType(
        private val typeProjection: TypeProjection
): DelegatingType(), SubtypingRepresentatives {

    private val delegateType = run {
        val scope = ErrorUtils.createErrorScope(
                "No member resolution should be done on captured type, it used only during constraint system resolution", true)
        KotlinTypeImpl.create(Annotations.EMPTY, CapturedTypeConstructor(typeProjection), false, listOf(), scope)
    }

    override fun getDelegate(): KotlinType = delegateType

    override fun <T : TypeCapability> getCapability(capabilityClass: Class<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return if (capabilityClass == javaClass<SubtypingRepresentatives>()) this as T
        else super<DelegatingType>.getCapability(capabilityClass)
    }

    override val subTypeRepresentative: KotlinType
        get() = representative(OUT_VARIANCE, builtIns.nullableAnyType)

    override val superTypeRepresentative: KotlinType
        get() = representative(IN_VARIANCE, builtIns.nothingType)

    private fun representative(variance: Variance, default: KotlinType) =
        if (typeProjection.getProjectionKind() == variance) typeProjection.getType() else default

    override fun sameTypeConstructor(type: KotlinType) = delegateType.getConstructor() === type.getConstructor()

    override fun toString() = "Captured($typeProjection)"
}

public fun createCapturedType(typeProjection: TypeProjection): KotlinType = CapturedType(typeProjection)

public fun KotlinType.isCaptured(): Boolean = getConstructor() is CapturedTypeConstructor
