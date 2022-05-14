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

package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.actions.JvmSmartStepIntoHandler
import com.intellij.debugger.actions.MethodSmartStepTarget
import com.intellij.debugger.actions.SmartStepTarget
import com.intellij.debugger.engine.MethodFilter
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiMethod
import com.intellij.util.Range
import com.intellij.util.containers.OrderedSet
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil

public class KotlinSmartStepIntoHandler : JvmSmartStepIntoHandler() {

    override fun isAvailable(position: SourcePosition?) = position?.getFile() is KtFile

    override fun findSmartStepTargets(position: SourcePosition): List<SmartStepTarget> {
        if (position.getLine() < 0) return emptyList()

        val file = position.getFile()

        val lineStart = CodeInsightUtils.getStartLineOffset(file, position.getLine()) ?: return emptyList()

        val elementAtOffset = file.findElementAt(lineStart) ?: return emptyList()

        val element = CodeInsightUtils.getTopmostElementAtOffset(elementAtOffset, lineStart)
        if (element !is KtElement) return emptyList()

        val elementTextRange = element.getTextRange() ?: return emptyList()

        val doc = PsiDocumentManager.getInstance(file.getProject()).getDocument(file) ?: return emptyList()

        val lines = Range(doc.getLineNumber(elementTextRange.getStartOffset()), doc.getLineNumber(elementTextRange.getEndOffset()))
        val bindingContext = element.analyzeFully()
        val result = OrderedSet<SmartStepTarget>()

        // TODO support class initializers, local functions, delegated properties with specified type, setter for properties
        element.accept(object: KtTreeVisitorVoid() {

            override fun visitFunctionLiteralExpression(expression: KtFunctionLiteralExpression) {
                val context = expression.analyze()
                val resolvedCall = expression.getParentCall(context).getResolvedCall(context)
                if (resolvedCall != null && !InlineUtil.isInline(resolvedCall.getResultingDescriptor())) {
                    val arguments = resolvedCall.getValueArguments()
                    for ((param, argument) in arguments) {
                        if (argument.getArguments().any { it.getArgumentExpression() == expression}) {
                            val label = KotlinLambdaSmartStepTarget.calcLabel(resolvedCall.getResultingDescriptor(), param.getName())
                            result.add(KotlinLambdaSmartStepTarget(label, expression, lines))
                            break
                        }
                    }
                }
            }

            override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression) {
                // skip calls in object declarations
            }

            override fun visitIfExpression(expression: KtIfExpression) {
                expression.getCondition()?.accept(this)
            }

            override fun visitWhileExpression(expression: KtWhileExpression) {
                expression.getCondition()?.accept(this)
            }

            override fun visitDoWhileExpression(expression: KtDoWhileExpression) {
                expression.getCondition()?.accept(this)
            }

            override fun visitForExpression(expression: KtForExpression) {
                expression.getLoopRange()?.accept(this)
            }

            override fun visitWhenExpression(expression: KtWhenExpression) {
                expression.getSubjectExpression()?.accept(this)
            }

            override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
                recordFunction(expression)
                super.visitArrayAccessExpression(expression)
            }

            override fun visitUnaryExpression(expression: KtUnaryExpression) {
                recordFunction(expression.getOperationReference())
                super.visitUnaryExpression(expression)
            }

            override fun visitBinaryExpression(expression: KtBinaryExpression) {
                recordFunction(expression.getOperationReference())
                super.visitBinaryExpression(expression)
            }

            override fun visitCallExpression(expression: KtCallExpression) {
                val calleeExpression = expression.getCalleeExpression()
                if (calleeExpression != null) {
                    recordFunction(calleeExpression)
                }
                super.visitCallExpression(expression)
            }

            override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                val resolvedCall = expression.getResolvedCall(bindingContext)
                if (resolvedCall != null) {
                    val propertyDescriptor = resolvedCall.getResultingDescriptor()
                    if (propertyDescriptor is PropertyDescriptor) {
                        val getterDescriptor = propertyDescriptor.getGetter()
                        if (getterDescriptor != null && !getterDescriptor.isDefault()) {
                            val delegatedResolvedCall = bindingContext[BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, getterDescriptor]
                            if (delegatedResolvedCall == null) {
                                val getter = DescriptorToSourceUtilsIde.getAnyDeclaration(file.getProject(), getterDescriptor)
                                if (getter is KtPropertyAccessor && (getter.getBodyExpression() != null || getter.getEqualsToken() != null)) {
                                    val label = KotlinMethodSmartStepTarget.calcLabel(getterDescriptor)
                                    result.add(KotlinMethodSmartStepTarget(getter, label, expression, lines))
                                }
                            }
                            else {
                                val delegatedPropertyGetterDescriptor = delegatedResolvedCall.getResultingDescriptor()
                                if (delegatedPropertyGetterDescriptor is CallableMemberDescriptor) {
                                    val function = DescriptorToSourceUtilsIde.getAnyDeclaration(file.getProject(), delegatedPropertyGetterDescriptor)
                                    if (function is KtNamedFunction || function is KtSecondaryConstructor) {
                                        val label = "${propertyDescriptor.getName()}." + KotlinMethodSmartStepTarget.calcLabel(delegatedPropertyGetterDescriptor)
                                        result.add(KotlinMethodSmartStepTarget(function as KtFunction, label, expression, lines))
                                    }
                                }
                            }
                        }
                    }
                }
                super.visitSimpleNameExpression(expression)
            }

            private fun recordFunction(expression: KtExpression) {
                val resolvedCall = expression.getResolvedCall(bindingContext) ?: return

                val descriptor = resolvedCall.getResultingDescriptor()
                if (descriptor is CallableMemberDescriptor && !isIntrinsic(descriptor)) {
                    val function = DescriptorToSourceUtilsIde.getAnyDeclaration(file.getProject(), descriptor)
                    if (function is KtNamedFunction || function is KtSecondaryConstructor) {
                        val label = KotlinMethodSmartStepTarget.calcLabel(descriptor)
                        result.add(KotlinMethodSmartStepTarget(function as KtFunction, label, expression, lines))
                    }
                    else if (function is PsiMethod) {
                        result.add(MethodSmartStepTarget(function, null, expression, false, lines))
                    }
                }
            }
        }, null)

        return result
    }

    override fun createMethodFilter(stepTarget: SmartStepTarget?): MethodFilter? {
        return when (stepTarget) {
            is KotlinMethodSmartStepTarget -> KotlinBasicStepMethodFilter(stepTarget.resolvedElement, stepTarget.getCallingExpressionLines()!!)
            is KotlinLambdaSmartStepTarget -> KotlinLambdaMethodFilter(stepTarget.getLambda(), stepTarget.getCallingExpressionLines()!! )
            else -> super.createMethodFilter(stepTarget)
        }
    }


    private val methods = IntrinsicMethods()

    private fun isIntrinsic(descriptor: CallableMemberDescriptor): Boolean {
        return methods.getIntrinsic(descriptor) != null
    }
}
