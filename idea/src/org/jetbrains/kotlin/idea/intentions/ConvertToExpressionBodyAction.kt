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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.lexer.JetTokens
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

public class ConvertToExpressionBodyAction : PsiElementBaseIntentionAction() {
    override fun getFamilyName(): String = JetBundle.message("convert.to.expression.body.action.family.name")

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        setText(JetBundle.message("convert.to.expression.body.action.name"))
        val data = calcData(element)
        return data != null && !containsReturn(data.value)
    }

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val (declaration, value) = calcData(element)!!

        if (!declaration.hasDeclaredReturnType() && declaration is JetNamedFunction) {
            val valueType = expressionType(value)
            if (valueType == null || !KotlinBuiltIns.isUnit(valueType)) {
                specifyTypeExplicitly(declaration, "Unit")
            }
        }

        val body = declaration.getBodyExpression()!!
        declaration.addBefore(JetPsiFactory(declaration).createEQ(), body)
        body.replace(value)

        if (declaration.hasDeclaredReturnType() && declaration is JetCallableDeclaration && canOmitType(declaration)) {
            val typeRef = declaration.getTypeReference()!!
            val colon = declaration.getColon()!!
            val range = TextRange(colon.getTextRange().getStartOffset(), typeRef.getTextRange().getEndOffset())
            editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset())
            editor.getCaretModel().moveToOffset(range.getEndOffset())
        }
    }

    private fun canOmitType(declaration: JetCallableDeclaration): Boolean {
        if (declaration.getModifierList()?.hasModifier(JetTokens.OVERRIDE_KEYWORD) ?: false) return true
        val descriptor = declaration.resolveToDescriptor()
        return !((descriptor as? DeclarationDescriptorWithVisibility)?.getVisibility()?.isPublicAPI() ?: false)
    }

    private data class Data(val declaration: JetDeclarationWithBody, val value: JetExpression)

    private fun calcData(element: PsiElement): Data? {
        val declaration = element.getStrictParentOfType<JetDeclarationWithBody>()
        if (declaration == null || declaration is JetFunctionLiteral) return null
        val body = declaration.getBodyExpression()
        if (!declaration.hasBlockBody() || body !is JetBlockExpression) return null

        val statements = body.getStatements()
        if (statements.size != 1) return null
        val statement = statements[0]
        return when(statement) {
            is JetReturnExpression -> {
                val value = statement.getReturnedExpression()
                if (value != null) Data(declaration, value) else null
            }

            //TODO: IMO this is not good code, there should be a way to detect that JetExpression does not have value
            is JetDeclaration -> null // is JetExpression but does not have value
            is JetLoopExpression -> null // is JetExpression but does not have value

            is JetExpression -> {
                if (statement is JetBinaryExpression && statement.getOperationToken() == JetTokens.EQ) return null // assignment does not have value

                val expressionType = expressionType(statement)
                if (expressionType != null &&
                      (KotlinBuiltIns.isUnit(expressionType) || KotlinBuiltIns.isNothing(expressionType)))
                    Data(declaration, statement)
                else
                    null
            }

            else -> null
        }
    }

    private fun containsReturn(element: PsiElement): Boolean {
        if (element is JetReturnExpression) return true
        //TODO: would be better to have some interface of declaration where return can be used
        if (element is JetNamedFunction || element is JetPropertyAccessor) return false // can happen inside

        var child = element.getFirstChild()
        while (child != null) {
            if (containsReturn(child!!)) return true
            child = child!!.getNextSibling()
        }

        return false
    }
}
