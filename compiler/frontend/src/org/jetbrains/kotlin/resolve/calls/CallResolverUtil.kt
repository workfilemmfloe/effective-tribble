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

package org.jetbrains.kotlin.resolve.calls.callResolverUtil

import com.google.common.collect.Lists
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.EXPECTED_TYPE_POSITION
import org.jetbrains.kotlin.resolve.calls.inference.getNestedTypeVariables
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.validation.InfixValidator
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.TypeUtils.DONT_CARE
import org.jetbrains.kotlin.util.OperatorNameConventions

public enum class ResolveArgumentsMode {
    RESOLVE_FUNCTION_ARGUMENTS,
    SHAPE_FUNCTION_ARGUMENTS
}


public fun hasUnknownFunctionParameter(type: KotlinType): Boolean {
    assert(ReflectionTypes.isCallableType(type)) { "type $type is not a function or property" }
    return getParameterArgumentsOfCallableType(type).any {
        TypeUtils.containsSpecialType(it.getType(), DONT_CARE) || ErrorUtils.containsUninferredParameter(it.getType())
    }
}

public fun hasUnknownReturnType(type: KotlinType): Boolean {
    assert(ReflectionTypes.isCallableType(type)) { "type $type is not a function or property" }
    return ErrorUtils.containsErrorType(getReturnTypeForCallable(type))
}

public fun replaceReturnTypeForCallable(type: KotlinType, given: KotlinType): KotlinType {
    assert(ReflectionTypes.isCallableType(type)) { "type $type is not a function or property" }
    val newArguments = Lists.newArrayList<TypeProjection>()
    newArguments.addAll(getParameterArgumentsOfCallableType(type))
    newArguments.add(TypeProjectionImpl(Variance.INVARIANT, given))
    return replaceTypeArguments(type, newArguments)
}

public fun replaceReturnTypeByUnknown(type: KotlinType) = replaceReturnTypeForCallable(type, DONT_CARE)

private fun replaceTypeArguments(type: KotlinType, newArguments: List<TypeProjection>) =
        KotlinTypeImpl.create(type.getAnnotations(), type.getConstructor(), type.isMarkedNullable(), newArguments, type.getMemberScope())

private fun getParameterArgumentsOfCallableType(type: KotlinType) =
        type.getArguments().dropLast(1)

fun getReturnTypeForCallable(type: KotlinType) =
        type.getArguments().last().getType()

private fun CallableDescriptor.hasReturnTypeDependentOnUninferredParams(constraintSystem: ConstraintSystem): Boolean {
    val returnType = returnType ?: return false
    val nestedTypeVariables = constraintSystem.getNestedTypeVariables(returnType)
    return nestedTypeVariables.any { constraintSystem.getTypeBounds(it).value == null }
}

public fun CallableDescriptor.hasInferredReturnType(constraintSystem: ConstraintSystem): Boolean {
    if (hasReturnTypeDependentOnUninferredParams(constraintSystem)) return false

    // Expected type mismatch was reported before as 'TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH'
    if (constraintSystem.status.hasOnlyErrorsDerivedFrom(EXPECTED_TYPE_POSITION)) return false
    return true
}

public fun getErasedReceiverType(receiverParameterDescriptor: ReceiverParameterDescriptor, descriptor: CallableDescriptor): KotlinType {
    var receiverType = receiverParameterDescriptor.getType()
    for (typeParameter in descriptor.typeParameters) {
        if (typeParameter.typeConstructor == receiverType.constructor) {
            receiverType = TypeIntersector.getUpperBoundsAsType(typeParameter)
        }
    }
    val fakeTypeArguments = ContainerUtil.newSmartList<TypeProjection>()
    for (typeProjection in receiverType.getArguments()) {
        fakeTypeArguments.add(TypeProjectionImpl(typeProjection.getProjectionKind(), DONT_CARE))
    }
    return KotlinTypeImpl.create(receiverType.getAnnotations(), receiverType.getConstructor(), receiverType.isMarkedNullable(), fakeTypeArguments,
                       ErrorUtils.createErrorScope("Error scope for erased receiver type", /*throwExceptions=*/true))
}

public fun isOrOverridesSynthesized(descriptor: CallableMemberDescriptor): Boolean {
    if (descriptor.getKind() == CallableMemberDescriptor.Kind.SYNTHESIZED) {
        return true
    }
    if (descriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        return descriptor.getOverriddenDescriptors().all {
            isOrOverridesSynthesized(it)
        }
    }
    return false
}


fun isConventionCall(call: Call): Boolean {
    if (call is CallTransformer.CallForImplicitInvoke) return true
    val callElement = call.callElement
    if (callElement is KtArrayAccessExpression || callElement is KtMultiDeclarationEntry) return true
    val calleeExpression = call.calleeExpression as? KtOperationReferenceExpression ?: return false
    return calleeExpression.getNameForConventionalOperation() != null
}

fun isInfixCall(call: Call): Boolean = InfixValidator.isInfixCall(call.calleeExpression)

fun getUnaryPlusOrMinusOperatorFunctionName(call: Call): Name? {
    if (call.callElement !is KtPrefixExpression) return null
    val calleeExpression = call.calleeExpression as? KtOperationReferenceExpression ?: return null
    val name = calleeExpression.getNameForConventionalOperation(unaryOperations = true, binaryOperations = false)
    return if (name == OperatorNameConventions.UNARY_PLUS || name == OperatorNameConventions.UNARY_MINUS) name else null
}

public fun isInvokeCallOnVariable(call: Call): Boolean {
    if (call.getCallType() !== Call.CallType.INVOKE) return false
    val dispatchReceiver = call.getDispatchReceiver()
    //calleeExpressionAsDispatchReceiver for invoke is always ExpressionReceiver, see CallForImplicitInvoke
    val expression = (dispatchReceiver as ExpressionReceiver).expression
    return expression is KtSimpleNameExpression
}

public fun isInvokeCallOnExpressionWithBothReceivers(call: Call): Boolean {
    if (call.getCallType() !== Call.CallType.INVOKE || isInvokeCallOnVariable(call)) return false
    return call.getExplicitReceiver().exists() && call.getDispatchReceiver().exists()
}

public fun getSuperCallExpression(call: Call): KtSuperExpression? {
    return (call.getExplicitReceiver() as? ExpressionReceiver)?.expression as? KtSuperExpression
}

public fun getEffectiveExpectedType(parameterDescriptor: ValueParameterDescriptor, argument: ValueArgument): KotlinType {
    if (argument.getSpreadElement() != null) {
        if (parameterDescriptor.varargElementType == null) {
            // Spread argument passed to a non-vararg parameter, an error is already reported by ValueArgumentsToParametersMapper
            return DONT_CARE
        }
        return parameterDescriptor.getType()
    }
    val varargElementType = parameterDescriptor.varargElementType
    if (varargElementType != null) {
        return varargElementType
    }

    return parameterDescriptor.getType()
}
