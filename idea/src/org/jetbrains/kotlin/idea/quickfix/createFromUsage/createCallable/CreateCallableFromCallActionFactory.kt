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

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.refactoring.canRefactor
import org.jetbrains.kotlin.idea.core.refactoring.getExtractionContainers
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.receivers.Qualifier
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import java.util.*

sealed class CreateCallableFromCallActionFactory<E : JetExpression>(
        extensionsEnabled: Boolean = true
) : CreateCallableMemberFromUsageFactory<E>(extensionsEnabled) {
    protected abstract fun doCreateCallableInfo(
            expression: E,
            context: BindingContext,
            name: String,
            receiverType: TypeInfo,
            possibleContainers: List<JetElement>
    ): CallableInfo?

    protected fun getExpressionOfInterest(diagnostic: Diagnostic): JetExpression? {
        val diagElement = diagnostic.psiElement
        if (PsiTreeUtil.getParentOfType(
                diagElement,
                javaClass<JetTypeReference>(), javaClass<JetAnnotationEntry>(), javaClass<JetImportDirective>()
        ) != null) return null

        return when (diagnostic.factory) {
            in Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS, Errors.EXPRESSION_EXPECTED_PACKAGE_FOUND -> {
                val parent = diagElement.parent
                if (parent is JetCallExpression && parent.calleeExpression == diagElement) parent else diagElement
            }

            Errors.NO_VALUE_FOR_PARAMETER,
            Errors.TOO_MANY_ARGUMENTS -> diagElement.getNonStrictParentOfType<JetCallExpression>()

            else -> throw AssertionError("Unexpected diagnostic: ${diagnostic.factory}")
        } as? JetExpression
    }

    override fun createCallableInfo(element: E, diagnostic: Diagnostic): CallableInfo? {
        val project = element.project

        val calleeExpr = when (element) {
                             is JetCallExpression -> element.calleeExpression
                             is JetSimpleNameExpression -> element
                             else -> null
                         } as? JetSimpleNameExpression ?: return null

        if (calleeExpr.getReferencedNameElementType() != JetTokens.IDENTIFIER) return null

        val context = calleeExpr.analyze()
        val receiver = element.getCall(context)?.explicitReceiver ?: ReceiverValue.NO_RECEIVER
        val receiverType = getReceiverTypeInfo(context, project, receiver) ?: return null

        val possibleContainers =
                if (receiverType is TypeInfo.Empty) {
                    val containers = with(element.getQualifiedExpressionForSelectorOrThis().getExtractionContainers()) {
                        if (element is JetCallExpression) this else filter { it is JetClassBody || it is JetFile }
                    }
                    if (containers.isNotEmpty()) containers else return null
                }
                else Collections.emptyList()

        return doCreateCallableInfo(element, context, calleeExpr.getReferencedName(), receiverType, possibleContainers)
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
                TypeInfo.StaticContextRequired(TypeInfo(classifier.defaultType, Variance.IN_VARIANCE))
            }
            else -> TypeInfo(receiver.type, Variance.IN_VARIANCE)
        }
    }

    object Property: CreateCallableFromCallActionFactory<JetSimpleNameExpression>() {
        override fun getElementOfInterest(diagnostic: Diagnostic): JetSimpleNameExpression? {
            return getExpressionOfInterest(diagnostic) as? JetSimpleNameExpression
        }

        override fun doCreateCallableInfo(
                expression: JetSimpleNameExpression,
                context: BindingContext,
                name: String,
                receiverType: TypeInfo,
                possibleContainers: List<JetElement>
        ): CallableInfo? {
            val fullCallExpr = expression.getQualifiedExpressionForSelectorOrThis()
            val varExpected = fullCallExpr.getAssignmentByLHS() != null
            val returnType = TypeInfo(
                    fullCallExpr.getExpressionForTypeGuess(),
                    if (varExpected) Variance.INVARIANT else Variance.OUT_VARIANCE
            )
            return PropertyInfo(name, receiverType, returnType, varExpected, possibleContainers)
        }
    }

    object Function: CreateCallableFromCallActionFactory<JetCallExpression>() {
        override fun getElementOfInterest(diagnostic: Diagnostic): JetCallExpression? {
            return getExpressionOfInterest(diagnostic) as? JetCallExpression
        }

        override fun doCreateCallableInfo(
                expression: JetCallExpression,
                context: BindingContext,
                name: String,
                receiverType: TypeInfo,
                possibleContainers: List<JetElement>
        ): CallableInfo? {
            val parameters = expression.getParameterInfos()
            val typeParameters = expression.getTypeInfoForTypeArguments()
            val returnType = TypeInfo(expression.getQualifiedExpressionForSelectorOrThis(), Variance.OUT_VARIANCE)
            return FunctionInfo(name, receiverType, returnType, possibleContainers, parameters, typeParameters)
        }
    }

    object Constructor: CreateCallableFromCallActionFactory<JetCallExpression>() {
        override fun getElementOfInterest(diagnostic: Diagnostic): JetCallExpression? {
            return getExpressionOfInterest(diagnostic) as? JetCallExpression
        }

        override fun doCreateCallableInfo(
                expression: JetCallExpression,
                context: BindingContext,
                name: String,
                receiverType: TypeInfo,
                possibleContainers: List<JetElement>
        ): CallableInfo? {
            if (expression.typeArguments.isNotEmpty()) return null

            val constructorDescriptor = expression.getResolvedCall(context)?.resultingDescriptor as? ConstructorDescriptor
            val classDescriptor = constructorDescriptor?.containingDeclaration as? ClassDescriptor
            val klass = classDescriptor?.source?.getPsi()
            if ((klass !is JetClass && klass !is PsiClass) || !klass.canRefactor()) return null

            val expectedType = context[BindingContext.EXPECTED_EXPRESSION_TYPE, expression.getQualifiedExpressionForSelectorOrThis()]
                               ?: classDescriptor!!.builtIns.nullableAnyType
            if (!classDescriptor!!.defaultType.isSubtypeOf(expectedType)) return null

            val parameters = expression.getParameterInfos()

            return SecondaryConstructorInfo(parameters, klass)
        }
    }

    companion object {
        val INSTANCES = arrayOf(Function, Constructor, Property)
    }
}
