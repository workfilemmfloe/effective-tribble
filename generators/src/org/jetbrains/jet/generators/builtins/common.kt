/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.generators.builtins

import org.jetbrains.jet.generators.builtins.ProgressionKind.*

enum class ProgressionKind {
    BYTE
    CHAR
    SHORT
    INT
    LONG
    FLOAT
    DOUBLE

    val capitalized = name().toLowerCase().capitalize()
}

fun progressionIncrementType(kind: ProgressionKind) = when (kind) {
    BYTE, CHAR, SHORT -> "Int"
    else -> kind.capitalized
}

fun areEqualNumbers(kind: ProgressionKind, v: String) = when (kind) {
    FLOAT, DOUBLE -> "java.lang.${kind.capitalized}.compare($v, other.$v) == 0"
    else -> "$v == other.$v"
}

fun hashLong(v: String) = "($v xor ($v ushr 32))"

fun floatToIntBits(v: String) = "java.lang.Float.floatToIntBits($v)"

fun doubleToLongBits(v: String) = "java.lang.Double.doubleToLongBits($v)"

