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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.analysis.computeTypeInContext
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.CommentSaver
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

public class ConvertToExpressionBodyIntention : JetSelfTargetingOffsetIndependentIntention<JetDeclarationWithBody>(
        javaClass(), "Convert to expression body"
) {
    override fun isApplicableTo(element: JetDeclarationWithBody): Boolean {
        val value = calcValue(element) ?: return false
        return !value.anyDescendantOfType<JetReturnExpression>(
                canGoInside = { it !is JetFunctionLiteral && it !is JetNamedFunction && it !is JetPropertyAccessor }
        )
    }

    override fun allowCaretInsideElement(element: PsiElement) = element !is JetDeclaration

    override fun applyTo(element: JetDeclarationWithBody, editor: Editor) {
        applyToInternal(element) {
            val typeRef = it.getTypeReference()!!
            val colon = it.getColon()!!
            val range = TextRange(colon.startOffset, typeRef.endOffset)
            editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset())
            editor.getCaretModel().moveToOffset(range.getEndOffset())
        }
    }

    public fun applyTo(declaration: JetDeclarationWithBody, canDeleteTypeRef: Boolean) {
        applyToInternal(declaration) {
            if (canDeleteTypeRef) {
                it.deleteChildRange(it.getColon()!!, it.getTypeReference()!!)
            }
        }
    }

    private fun applyToInternal(declaration: JetDeclarationWithBody, onFinish: (JetCallableDeclaration) -> Unit) {
        val value = calcValue(declaration)!!

        if (!declaration.hasDeclaredReturnType() && declaration is JetNamedFunction) {
            val valueType = value.analyze().getType(value)
            if (valueType == null || !KotlinBuiltIns.isUnit(valueType)) {
                declaration.setType(KotlinBuiltIns.FQ_NAMES.unit.asString(), shortenReferences = true)
            }
        }

        val omitType = declaration.hasDeclaredReturnType() && declaration is JetCallableDeclaration && canOmitType(declaration, value)

        val body = declaration.getBodyExpression()!!

        val commentSaver = CommentSaver(body)

        declaration.addBefore(JetPsiFactory(declaration).createEQ(), body)
        val newBody = body.replace(value)

        commentSaver.restore(newBody)

        if (omitType) {
            onFinish(declaration as JetCallableDeclaration)
        }
    }

    private fun canOmitType(declaration: JetCallableDeclaration, expression: JetExpression): Boolean {
        // Workaround for anonymous objects and similar expressions without resolution scope
        // TODO: This should probably be fixed in front-end so that resolution scope is recorded for anonymous objects as well
        val scopeExpression = ((declaration as? JetDeclarationWithBody)?.getBodyExpression() as? JetBlockExpression)
                                 ?.getStatements()?.singleOrNull()
                         ?: return false

        val declaredType = (declaration.resolveToDescriptor() as? CallableDescriptor)?.getReturnType() ?: return false
        val scope = scopeExpression.analyze()[BindingContext.RESOLUTION_SCOPE, scopeExpression] ?: return false
        val expressionType = expression.computeTypeInContext(scope)
        return expressionType?.isSubtypeOf(declaredType) ?: false
    }

    private fun calcValue(declaration: JetDeclarationWithBody): JetExpression? {
        if (declaration is JetFunctionLiteral) return null
        val body = declaration.getBodyExpression()
        if (!declaration.hasBlockBody() || body !is JetBlockExpression) return null

        val statement = body.getStatements().singleOrNull() ?: return null
        when(statement) {
            is JetReturnExpression -> {
                return statement.getReturnedExpression()
            }

            //TODO: IMO this is not good code, there should be a way to detect that JetExpression does not have value
            is JetDeclaration, is JetLoopExpression -> return null // is JetExpression but does not have value

            else  -> {
                if (statement is JetBinaryExpression && statement.operationToken in JetTokens.ALL_ASSIGNMENTS) return null // assignment does not have value
                val expressionType = statement.analyze().getType(statement) ?: return null
                if (!KotlinBuiltIns.isUnit(expressionType) && !KotlinBuiltIns.isNothing(expressionType)) return null
                return statement
            }
        }
    }
}
