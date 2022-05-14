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

package org.jetbrains.jet.j2k.usageProcessing

import org.jetbrains.jet.j2k.ast.Expression
import com.intellij.psi.*
import org.jetbrains.jet.j2k.CodeConverter
import org.jetbrains.jet.j2k.SpecialExpressionConverter

trait UsageProcessing {
    val targetElement: PsiElement
    val convertedCodeProcessor: ConvertedCodeProcessor?
    val javaCodeProcessor: ExternalCodeProcessor?
    val kotlinCodeProcessor: ExternalCodeProcessor?
}

trait ConvertedCodeProcessor {
    fun convertVariableUsage(expression: PsiReferenceExpression, codeConverter: CodeConverter): Expression? = null

    fun convertMethodUsage(methodCall: PsiMethodCallExpression, codeConverter: CodeConverter): Expression? = null
}

trait ExternalCodeProcessor {
    fun processUsage(reference: PsiReference)
}

class UsageProcessingExpressionConverter(val processings: Map<PsiElement, UsageProcessing>) : SpecialExpressionConverter {
    override fun convertExpression(expression: PsiExpression, codeConverter: CodeConverter): Expression? {
        if (processings.isEmpty()) return null

        when (expression) {
            is PsiReferenceExpression -> {
                val target = expression.resolve() as? PsiVariable ?: return null
                val processor = processings[target]?.convertedCodeProcessor ?: return null
                return processor.convertVariableUsage(expression, codeConverter)
            }

            is PsiMethodCallExpression -> {
                val target = expression.getMethodExpression().resolve() as? PsiMethod ?: return null
                val processor = processings[target]?.convertedCodeProcessor ?: return null
                return processor.convertMethodUsage(expression, codeConverter)
            }

            else -> return null
        }
    }
}