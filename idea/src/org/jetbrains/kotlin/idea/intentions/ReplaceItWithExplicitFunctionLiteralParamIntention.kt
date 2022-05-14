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

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.references.JetReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

public class ReplaceItWithExplicitFunctionLiteralParamIntention() : JetSelfTargetingOffsetIndependentIntention<JetNameReferenceExpression> (
        javaClass(), "Replace 'it' with explicit parameter"
), LowPriorityAction {
    override fun isApplicableTo(element: JetNameReferenceExpression)
            = isAutoCreatedItUsage(element)

    override fun applyTo(element: JetNameReferenceExpression, editor: Editor) {
        val target = element.mainReference.resolveToDescriptors(element.analyze()).single()

        val functionLiteral = DescriptorToSourceUtils.descriptorToDeclaration(target.getContainingDeclaration()!!) as JetFunctionLiteral

        val newExpr = JetPsiFactory(element).createExpression("{ it -> }") as JetFunctionLiteralExpression
        functionLiteral.addRangeAfter(
                newExpr.getFunctionLiteral().getValueParameterList(),
                newExpr.getFunctionLiteral().getArrow()!!,
                functionLiteral.getLBrace())
        PsiDocumentManager.getInstance(element.getProject()).doPostponedOperationsAndUnblockDocument(editor.getDocument())

        val paramToRename = functionLiteral.getValueParameters().single()
        editor.getCaretModel().moveToOffset(element.getTextOffset())
        VariableInplaceRenameHandler().doRename(paramToRename, editor, null)
    }
}
