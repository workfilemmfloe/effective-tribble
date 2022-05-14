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

package org.jetbrains.kotlin.idea.findUsages

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import com.intellij.psi.PsiPackage
import com.intellij.psi.PsiReferenceExpression
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.findUsages.UsageTypeEnum.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.references.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.DescriptorUtils

public object UsageTypeUtils {
    public fun getUsageType(element: PsiElement?): UsageTypeEnum? {
        when (element) {
            is JetForExpression -> return IMPLICIT_ITERATION
            is JetMultiDeclaration -> return READ
            is JetPropertyDelegate -> return PROPERTY_DELEGATION
        }

        val refExpr = element?.getNonStrictParentOfType<JetReferenceExpression>()
        if (refExpr == null) return null

        val context = refExpr.analyze()

        fun getCommonUsageType(): UsageTypeEnum? {
            return when {
                refExpr.getNonStrictParentOfType<JetImportDirective>() != null ->
                    CLASS_IMPORT
                refExpr.getParentOfTypeAndBranch<JetCallableReferenceExpression>(){ getCallableReference() } != null ->
                    CALLABLE_REFERENCE
                else -> null
            }
        }

        fun getClassUsageType(): UsageTypeEnum? {
            if (refExpr.getNonStrictParentOfType<JetTypeProjection>() != null) return TYPE_PARAMETER

            val property = refExpr.getNonStrictParentOfType<JetProperty>()
            if (property != null) {
                when {
                    property.getTypeReference().isAncestor(refExpr) ->
                        return if (property.isLocal()) CLASS_LOCAL_VAR_DECLARATION else NON_LOCAL_PROPERTY_TYPE

                    property.getReceiverTypeReference().isAncestor(refExpr) ->
                        return EXTENSION_RECEIVER_TYPE
                }
            }

            val function = refExpr.getNonStrictParentOfType<JetFunction>()
            if (function != null) {
                when {
                    function.getTypeReference().isAncestor(refExpr) ->
                        return FUNCTION_RETURN_TYPE
                    function.getReceiverTypeReference().isAncestor(refExpr) ->
                        return EXTENSION_RECEIVER_TYPE
                }
            }

            return when {
                refExpr.getParentOfTypeAndBranch<JetTypeParameter>(){ getExtendsBound() } != null
                || refExpr.getParentOfTypeAndBranch<JetTypeConstraint>(){ getBoundTypeReference() } != null ->
                    TYPE_CONSTRAINT

                refExpr is JetDelegationSpecifier
                || refExpr.getParentOfTypeAndBranch<JetDelegationSpecifier>(){ getTypeReference() } != null ->
                    SUPER_TYPE

                refExpr.getParentOfTypeAndBranch<JetTypedef>(){ getTypeReference() } != null ->
                    TYPE_DEFINITION

                refExpr.getParentOfTypeAndBranch<JetParameter>(){ getTypeReference() } != null ->
                    VALUE_PARAMETER_TYPE

                refExpr.getParentOfTypeAndBranch<JetIsExpression>(){ getTypeReference() } != null
                || refExpr.getParentOfTypeAndBranch<JetWhenConditionIsPattern>(){ getTypeReference() } != null ->
                    IS

                with(refExpr.getParentOfTypeAndBranch<JetBinaryExpressionWithTypeRHS>(){ getRight() }) {
                    val opType = this?.getOperationReference()?.getReferencedNameElementType()
                   ^opType == JetTokens.AS_KEYWORD || opType == JetTokens.AS_SAFE || opType == JetTokens.COLON
                } ->
                    CLASS_CAST_TO

                with(refExpr.getNonStrictParentOfType<JetDotQualifiedExpression>()) {
                    if (this == null) false
                    else if (getReceiverExpression() == refExpr) true
                    else
                        getSelectorExpression() == refExpr
                        && getParentOfTypeAndBranch<JetDotQualifiedExpression>(strict = true) { getReceiverExpression() } != null
                } ->
                    CLASS_OBJECT_ACCESS

                refExpr.getParentOfTypeAndBranch<JetSuperExpression>(){ getSuperTypeQualifier() } != null ->
                    SUPER_TYPE_QUALIFIER

                else -> null
            }
        }

        fun getVariableUsageType(): UsageTypeEnum? {
            if (refExpr.getParentOfTypeAndBranch<JetDelegatorByExpressionSpecifier>(){ getDelegateExpression() } != null) {
                return DELEGATE
            }

            if (refExpr.getParent() is JetValueArgumentName) return NAMED_ARGUMENT

            val dotQualifiedExpression = refExpr.getNonStrictParentOfType<JetDotQualifiedExpression>()

            if (dotQualifiedExpression != null) {
                val parent = dotQualifiedExpression.getParent()
                when {
                    dotQualifiedExpression.getReceiverExpression().isAncestor(refExpr) ->
                        return RECEIVER

                    parent is JetDotQualifiedExpression && parent.getReceiverExpression().isAncestor(refExpr) ->
                        return RECEIVER
                }
            }

            return when (refExpr.readWriteAccess(useResolveForReadWrite = true)) {
                ReferenceAccess.READ -> READ
                ReferenceAccess.WRITE, ReferenceAccess.READ_WRITE -> WRITE
            }
        }

        fun getFunctionUsageType(descriptor: FunctionDescriptor): UsageTypeEnum? {
            when (refExpr.mainReference) {
                is JetArrayAccessReference -> {
                    return when {
                        context[BindingContext.INDEXED_LVALUE_GET, refExpr] != null -> IMPLICIT_GET
                        context[BindingContext.INDEXED_LVALUE_SET, refExpr] != null -> IMPLICIT_SET
                        else -> null
                    }
                }
                is JetInvokeFunctionReference -> return IMPLICIT_INVOKE
            }

            return when {
                refExpr.getParentOfTypeAndBranch<JetDelegationSpecifier>(){ getTypeReference() } != null ->
                    SUPER_TYPE

                descriptor is ConstructorDescriptor
                && refExpr.getParentOfTypeAndBranch<JetAnnotationEntry>(){ getTypeReference() } != null ->
                    ANNOTATION

                with(refExpr.getParentOfTypeAndBranch<JetCallExpression>(){ getCalleeExpression() }) {
                    this?.getCalleeExpression() is JetSimpleNameExpression
                } ->
                    if (descriptor is ConstructorDescriptor) CLASS_NEW_OPERATOR else FUNCTION_CALL

                refExpr.getParentOfTypeAndBranch<JetBinaryExpression>(){ getOperationReference() } != null,
                refExpr.getParentOfTypeAndBranch<JetUnaryExpression>(){ getOperationReference() } != null,
                refExpr.getParentOfTypeAndBranch<JetWhenConditionInRange>(){ getOperationReference() } != null ->
                    FUNCTION_CALL

                else -> null
            }
        }

        fun getPackageUsageType(): UsageTypeEnum? {
            return when {
                refExpr.getNonStrictParentOfType<JetPackageDirective>() != null -> PACKAGE_DIRECTIVE
                refExpr.getNonStrictParentOfType<JetQualifiedExpression>() != null -> PACKAGE_MEMBER_ACCESS
                else -> getClassUsageType()
            }
        }

        val usageType = getCommonUsageType()
        if (usageType != null) return usageType

        val descriptor = context[BindingContext.REFERENCE_TARGET, refExpr]

        return when (descriptor) {
            is ClassifierDescriptor -> when {
            // Treat object accesses as variables to simulate the old behaviour (when variables were created for objects)
                DescriptorUtils.isNonCompanionObject(descriptor), DescriptorUtils.isEnumEntry(descriptor) -> getVariableUsageType()
                DescriptorUtils.isCompanionObject(descriptor) -> COMPANION_OBJECT_ACCESS
                else -> getClassUsageType()
            }
            is PackageViewDescriptor -> {
                if (refExpr.mainReference.resolve() is PsiPackage) getPackageUsageType() else getClassUsageType()
            }
            is VariableDescriptor -> getVariableUsageType()
            is FunctionDescriptor -> getFunctionUsageType(descriptor)
            else -> null
        }
    }
}

enum class UsageTypeEnum {
    TYPE_CONSTRAINT,
    VALUE_PARAMETER_TYPE,
    NON_LOCAL_PROPERTY_TYPE,
    FUNCTION_RETURN_TYPE,
    SUPER_TYPE,
    TYPE_DEFINITION,
    IS,
    CLASS_OBJECT_ACCESS,
    COMPANION_OBJECT_ACCESS,
    EXTENSION_RECEIVER_TYPE,
    SUPER_TYPE_QUALIFIER,

    FUNCTION_CALL,
    IMPLICIT_GET,
    IMPLICIT_SET,
    IMPLICIT_INVOKE,
    IMPLICIT_ITERATION,
    PROPERTY_DELEGATION,

    RECEIVER,
    DELEGATE,

    PACKAGE_DIRECTIVE,
    PACKAGE_MEMBER_ACCESS,

    CALLABLE_REFERENCE,

    READ,
    WRITE,
    CLASS_IMPORT,
    CLASS_LOCAL_VAR_DECLARATION,
    TYPE_PARAMETER,
    CLASS_CAST_TO,
    ANNOTATION,
    CLASS_NEW_OPERATOR,
    NAMED_ARGUMENT
}
