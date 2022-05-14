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

package org.jetbrains.kotlin.resolve.constants

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeConstructor
import java.util.*

public class IntegerValueTypeConstructor(
        private val value: Long,
        private val builtIns: KotlinBuiltIns
) : TypeConstructor {
    private val supertypes = ArrayList<JetType>(4)

    init {
        // order of types matters
        // 'getPrimitiveNumberType' returns first of supertypes that is a subtype of expected type
        // for expected type 'Any' result type 'Int' should be returned
        checkBoundsAndAddSuperType(value, Integer.MIN_VALUE.toLong(), Integer.MAX_VALUE.toLong(), builtIns.getIntType())
        checkBoundsAndAddSuperType(value, java.lang.Byte.MIN_VALUE.toLong(), java.lang.Byte.MAX_VALUE.toLong(), builtIns.getByteType())
        checkBoundsAndAddSuperType(value, java.lang.Short.MIN_VALUE.toLong(), java.lang.Short.MAX_VALUE.toLong(), builtIns.getShortType())
        supertypes.add(builtIns.getLongType())
    }

    private fun checkBoundsAndAddSuperType(value: Long, minValue: Long, maxValue: Long, kotlinType: JetType) {
        if (value >= minValue && value <= maxValue) {
            supertypes.add(kotlinType)
        }
    }

    override fun getSupertypes(): Collection<JetType> = supertypes

    override fun getParameters(): List<TypeParameterDescriptor> = emptyList()

    override fun isFinal() = false

    override fun isDenotable() = false

    override fun getDeclarationDescriptor() = null

    override fun getAnnotations() = Annotations.EMPTY

    public fun getValue(): Long = value

    override fun getBuiltIns(): KotlinBuiltIns {
        return builtIns
    }

    override fun toString() = "IntegerValueType(" + value + ")"
}
