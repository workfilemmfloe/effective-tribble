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

import org.jetbrains.kotlin.psi.JetCallExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.psi.JetDotQualifiedExpression
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.psi.JetValueArgument
import org.jetbrains.kotlin.psi.JetPsiUnparsingUtils
import org.jetbrains.kotlin.psi.JetPsiFactory
import com.intellij.codeInsight.hint.HintManager
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.idea.caches.resolve.analyze

public open class ReplaceWithInfixFunctionCallIntention : JetSelfTargetingIntention<JetCallExpression>("replace.with.infix.function.call.intention", javaClass()) {
    override fun isApplicableTo(element: JetCallExpression): Boolean {
        throw IllegalStateException("isApplicableTo(JetExpressionImpl, Editor) should be called instead")
    }

    override fun isApplicableTo(element: JetCallExpression, editor: Editor): Boolean {
        val caretLocation = editor.getCaretModel().getOffset()

        val calleeExpr = element.getCalleeExpression()
        if (calleeExpr == null) return false

        val textRange = calleeExpr.getTextRange()
        if (textRange == null) return false

        if (caretLocation !in textRange) return false

        val parent = element.getParent()

        if (parent is JetDotQualifiedExpression) {
            val typeArguments = element.getTypeArgumentList()
            val valueArguments = element.getValueArgumentList()
            val functionLiteralArguments = element.getFunctionLiteralArguments()
            val numOfTotalValueArguments = (valueArguments?.getArguments()?.size() ?: 0) + functionLiteralArguments.size()

            if (typeArguments?.getArguments()?.size() ?: 0 == 0 &&
                    numOfTotalValueArguments == 1) {

                if (valueArguments?.getArguments()?.size() == 1 && valueArguments?.getArguments()?.first()?.isNamed() ?: false) {
                    val file = element.getContainingJetFile()
                    val bindingContext = file.analyzeFully()
                    val resolvedCall = element.getResolvedCall(bindingContext)
                    val valueArgumentsMap = resolvedCall?.getValueArguments()
                    val firstArgument = valueArguments?.getArguments()?.first()

                    return valueArgumentsMap?.keySet()?.any { it.getName().asString() == firstArgument?.getArgumentName()?.getText() && it.getIndex() == 0 } ?: false
                } else {
                    return true
                }
            } else {
                return false
            }
        } else {
            return false
        }
    }

    open fun intentionFailed(editor: Editor, messageID: String) {
        val message = "Intention failed: ${JetBundle.message("replace.with.infix.function.call.intention.error.$messageID")}"
        HintManager.getInstance().showErrorHint(editor, message)
    }

    override fun applyTo(element: JetCallExpression, editor: Editor) {
        val parent = element.getParent() as JetDotQualifiedExpression
        val receiver = parent.getReceiverExpression()
        val leftHandText = parent.getReceiverExpression().getText()
        val rightHandTextStringBuilder = StringBuilder()
        val operatorText = element.getCalleeExpression()!!.getText()
        val valueArguments = element.getValueArgumentList()?.getArguments() ?: listOf<JetValueArgument>()
        val functionLiteralArguments = element.getFunctionLiteralArguments()
        val bindingContext = parent.analyze()
        val receiverType = bindingContext[BindingContext.EXPRESSION_TYPE, receiver]
        if (receiverType == null) {
            if (bindingContext[BindingContext.QUALIFIER, receiver] != null) {
                intentionFailed(editor, "package.call")
                return
            }
            intentionFailed(editor, "resolution.failed")
            return
        }

        rightHandTextStringBuilder.append(
                if (valueArguments.size() > 0)
                    JetPsiUnparsingUtils.parenthesizeIfNeeded(valueArguments.first().getArgumentExpression())
                else
                    functionLiteralArguments.first().getText()
        )

        val replacement = JetPsiFactory(element).createExpression("$leftHandText $operatorText ${rightHandTextStringBuilder.toString()}")

        parent.replace(replacement)
    }
}
