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

package org.jetbrains.kotlin.j2k.usageProcessing

import com.intellij.psi.*
import org.jetbrains.kotlin.j2k.CodeConverter
import org.jetbrains.kotlin.j2k.ast.*
import com.intellij.util.IncorrectOperationException

class FieldToPropertyProcessing(val field: PsiField, val propertyName: String, val isNullable: Boolean) : UsageProcessing {
    override val targetElement: PsiElement get() = field

    override val convertedCodeProcessor = if (field.getName() != propertyName) {
        object: ConvertedCodeProcessor {
            override fun convertVariableUsage(expression: PsiReferenceExpression, codeConverter: CodeConverter): Expression? {
                val identifier = Identifier(propertyName, isNullable).assignNoPrototype()

                val qualifier = expression.getQualifierExpression()
                if (qualifier != null) {
                    return QualifiedExpression(codeConverter.convertExpression(qualifier), identifier)
                }
                else {
                    // check if field name is shadowed
                    val elementFactory = PsiElementFactory.SERVICE.getInstance(expression.getProject())
                    val refExpr = try {
                        elementFactory.createExpressionFromText(propertyName, expression) as? PsiReferenceExpression ?: return identifier
                    }
                    catch(e: IncorrectOperationException) {
                        return identifier
                    }
                    return if (refExpr.resolve() == null)
                        identifier
                    else
                        QualifiedExpression(ThisExpression(Identifier.Empty).assignNoPrototype(), identifier) //TODO: this is not correct in case of nested/anonymous classes
                }
            }
        }
    }
    else {
        null
    }

    override val javaCodeProcessor: ExternalCodeProcessor? get() = null //TODO

    override val kotlinCodeProcessor: ExternalCodeProcessor? get() = null //TODO
}
