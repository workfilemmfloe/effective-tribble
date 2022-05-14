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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.BranchedFoldingUtils
import org.jetbrains.kotlin.idea.intentions.branches
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class FoldIfToReturnIntention : SelfTargetingRangeIntention<KtIfExpression>(
        KtIfExpression::class.java,
        "Lift return out of 'if' expression"
) {

    override fun applicabilityRange(element: KtIfExpression): TextRange? {
        val branches = element.branches

        val lastElse = branches.lastOrNull()?.getStrictParentOfType<KtIfExpression>()?.`else` ?: return null
        if (BranchedFoldingUtils.getFoldableBranchedReturn(lastElse) == null) return null

        if (branches.any { BranchedFoldingUtils.getFoldableBranchedReturn(it) == null }) return null
        return element.ifKeyword.textRange
    }

    override fun applyTo(element: KtIfExpression, editor: Editor?) {
        for (it in element.branches) {
            val returnExpression = BranchedFoldingUtils.getFoldableBranchedReturn(it)
            returnExpression!!.replace(returnExpression.returnedExpression!!)
        }

        element.replace(KtPsiFactory(element).createExpressionByPattern("return $0", element))
    }
}