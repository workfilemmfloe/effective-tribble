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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import org.jetbrains.kotlin.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.Qualifier
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import java.util.Collections
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.resolve.BindingContext
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.quickfix.JetIntentionActionsFactory
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import com.intellij.psi.PsiClass
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeAndGetResult
import org.jetbrains.kotlin.idea.core.refactoring.canRefactor
import org.jetbrains.kotlin.idea.core.refactoring.getExtractionContainers
import org.jetbrains.kotlin.idea.refactoring.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import java.util.ArrayList

object CreateCallableFromCallActionFactory : JetIntentionActionsFactory() {
    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction>? {
        val diagElement = diagnostic.getPsiElement()
        if (PsiTreeUtil.getParentOfType(
                diagElement,
                javaClass<JetTypeReference>(), javaClass<JetAnnotationEntry>(), javaClass<JetImportDirective>()
        ) != null) return null

        val callExpr = when (diagnostic.getFactory()) {
                           in Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS, Errors.EXPRESSION_EXPECTED_PACKAGE_FOUND -> {
                               val parent = diagElement.getParent()
                               if (parent is JetCallExpression && parent.getCalleeExpression() == diagElement) parent else diagElement
                           }

                           Errors.NO_VALUE_FOR_PARAMETER,
                           Errors.TOO_MANY_ARGUMENTS -> diagElement.getNonStrictParentOfType<JetCallExpression>()

                           else -> throw AssertionError("Unexpected diagnostic: ${diagnostic.getFactory()}")
                       } as? JetExpression ?: return null

        val project = callExpr.getProject()

        val calleeExpr = when (callExpr) {
                             is JetCallExpression -> callExpr.getCalleeExpression()
                             is JetSimpleNameExpression -> callExpr
                             else -> null
                         } as? JetSimpleNameExpression ?: return null

        if (calleeExpr.getReferencedNameElementType() != JetTokens.IDENTIFIER) return null

        val callParent = callExpr.getParent()
        val fullCallExpr =
                if (callParent is JetQualifiedExpression && callParent.getSelectorExpression() == callExpr) callParent else callExpr

        val context = calleeExpr.analyze()
        val receiver = callExpr.getCall(context)?.getExplicitReceiver() ?: ReceiverValue.NO_RECEIVER
        val receiverType = getReceiverTypeInfo(context, project, receiver) ?: return null

        val possibleContainers =
                if (receiverType is TypeInfo.Empty) {
                    val containers = with(fullCallExpr.getExtractionContainers()) {
                        if (callExpr is JetCallExpression) this else filter { it is JetClassBody || it is JetFile }
                    }
                    if (containers.isNotEmpty()) containers else return null
                }
                else Collections.emptyList()

        val name = calleeExpr.getReferencedName()
        val anyType = KotlinBuiltIns.getInstance().getNullableAnyType()

        val callableInfos = ArrayList<CallableInfo>(2)
        when (callExpr) {
            is JetCallExpression -> {
                val parameters = callExpr.getValueArguments().map {
                    ParameterInfo(
                            it.getArgumentExpression()?.let { TypeInfo(it, Variance.IN_VARIANCE) } ?: TypeInfo(anyType, Variance.IN_VARIANCE),
                            it.getArgumentName()?.getReferenceExpression()?.getReferencedName()
                    )
                }
                val typeParameters = callExpr.getTypeInfoForTypeArguments()
                val returnType = TypeInfo(fullCallExpr, Variance.OUT_VARIANCE)
                callableInfos.add(FunctionInfo(name, receiverType, returnType, possibleContainers, parameters, typeParameters))

                val expectedType = context[BindingContext.EXPECTED_EXPRESSION_TYPE, fullCallExpr] ?: anyType
                val constructorDescriptor = callExpr.getResolvedCall(context)?.getResultingDescriptor() as? ConstructorDescriptor
                val classDescriptor = constructorDescriptor?.getContainingDeclaration() as? ClassDescriptor
                val klass = classDescriptor?.let { DescriptorToSourceUtilsIde.getAnyDeclaration(project, it) }
                if ((klass is JetClass || klass is PsiClass) && klass.canRefactor()
                    && typeParameters.isEmpty()
                    && classDescriptor!!.getDefaultType().isSubtypeOf(expectedType)) {
                    callableInfos.add(SecondaryConstructorInfo(parameters, klass))
                }
            }

            is JetSimpleNameExpression -> {
                val varExpected = fullCallExpr.getAssignmentByLHS() != null
                val returnType = TypeInfo(
                        fullCallExpr.getExpressionForTypeGuess(),
                        if (varExpected) Variance.INVARIANT else Variance.OUT_VARIANCE
                )
                callableInfos.add(PropertyInfo(name, receiverType, returnType, varExpected, possibleContainers))
            }
        }

        return callableInfos.flatMap{ CreateCallableFromUsageFixes(callExpr, it) }
    }

    private fun getReceiverTypeInfo(context: BindingContext, project: Project, receiver: ReceiverValue): TypeInfo? {
        return when {
            !receiver.exists() -> TypeInfo.Empty
            receiver is Qualifier -> {
                val qualifierType = context.getType(receiver.expression)
                if (qualifierType != null) return TypeInfo(qualifierType, Variance.IN_VARIANCE)

                val classifier = receiver.classifier as? JavaClassDescriptor ?: return null
                val javaClass = DescriptorToSourceUtilsIde.getAnyDeclaration(project, classifier) as? PsiClass
                if (javaClass == null || !javaClass.canRefactor()) return null
                TypeInfo.StaticContextRequired(TypeInfo(classifier.getDefaultType(), Variance.IN_VARIANCE))
            }
            else -> TypeInfo(receiver.getType(), Variance.IN_VARIANCE)
        }
    }
}
