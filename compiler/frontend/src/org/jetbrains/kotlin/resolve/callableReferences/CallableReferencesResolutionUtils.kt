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

package org.jetbrains.kotlin.resolve.callableReferences

import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.diagnostics.Errors.CALLABLE_REFERENCE_LHS_NOT_A_CLASS
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassQualifier
import org.jetbrains.kotlin.resolve.scopes.receivers.Receiver
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils

fun resolvePossiblyAmbiguousCallableReference(
        callableReferenceExpression: KtCallableReferenceExpression,
        lhsType: KotlinType?,
        context: ResolutionContext<*>,
        resolutionMode: ResolveArgumentsMode,
        callResolver: CallResolver
): OverloadResolutionResults<CallableDescriptor>? {
    val reference = callableReferenceExpression.callableReference

    fun resolveWithReceiver(traceTitle: String?, receiver: Receiver?): OverloadResolutionResults<CallableDescriptor>? {
        val call = CallMaker.makeCall(reference, receiver, null, reference, emptyList())
        val temporaryTrace = TemporaryTraceAndCache.create(context, traceTitle, reference)
        val newContext =
                if (resolutionMode == ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS)
                    context.replaceTraceAndCache(temporaryTrace).replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE)
                else
                    context.replaceTraceAndCache(temporaryTrace)

        val resolutionResults = callResolver.resolveCallForMember(
                reference, BasicCallResolutionContext.create(newContext, call, CheckArgumentTypesMode.CHECK_CALLABLE_TYPE)
        )

        val shouldCommitTrace =
                if (resolutionMode == ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS) resolutionResults.isSingleResult
                else !resolutionResults.isNothing
        if (shouldCommitTrace) temporaryTrace.commit()

        return if (resolutionResults.isNothing) null else resolutionResults
    }

    if (lhsType == null) {
        return resolveWithReceiver("resolve callable reference with empty LHS", null)
    }

    val classifier = lhsType.constructor.declarationDescriptor
    if (classifier !is ClassDescriptor) {
        context.trace.report(CALLABLE_REFERENCE_LHS_NOT_A_CLASS.on(callableReferenceExpression))
        return null
    }

    val referenceExpression = (callableReferenceExpression.typeReference?.typeElement as? KtUserType)?.referenceExpression
    if (referenceExpression != null) {
        val qualifier = ClassQualifier(referenceExpression, classifier)
        val possibleStatic = resolveWithReceiver("resolve unbound callable reference in static scope", qualifier)
        if (possibleStatic != null) return possibleStatic
    }

    return resolveWithReceiver("trace to resolve ::${reference.getReferencedName()} with receiver", TransientReceiver(lhsType))
}

fun createReflectionTypeForCallableDescriptor(
        descriptor: CallableDescriptor,
        lhsType: KotlinType?,
        reflectionTypes: ReflectionTypes,
        ignoreReceiver: Boolean,
        scopeOwnerDescriptor: DeclarationDescriptor
): KotlinType? {
    val extensionReceiver = descriptor.extensionReceiverParameter
    val dispatchReceiver = descriptor.dispatchReceiverParameter?.let { dispatchReceiver ->
        // See CallableDescriptor#getOwnerForEffectiveDispatchReceiverParameter
        if ((descriptor as? CallableMemberDescriptor)?.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
            DescriptorUtils.getDispatchReceiverParameterIfNeeded(descriptor.containingDeclaration)
        else dispatchReceiver
    }

    val receiverType =
            if ((extensionReceiver != null || dispatchReceiver != null) && !ignoreReceiver)
                lhsType ?: extensionReceiver?.type ?: dispatchReceiver?.type
            else null

    return when (descriptor) {
        is FunctionDescriptor -> {
            val returnType = descriptor.returnType ?: return null
            val valueParametersTypes = ExpressionTypingUtils.getValueParametersTypes(descriptor.valueParameters)
            return reflectionTypes.getKFunctionType(Annotations.EMPTY, receiverType, valueParametersTypes, returnType)
        }
        is PropertyDescriptor -> {
            val mutable = descriptor.isVar && run {
                val setter = descriptor.setter
                setter == null || Visibilities.isVisible(receiverType?.let(::TransientReceiver), setter, scopeOwnerDescriptor)
            }
            reflectionTypes.getKPropertyType(Annotations.EMPTY, receiverType, descriptor.type, mutable)
        }
        is VariableDescriptor -> null
        else -> throw UnsupportedOperationException("Callable reference resolved to an unsupported descriptor: $descriptor")
    }
}
