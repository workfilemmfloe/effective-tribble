/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.effectsystem.resolving.utility

import org.jetbrains.kotlin.effectsystem.factories.ValuesFactory
import org.jetbrains.kotlin.effectsystem.factories.lift
import org.jetbrains.kotlin.effectsystem.impls.ESConstant
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ConstantsParser {
    fun parseConstantValue(constantValue: ConstantValue<*>?): ESConstant? = when (constantValue.safeAs<EnumValue>()?.value?.name?.identifier) {
        "TRUE" -> true.lift()
        "FALSE" -> false.lift()
        "NULL" -> null.lift()
        "NOT_NULL" -> ValuesFactory.NOT_NULL_CONSTANT
        "UNKNOWN" -> ValuesFactory.UNKNOWN_CONSTANT
        null -> null
        else -> throw IllegalStateException("Unknown annotation-constant: $this")
    }
}