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

import org.jetbrains.kotlin.psi.JetFunctionLiteralExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.psi.JetPsiFactory
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.idea.codeInsight.ShortenReferences
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.caches.resolve.analyze

public class MakeTypeExplicitInLambdaIntention : JetSelfTargetingIntention<JetFunctionLiteralExpression>(
        "make.type.explicit.in.lambda", javaClass()) {

    override fun isApplicableTo(element: JetFunctionLiteralExpression): Boolean {
        throw IllegalStateException("isApplicableTo(JetExpressionImpl, Editor) should be called instead")
    }

    override fun isApplicableTo(element: JetFunctionLiteralExpression, editor: Editor): Boolean {
        val openBraceOffset = element.getLeftCurlyBrace().getStartOffset()
        val closeBraceOffset = element.getRightCurlyBrace()?.getStartOffset()
        val caretLocation = editor.getCaretModel().getOffset()
        val arrow = element.getFunctionLiteral().getArrowNode()
        if (arrow != null && !(openBraceOffset < caretLocation && caretLocation < arrow.getStartOffset() + 2) &&
            caretLocation != closeBraceOffset) return false
        else if (arrow == null && caretLocation != openBraceOffset + 1 && caretLocation != closeBraceOffset) return false

        val context = element.analyze()
        val func = context[BindingContext.FUNCTION, element.getFunctionLiteral()]
        if (func == null || ErrorUtils.containsErrorType(func)) return false

        if (hasImplicitReturnType(element) && func.getReturnType() != null) return true
        if (hasImplicitReceiverType(element) && func.getExtensionReceiverParameter()?.getType() != null) return true

        val params = element.getValueParameters()
        return params.any { it.getTypeReference() == null }
    }

    override fun applyTo(element: JetFunctionLiteralExpression, editor: Editor) {
        val functionLiteral = element.getFunctionLiteral()
        val context = element.analyze()
        val func = context[BindingContext.FUNCTION, functionLiteral]!!

        // Step 1: make the parameters types explicit
        val valueParameters = func.getValueParameters()
        val parameterString = valueParameters.map({descriptor -> "" + descriptor.getName() +
                                                      ": " + IdeDescriptorRenderers.SOURCE_CODE.renderType(descriptor.getType())
                                                  }).makeString(", ", "(", ")")
        val psiFactory = JetPsiFactory(element)
        val newParameterList = psiFactory.createParameterList(parameterString)
        val oldParameterList = functionLiteral.getValueParameterList()
        if (oldParameterList != null) {
            oldParameterList.replace(newParameterList)
        }
        else {
            val openBraceElement = functionLiteral.getLBrace()
            val nextSibling = openBraceElement?.getNextSibling()
            val addNewline = nextSibling is PsiWhiteSpace && nextSibling.getText()?.contains("\n") ?: false
            val (whitespace, arrow) = psiFactory.createWhitespaceAndArrow()
            functionLiteral.addRangeAfter(whitespace, arrow, openBraceElement)
            functionLiteral.addAfter(newParameterList, openBraceElement)
            if (addNewline) {
                functionLiteral.addAfter(psiFactory.createNewLine(), openBraceElement)
            }
        }
        ShortenReferences.process(element.getValueParameters())

        // Step 2: make the return type explicit
        val expectedReturnType = func.getReturnType()
        if (hasImplicitReturnType(element) && expectedReturnType != null) {
            val paramList = functionLiteral.getValueParameterList()
            val returnTypeColon = psiFactory.createColon()
            val returnTypeExpr = psiFactory.createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(expectedReturnType))
            functionLiteral.addAfter(returnTypeExpr, paramList)
            functionLiteral.addAfter(returnTypeColon, paramList)
            ShortenReferences.process(functionLiteral.getTypeReference()!!)
        }

        // Step 3: make the receiver type explicit
        val expectedReceiverType = func.getExtensionReceiverParameter()?.getType()
        if (hasImplicitReceiverType(element) && expectedReceiverType != null) {
            val receiverTypeString = IdeDescriptorRenderers.SOURCE_CODE.renderType(expectedReceiverType)
            val dot = functionLiteral.addBefore(psiFactory.createDot(), functionLiteral.getValueParameterList())
            functionLiteral.addBefore(psiFactory.createType(receiverTypeString), dot)
            ShortenReferences.process(functionLiteral.getReceiverTypeReference()!!)
        }
    }

    private fun hasImplicitReturnType(element: JetFunctionLiteralExpression): Boolean {
        return !element.hasDeclaredReturnType()
    }

    private fun hasImplicitReceiverType(element: JetFunctionLiteralExpression): Boolean {
        return element.getFunctionLiteral().getReceiverTypeReference() == null
    }
}
