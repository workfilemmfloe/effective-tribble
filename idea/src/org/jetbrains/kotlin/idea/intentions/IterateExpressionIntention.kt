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

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.resolve.ideService
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

public class IterateExpressionIntention : SelfTargetingIntention<KtExpression>(KtExpression::class.java, "Iterate over collection"), HighPriorityAction {
    override fun isApplicableTo(element: KtExpression, caretOffset: Int): Boolean {
        if (element.parent !is KtBlockExpression) return false
        val range = element.textRange
        if (caretOffset != range.startOffset && caretOffset != range.endOffset) return false
        val data = data(element) ?: return false
        text = "Iterate over '${IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(data.collectionType)}'"
        return true
    }

    private data class Data(val collectionType: KotlinType, val elementType: KotlinType)

    private fun data(expression: KtExpression): Data? {
        val resolutionFacade = expression.getResolutionFacade()
        val bindingContext = resolutionFacade.analyze(expression, BodyResolveMode.PARTIAL)
        val type = bindingContext.getType(expression) ?: return null
        val scope = expression.getResolutionScope(bindingContext, resolutionFacade)
        val detector = resolutionFacade.ideService<IterableTypesDetection>().createDetector(scope)
        val elementType = detector.elementType(type)?.type ?: return null
        return Data(type, elementType)
    }

    override fun applyTo(element: KtExpression, editor: Editor) {
        //TODO: multi-declaration (when?)

        val elementType = data(element)!!.elementType
        val nameValidator = NewDeclarationNameValidator(element, element.siblings(), NewDeclarationNameValidator.Target.VARIABLES)
        val bindingContext = element.analyze(BodyResolveMode.PARTIAL)
        val names = KotlinNameSuggester.suggestIterationVariableNames(element, elementType, bindingContext, nameValidator, "e")

        var forExpression = KtPsiFactory(element).createExpressionByPattern("for($0 in $1) {\nx\n}", names.first(), element) as KtForExpression
        forExpression = element.replaced(forExpression)

        PsiDocumentManager.getInstance(forExpression.project).doPostponedOperationsAndUnblockDocument(editor.document)

        val bodyPlaceholder = (forExpression.body as KtBlockExpression).statements.single()

        val templateBuilder = TemplateBuilderImpl(forExpression)
        templateBuilder.replaceElement(forExpression.loopParameter!!, ChooseStringExpression(names))
        templateBuilder.replaceElement(bodyPlaceholder, ConstantNode(""), false)
        templateBuilder.setEndVariableAfter(bodyPlaceholder)

        templateBuilder.run(editor, true)
    }
}