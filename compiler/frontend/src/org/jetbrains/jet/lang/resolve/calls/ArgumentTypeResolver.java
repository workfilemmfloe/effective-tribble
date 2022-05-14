/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.TemporaryBindingTrace;
import org.jetbrains.jet.lang.resolve.TypeResolver;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallImpl;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeInfo;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.*;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.ResolveMode.RESOLVE_FUNCTION_ARGUMENTS;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.ResolveMode.SKIP_FUNCTION_ARGUMENTS;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

public class ArgumentTypeResolver {
    @NotNull
    private final JetTypeChecker typeChecker = JetTypeChecker.INSTANCE;

    @NotNull
    private TypeResolver typeResolver;
    @NotNull
    private ExpressionTypingServices expressionTypingServices;

    @Inject
    public void setTypeResolver(@NotNull TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    public boolean isSubtypeOfForArgumentType(@NotNull JetType subtype, @NotNull JetType supertype) {
        if (subtype == PLACEHOLDER_FUNCTION_TYPE) {
            return isFunctionOrErrorType(supertype) || KotlinBuiltIns.getInstance().isAny(supertype); //todo function type extends
        }
        if (supertype == PLACEHOLDER_FUNCTION_TYPE) {
            return isFunctionOrErrorType(subtype); //todo extends function type
        }
        return typeChecker.isSubtypeOf(subtype, supertype);
    }

    private static boolean isFunctionOrErrorType(@NotNull JetType supertype) {
        return KotlinBuiltIns.getInstance().isFunctionOrExtensionFunctionType(supertype) || ErrorUtils.isErrorType(supertype);
    }

    public void checkTypesWithNoCallee(@NotNull ResolutionContext context) {
        checkTypesWithNoCallee(context, SKIP_FUNCTION_ARGUMENTS);
    }

    public void checkTypesWithNoCallee(@NotNull ResolutionContext context, @NotNull ResolveMode resolveFunctionArgumentBodies) {
        for (ValueArgument valueArgument : context.call.getValueArguments()) {
            JetExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression != null && !(argumentExpression instanceof JetFunctionLiteralExpression)) {
                expressionTypingServices.getType(context.scope, argumentExpression, NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace);
            }
        }

        if (resolveFunctionArgumentBodies == RESOLVE_FUNCTION_ARGUMENTS) {
            checkTypesForFunctionArgumentsWithNoCallee(context);
        }

        for (JetTypeProjection typeProjection : context.call.getTypeArguments()) {
            JetTypeReference typeReference = typeProjection.getTypeReference();
            if (typeReference == null) {
                context.trace.report(Errors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(typeProjection));
            }
            else {
                typeResolver.resolveType(context.scope, typeReference, context.trace, true);
            }
        }
    }

    public void checkTypesForFunctionArgumentsWithNoCallee(@NotNull ResolutionContext context) {
        for (ValueArgument valueArgument : context.call.getValueArguments()) {
            JetExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression != null && (argumentExpression instanceof JetFunctionLiteralExpression)) {
                expressionTypingServices.getType(context.scope, argumentExpression, NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace);
            }
        }

        for (JetExpression expression : context.call.getFunctionLiteralArguments()) {
            expressionTypingServices.getType(context.scope, expression, NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace);
        }
    }

    public void checkUnmappedArgumentTypes(ResolutionContext context, Set<ValueArgument> unmappedArguments) {
        for (ValueArgument valueArgument : unmappedArguments) {
            JetExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression != null) {
                expressionTypingServices.getType(context.scope, argumentExpression, NO_EXPECTED_TYPE, context.dataFlowInfo, context.trace);
            }
        }
    }

    public <D extends CallableDescriptor> void checkTypesForFunctionArguments(ResolutionContext context, ResolvedCallImpl<D> resolvedCall) {
        Map<ValueParameterDescriptor, ResolvedValueArgument> arguments = resolvedCall.getValueArguments();
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : arguments.entrySet()) {
            ValueParameterDescriptor valueParameterDescriptor = entry.getKey();
            JetType varargElementType = valueParameterDescriptor.getVarargElementType();
            JetType functionType;
            if (varargElementType != null) {
                functionType = varargElementType;
            }
            else {
                functionType = valueParameterDescriptor.getType();
            }
            ResolvedValueArgument valueArgument = entry.getValue();
            List<ValueArgument> valueArguments = valueArgument.getArguments();
            for (ValueArgument argument : valueArguments) {
                JetExpression expression = argument.getArgumentExpression();
                if (expression instanceof JetFunctionLiteralExpression) {
                    expressionTypingServices.getType(context.scope, expression, functionType, context.dataFlowInfo, context.trace);
                }
            }
        }
    }

    @NotNull
    public JetTypeInfo getArgumentTypeInfo(
            @Nullable JetExpression expression,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            @NotNull ResolveMode resolveFunctionArgumentBodies
    ) {
        if (expression == null) {
            return JetTypeInfo.create(null, dataFlowInfo);
        }
        if (expression instanceof JetFunctionLiteralExpression && resolveFunctionArgumentBodies == SKIP_FUNCTION_ARGUMENTS) {
            JetType type = getFunctionLiteralType((JetFunctionLiteralExpression) expression, scope, trace);
            return JetTypeInfo.create(type, dataFlowInfo);
        }
        return expressionTypingServices.getTypeInfo(scope, expression, expectedType, dataFlowInfo, trace);
    }

    @Nullable
    private JetType getFunctionLiteralType(
            @NotNull JetFunctionLiteralExpression expression,
            @NotNull JetScope scope,
            @NotNull BindingTrace trace
    ) {
        List<JetParameter> valueParameters = expression.getValueParameters();
        if (valueParameters.isEmpty()) {
            return PLACEHOLDER_FUNCTION_TYPE;
        }
        TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(
                trace, "trace to resolve function literal parameter types");
        List<JetType> parameterTypes = Lists.newArrayList();
        for (JetParameter parameter : valueParameters) {
            parameterTypes.add(resolveTypeRefWithDefault(parameter.getTypeReference(), scope, temporaryTrace, DONT_CARE));
        }
        JetFunctionLiteral functionLiteral = expression.getFunctionLiteral();
        JetType returnType = resolveTypeRefWithDefault(functionLiteral.getReturnTypeRef(), scope, temporaryTrace, DONT_CARE);
        assert returnType != null;
        JetType receiverType = resolveTypeRefWithDefault(functionLiteral.getReceiverTypeRef(), scope, temporaryTrace, null);
        return KotlinBuiltIns.getInstance().getFunctionType(Collections.<AnnotationDescriptor>emptyList(), receiverType, parameterTypes, returnType);
    }

    @Nullable
    public JetType resolveTypeRefWithDefault(
            @Nullable JetTypeReference returnTypeRef,
            @NotNull JetScope scope,
            @NotNull BindingTrace trace,
            @Nullable JetType defaultValue
    ) {
        if (returnTypeRef != null) {
            return expressionTypingServices.getTypeResolver().resolveType(scope, returnTypeRef, trace, true);
        }
        return defaultValue;
    }
}
