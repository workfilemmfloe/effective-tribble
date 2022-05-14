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

package org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.BranchedUnfoldingUtils
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetPsiUtil
import org.jetbrains.kotlin.psi.JetWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

public class UnfoldAssignmentToWhenIntention : JetSelfTargetingRangeIntention<JetBinaryExpression>(javaClass(), "Replace assignment with 'when' expression" ), LowPriorityAction {
    override fun applicabilityRange(element: JetBinaryExpression): TextRange? {
        if (element.getOperationToken() !in JetTokens.ALL_ASSIGNMENTS) return null
        if (element.getLeft() == null) return null
        val right = element.getRight() as? JetWhenExpression ?: return null
        if (!JetPsiUtil.checkWhenExpressionHasSingleElse(right)) return null
        if (right.getEntries().any { it.getExpression() == null }) return null
        return TextRange(element.startOffset, right.getWhenKeyword().endOffset)
    }

    override fun applyTo(element: JetBinaryExpression, editor: Editor) {
        BranchedUnfoldingUtils.unfoldAssignmentToWhen(element, editor)
    }
}