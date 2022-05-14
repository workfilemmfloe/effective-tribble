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

package org.jetbrains.k2js.translate.intrinsic.operation

import com.google.common.collect.ImmutableSet
import com.google.dart.compiler.backend.js.ast.*
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import org.jetbrains.jet.lexer.JetToken
import org.jetbrains.k2js.translate.context.Namer
import org.jetbrains.k2js.translate.context.TranslationContext
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder.pattern
import org.jetbrains.k2js.translate.operation.OperatorTable
import org.jetbrains.k2js.translate.utils.ID
import org.jetbrains.k2js.translate.utils.JsAstUtils.*
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils
import org.jetbrains.k2js.translate.utils.PsiUtils.getOperationToken

public object LongCompareToBOIF : BinaryOperationIntrinsicFactory {

    val FLOATING_POINT_COMPARE_TO_LONG_PATTERN = pattern("Double|Float.compareTo(Long)")
    val LONG_COMPARE_TO_FLOATING_POINT_PATTERN = pattern("Long.compareTo(Float|Double)")
    val INTEGER_COMPARE_TO_LONG_PATTERN = pattern("Int|Short|Byte.compareTo(Long)")
    val CHAR_COMPARE_TO_LONG_PATTERN = pattern("Char.compareTo(Long)")
    val LONG_COMPARE_TO_INTEGER_PATTERN = pattern("Long.compareTo(Int|Short|Byte)")
    val LONG_COMPARE_TO_CHAR_PATTERN = pattern("Long.compareTo(Char)")
    val LONG_COMPARE_TO_LONG_PATTERN = pattern("Long.compareTo(Long)")

    private object FLOATING_POINT_COMPARE_TO_LONG : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: JetBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            return JsBinaryOperation(operator, left, invokeMethod(right, Namer.LONG_TO_NUMBER))
        }
    }

    private object LONG_COMPARE_TO_FLOATING_POINT : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: JetBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            return JsBinaryOperation(operator, invokeMethod(left, Namer.LONG_TO_NUMBER), right)
        }
    }

    private class CompareToBinaryIntrinsic(val toLeft: (JsExpression) -> JsExpression, val toRight: (JsExpression) -> JsExpression) : AbstractBinaryOperationIntrinsic() {
        override fun apply(expression: JetBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            val compareInvocation = compareForObject(toLeft(left), toRight(right))
            return JsBinaryOperation(operator, compareInvocation, JsNumberLiteral.ZERO)
        }
    }

    private val INTEGER_COMPARE_TO_LONG = CompareToBinaryIntrinsic( { longFromInt(it) }, ID)
    private val CHAR_COMPARE_TO_LONG  = CompareToBinaryIntrinsic( { longFromInt(charToInt(it)) }, ID)
    private val LONG_COMPARE_TO_INTEGER  = CompareToBinaryIntrinsic( ID, { longFromInt(it) })
    private val LONG_COMPARE_TO_CHAR  = CompareToBinaryIntrinsic( ID, { longFromInt(charToInt(it)) })
    private val LONG_COMPARE_TO_LONG  = CompareToBinaryIntrinsic( ID, ID )

    override public fun getSupportTokens(): ImmutableSet<JetToken> = OperatorConventions.COMPARISON_OPERATIONS

    override public fun getIntrinsic(descriptor: FunctionDescriptor): BinaryOperationIntrinsic? {
        if (JsDescriptorUtils.isBuiltin(descriptor)) {
            return when {
                FLOATING_POINT_COMPARE_TO_LONG_PATTERN.apply(descriptor) -> FLOATING_POINT_COMPARE_TO_LONG
                LONG_COMPARE_TO_FLOATING_POINT_PATTERN.apply(descriptor) -> LONG_COMPARE_TO_FLOATING_POINT
                INTEGER_COMPARE_TO_LONG_PATTERN.apply(descriptor) -> INTEGER_COMPARE_TO_LONG
                CHAR_COMPARE_TO_LONG_PATTERN.apply(descriptor) -> CHAR_COMPARE_TO_LONG
                LONG_COMPARE_TO_INTEGER_PATTERN.apply(descriptor) -> LONG_COMPARE_TO_INTEGER
                LONG_COMPARE_TO_CHAR_PATTERN.apply(descriptor) -> LONG_COMPARE_TO_CHAR
                LONG_COMPARE_TO_LONG_PATTERN.apply(descriptor) -> LONG_COMPARE_TO_LONG
                else -> null
            }
        }
        return null
    }
}
