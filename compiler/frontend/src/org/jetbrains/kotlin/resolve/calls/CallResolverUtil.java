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

package org.jetbrains.kotlin.resolve.calls;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor;
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetSimpleNameExpression;
import org.jetbrains.kotlin.psi.JetSuperExpression;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.*;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.EXPECTED_TYPE_POSITION;
import static org.jetbrains.kotlin.types.TypeUtils.DONT_CARE;

public class CallResolverUtil {
    public static enum ResolveArgumentsMode {
        RESOLVE_FUNCTION_ARGUMENTS,
        SHAPE_FUNCTION_ARGUMENTS
    }

    private CallResolverUtil() {}


    public static boolean hasUnknownFunctionParameter(@NotNull JetType type) {
        assert KotlinBuiltIns.isFunctionOrExtensionFunctionType(type);
        List<TypeProjection> arguments = type.getArguments();
        // last argument is return type of function type
        List<TypeProjection> functionParameters = arguments.subList(0, arguments.size() - 1);
        for (TypeProjection functionParameter : functionParameters) {
            if (TypeUtils.containsSpecialType(functionParameter.getType(), DONT_CARE)
                || ErrorUtils.containsUninferredParameter(functionParameter.getType())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasUnknownReturnType(@NotNull JetType type) {
        assert KotlinBuiltIns.isFunctionOrExtensionFunctionType(type);
        JetType returnTypeFromFunctionType = KotlinBuiltIns.getReturnTypeFromFunctionType(type);
        return ErrorUtils.containsErrorType(returnTypeFromFunctionType);
    }

    public static JetType replaceReturnTypeByUnknown(@NotNull JetType type) {
        assert KotlinBuiltIns.isFunctionOrExtensionFunctionType(type);
        List<TypeProjection> arguments = type.getArguments();
        List<TypeProjection> newArguments = Lists.newArrayList();
        newArguments.addAll(arguments.subList(0, arguments.size() - 1));
        newArguments.add(new TypeProjectionImpl(Variance.INVARIANT, DONT_CARE));
        return new JetTypeImpl(type.getAnnotations(), type.getConstructor(), type.isMarkedNullable(), newArguments, type.getMemberScope());
    }

    private static boolean hasReturnTypeDependentOnUninferredParams(
            @NotNull CallableDescriptor candidateDescriptor,
            @NotNull ConstraintSystem constraintSystem
    ) {
        JetType returnType = candidateDescriptor.getReturnType();
        if (returnType == null) return false;

        for (TypeParameterDescriptor typeVariable : constraintSystem.getTypeVariables()) {
            JetType inferredValueForTypeVariable = constraintSystem.getTypeBounds(typeVariable).getValue();
            if (inferredValueForTypeVariable == null) {
                if (TypeUtils.dependsOnTypeParameters(returnType, Collections.singleton(typeVariable))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasInferredReturnType(
            @NotNull CallableDescriptor candidateDescriptor,
            @NotNull ConstraintSystem constraintSystem
    ) {
        if (hasReturnTypeDependentOnUninferredParams(candidateDescriptor, constraintSystem)) return false;

        // Expected type mismatch was reported before as 'TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH'
        if (constraintSystem.getStatus().hasOnlyErrorsFromPosition(EXPECTED_TYPE_POSITION.position())) return false;
        return true;
    }

    @NotNull
    public static JetType getErasedReceiverType(
            @NotNull ReceiverParameterDescriptor receiverParameterDescriptor,
            @NotNull CallableDescriptor descriptor
    ) {
        JetType receiverType = receiverParameterDescriptor.getType();
        for (TypeParameterDescriptor typeParameter : descriptor.getTypeParameters()) {
            if (typeParameter.getTypeConstructor().equals(receiverType.getConstructor())) {
                receiverType = typeParameter.getUpperBoundsAsType();
            }
        }
        List<TypeProjection> fakeTypeArguments = Lists.newArrayList();
        for (TypeProjection typeProjection : receiverType.getArguments()) {
            fakeTypeArguments.add(new TypeProjectionImpl(typeProjection.getProjectionKind(), DONT_CARE));
        }
        return new JetTypeImpl(
                receiverType.getAnnotations(), receiverType.getConstructor(), receiverType.isMarkedNullable(),
                fakeTypeArguments, ErrorUtils.createErrorScope("Error scope for erased receiver type", /*throwExceptions=*/true));
    }

    public static boolean isOrOverridesSynthesized(@NotNull CallableMemberDescriptor descriptor) {
        if (descriptor.getKind() == CallableMemberDescriptor.Kind.SYNTHESIZED) {
            return true;
        }
        if (descriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            for (CallableMemberDescriptor overridden : descriptor.getOverriddenDescriptors()) {
                if (!isOrOverridesSynthesized(overridden)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static boolean isInvokeCallOnVariable(@NotNull Call call) {
        if (call.getCallType() != Call.CallType.INVOKE) return false;
        ReceiverValue dispatchReceiver = call.getDispatchReceiver();
        //calleeExpressionAsDispatchReceiver for invoke is always ExpressionReceiver, see CallForImplicitInvoke
        JetExpression expression = ((ExpressionReceiver) dispatchReceiver).getExpression();
        return expression instanceof JetSimpleNameExpression;
    }

    public static boolean isInvokeCallOnExpressionWithBothReceivers(@NotNull Call call) {
        if (call.getCallType() != Call.CallType.INVOKE || isInvokeCallOnVariable(call)) return false;
        return call.getExplicitReceiver().exists() && call.getDispatchReceiver().exists();
    }

    public static JetSuperExpression getSuperCallExpression(@NotNull Call call) {
        ReceiverValue explicitReceiver = call.getExplicitReceiver();
        if (explicitReceiver instanceof ExpressionReceiver) {
            JetExpression receiverExpression = ((ExpressionReceiver) explicitReceiver).getExpression();
            if (receiverExpression instanceof JetSuperExpression) {
                return (JetSuperExpression) receiverExpression;
            }
        }
        return null;
    }
}
