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

package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import org.jetbrains.jet.j2k.ast.types.ArrayType
import org.jetbrains.jet.j2k.ast.types.isPrimitive

open class ArrayInitializerExpression(val arrayType: ArrayType, val initializers: List<Expression>) : Expression() {
    override fun toKotlin(): String {
        return createArrayFunction() + "(" + createInitializers() + ")"
    }

    private fun createInitializers(): String {
        return initializers.map { explicitConvertIfNeeded(it) }.makeString(", ")
    }

    private fun createArrayFunction(): String {
        val elementType = arrayType.elementType
        if (elementType.isPrimitive()) {
            return (elementType.convertedToNotNull().toKotlin() + "Array").decapitalize()
        }

        return arrayType.convertedToNotNull().toKotlin().decapitalize()
    }

    private fun innerTypeStr(): String {
        return arrayType.convertedToNotNull().toKotlin().replace("Array", "").toLowerCase()
    }

    private fun explicitConvertIfNeeded(i: Expression): String {
        val doubleOrFloatTypes = setOf("double", "float", "java.lang.double", "java.lang.float")
        val afterReplace: String = innerTypeStr().replace(">", "").replace("<", "").replace("?", "")
        if (doubleOrFloatTypes.contains(afterReplace))
        {
            if (i is LiteralExpression) {
                if (i.toKotlin().contains(".")) {
                    return i.toKotlin()
                }

                return i.toKotlin() + ".0"
            }

            return "(" + i.toKotlin() + ")" + getConversion(afterReplace)
        }

        return i.toKotlin()
    }

    class object {
        private open fun getConversion(afterReplace: String): String {
            if (afterReplace.contains("double"))
                return "." + OperatorConventions.DOUBLE + "()"

            if (afterReplace.contains("float"))
                return "." + OperatorConventions.FLOAT + "()"

            return ""
        }
    }
}
