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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerBase
import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerFactoryBase
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Consumer
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsResultOfLambda
import org.jetbrains.kotlin.resolve.inline.InlineUtil

class KotlinHighlightExitPointsHandlerFactory : HighlightUsagesHandlerFactoryBase() {
    companion object {
        private val RETURN_AND_THROW = TokenSet.create(KtTokens.RETURN_KEYWORD, KtTokens.THROW_KEYWORD)
    }

    override fun createHighlightUsagesHandler(editor: Editor, file: PsiFile, target: PsiElement): HighlightUsagesHandlerBase<*>? {
        if (target is LeafPsiElement && (target.elementType in RETURN_AND_THROW)) {
            val returnOrThrow = PsiTreeUtil.getParentOfType<KtExpression>(
                target,
                KtReturnExpression::class.java,
                KtThrowExpression::class.java
            ) ?: return null

            return OnExitUsagesHandler(editor, file, returnOrThrow)
        }

        if (target is LeafPsiElement && target.elementType == KtTokens.IDENTIFIER) {
            val refExpr = target.parent as? KtNameReferenceExpression ?: return null
            val call = refExpr.parent as? KtCallExpression ?: return null
            if (call.calleeExpression != refExpr) return null

            val lambda = call.lambdaArguments.singleOrNull()?: return null
            val literal = lambda.getLambdaExpression()?.functionLiteral ?: return null

            return OnExitUsagesHandler(editor, file, literal)
        }

        return null
    }

    private class OnExitUsagesHandler(editor: Editor, file: PsiFile, val target: KtExpression) :
        HighlightUsagesHandlerBase<PsiElement>(editor, file) {

        override fun getTargets() = listOf(target)

        override fun selectTargets(targets: MutableList<PsiElement>, selectionConsumer: Consumer<MutableList<PsiElement>>) {
            selectionConsumer.consume(targets)
        }

        override fun computeUsages(targets: MutableList<PsiElement>?) {
            val relevantFunction: KtDeclarationWithBody? =
                when (target) {
                    is KtFunctionLiteral -> target
                    else -> target.getRelevantDeclaration()
                }

            relevantFunction?.accept(object : KtVisitorVoid() {
                override fun visitKtElement(element: KtElement) {
                    element.acceptChildren(this)
                }

                override fun visitExpression(expression: KtExpression) {
                    if (relevantFunction is KtFunctionLiteral && KtPsiUtil.isStatement(expression)) {
                        if (expression !is KtIfExpression && expression !is KtWhenExpression && expression !is KtBlockExpression) {
                            val bindingContext = expression.analyze()
                            if (expression !is KtBlockExpression && expression.isUsedAsResultOfLambda(bindingContext)) {
                                if (expression.getRelevantDeclaration() == relevantFunction) {
                                    addOccurrence(expression)
                                }

                                return
                            }
                        }
                    }

                    super.visitExpression(expression)
                }


                private fun visitReturnOrThrow(expression: KtExpression) {
                    if (expression.getRelevantDeclaration() == relevantFunction) {
                        addOccurrence(expression)
                    }
                }

                override fun visitReturnExpression(expression: KtReturnExpression) {
                    visitReturnOrThrow(expression)
                }

                override fun visitThrowExpression(expression: KtThrowExpression) {
                    visitReturnOrThrow(expression)
                }
            })
        }
    }
}

private fun KtExpression.getRelevantDeclaration(): KtDeclarationWithBody? {
    if (this is KtReturnExpression) {
        (this.getTargetLabel()?.mainReference?.resolve() as? KtFunction)?.let { return it }
    }

    for (parent in parents) {
        if (parent is KtDeclarationWithBody) {
            when {
                parent is KtPropertyAccessor -> return parent
                InlineUtil.canBeInlineArgument(parent) && !InlineUtil.isInlinedArgument(parent as KtFunction, parent.analyze(), false) ->
                    return parent
            }
        }
    }

    return null
}