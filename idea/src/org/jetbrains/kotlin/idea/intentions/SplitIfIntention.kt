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

import org.jetbrains.kotlin.psi.JetIfExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetPsiUtil

public class SplitIfIntention : JetSelfTargetingIntention<JetExpression>("split.if", javaClass()) {
    override fun isApplicableTo(element: JetExpression, caretOffset: Int): Boolean {
        if (element !is JetSimpleNameExpression && element !is JetIfExpression) return false

        if (element is JetSimpleNameExpression) {
            return isOperatorValid(element)
        }

        if (element is JetIfExpression) {
            if (!isCursorOnIfKeyword(element, caretOffset)) return false
            if (getFirstValidOperator(element) == null) return false
        }
        return true
    }

    override fun applyTo(element: JetExpression, editor: Editor) {
        val currentElement = when (element) {
            is JetIfExpression -> getFirstValidOperator(element)
            else -> element as JetSimpleNameExpression
        }

        val ifExpression = currentElement!!.getNonStrictParentOfType<JetIfExpression>()
        val expression = currentElement.getParent() as JetBinaryExpression
        val rightExpression = getRight(expression, ifExpression!!.getCondition())
        val leftExpression = expression.getLeft()
        val elseExpression = ifExpression.getElse()
        val thenExpression = ifExpression.getThen()

        val psiFactory = JetPsiFactory(element)
        if (currentElement.getReferencedNameElementType() == JetTokens.ANDAND) {
            ifExpression.replace(
                    psiFactory.createIf(leftExpression, psiFactory.wrapInABlock(
                            psiFactory.createIf(rightExpression, thenExpression, elseExpression)
                    ),
                elseExpression)
            )
        }
        else {
            ifExpression.replace(psiFactory.createIf(leftExpression, thenExpression,
                psiFactory.createIf(rightExpression, thenExpression, elseExpression))
            )
        }
    }

    private fun getRight(element: JetBinaryExpression, condition: JetExpression): JetExpression {
        //gets the textOffset of the right side of the JetBinaryExpression in context to condition
        val startOffset = element.getRight()!!.getTextOffset() - condition.getTextOffset()
        val rightString = condition.getText()!![startOffset, condition.getTextLength()].toString()

        return JetPsiFactory(element).createExpression(rightString)
    }

    private fun isCursorOnIfKeyword(element: JetIfExpression, offset: Int): Boolean {
        val ifKeyword = JetPsiUtil.findChildByType(element, JetTokens.IF_KEYWORD) ?: return false
        return (offset >= ifKeyword.getTextOffset() && offset <= ifKeyword.getTextOffset() + ifKeyword.getTextLength())
    }

    private fun getFirstValidOperator(element: JetIfExpression): JetSimpleNameExpression? {
        if (element.getCondition() == null) return null
        val condition = element.getCondition()
        val childElements = PsiTreeUtil.findChildrenOfType(condition, javaClass<JetSimpleNameExpression>())
        return childElements.firstOrNull { isOperatorValid(it) }
    }

    private fun isOperatorValid(element: JetSimpleNameExpression): Boolean {
        val operator = element.getReferencedNameElementType()
        if (operator != JetTokens.ANDAND && operator != JetTokens.OROR) return false

        if (element.getParent() !is JetBinaryExpression) return false
        var expression = element.getParent() as JetBinaryExpression

        if (expression.getRight() == null || expression.getLeft() == null) return false

        while (expression.getParent() is JetBinaryExpression) {
            expression = expression.getParent() as JetBinaryExpression
            if (operator == JetTokens.ANDAND && expression.getOperationToken() != JetTokens.ANDAND) return false
            if (operator == JetTokens.OROR && expression.getOperationToken() != JetTokens.OROR) return false
        }

        if (expression.getParent()?.getParent() !is JetIfExpression) return false
        val ifExpression = expression.getParent()?.getParent() as JetIfExpression

        if (ifExpression.getCondition() == null) return false
        if (!PsiTreeUtil.isAncestor(ifExpression.getCondition(), element, false)) return false

        if (ifExpression.getThen() == null) return false

        return true
    }
}

