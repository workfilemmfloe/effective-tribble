/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.model.DefaultValueArgument;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.InlineUtil;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.InlineDescriptorUtils.allowsNonLocalReturns;
import static org.jetbrains.jet.lang.resolve.InlineDescriptorUtils.checkNonLocalReturnUsage;

public class InlineCallResolverExtension implements CallResolverExtension {

    private final SimpleFunctionDescriptor descriptor;

    private final Set<CallableDescriptor> inlinableParameters = new HashSet<CallableDescriptor>();

    private final boolean isEffectivelyPublicApiFunction;

    public InlineCallResolverExtension(@NotNull SimpleFunctionDescriptor descriptor) {
        assert descriptor.getInlineStrategy().isInline() : "This extension should be created only for inline functions but not " + descriptor;
        this.descriptor = descriptor;
        this.isEffectivelyPublicApiFunction = isEffectivelyPublicApi(descriptor);

        for (ValueParameterDescriptor param : descriptor.getValueParameters()) {
            if (isInlinableParameter(param)) {
                inlinableParameters.add(param);
            }
        }

        //add extension receiver as inlinable
        ReceiverParameterDescriptor receiverParameter = descriptor.getReceiverParameter();
        if (receiverParameter != null) {
            if (isInlinableParameter(receiverParameter)) {
                inlinableParameters.add(receiverParameter);
            }
        }
    }

    @Override
    public <F extends CallableDescriptor> void run(@NotNull ResolvedCall<F> resolvedCall, @NotNull BasicCallResolutionContext context) {
        JetExpression expression = context.call.getCalleeExpression();
        if (expression == null) {
            return;
        }

        //checking that only invoke or inlinable extension called on function parameter
        CallableDescriptor targetDescriptor = resolvedCall.getResultingDescriptor();
        checkCallWithReceiver(context, targetDescriptor, resolvedCall.getThisObject(), expression);
        checkCallWithReceiver(context, targetDescriptor, resolvedCall.getReceiverArgument(), expression);

        if (inlinableParameters.contains(targetDescriptor)) {
            if (!isInsideCall(expression)) {
                context.trace.report(Errors.USAGE_IS_NOT_INLINABLE.on(expression, expression, descriptor));
            }
        }

        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : resolvedCall.getValueArguments().entrySet()) {
            ResolvedValueArgument value = entry.getValue();
            ValueParameterDescriptor valueDescriptor = entry.getKey();
            if (!(value instanceof DefaultValueArgument)) {
                for (ValueArgument argument : value.getArguments()) {
                    checkValueParameter(context, targetDescriptor, argument, valueDescriptor);
                }
            }
        }

        checkVisibility(targetDescriptor, expression, context);
        checkRecursion(context, targetDescriptor, expression);
    }

    private static boolean isInsideCall(JetExpression expression) {
        JetElement parent = JetPsiUtil.getParentCallIfPresent(expression);
        if (parent instanceof JetBinaryExpression) {
            JetToken token = JetPsiUtil.getOperationToken((JetOperationExpression) parent);
            if (token == JetTokens.EQ || token == JetTokens.ANDAND || token == JetTokens.OROR) {
                //assignment
                return false;
            }
        }

        return parent != null;
    }



    private void checkValueParameter(
            @NotNull BasicCallResolutionContext context,
            @NotNull CallableDescriptor targetDescriptor,
            @NotNull ValueArgument targetArgument,
            @NotNull ValueParameterDescriptor targetParameterDescriptor
    ) {
        JetExpression argumentExpression = targetArgument.getArgumentExpression();
        if (argumentExpression == null) {
            return;
        }
        CallableDescriptor argumentCallee = getCalleeDescriptor(context, argumentExpression, false);

        if (argumentCallee != null && inlinableParameters.contains(argumentCallee)) {
            boolean isTargetInlineFunction = targetDescriptor instanceof SimpleFunctionDescriptor &&
                                             ((SimpleFunctionDescriptor) targetDescriptor).getInlineStrategy().isInline();

            if (!isTargetInlineFunction || !isInlinableParameter(targetParameterDescriptor)) {
                context.trace.report(Errors.USAGE_IS_NOT_INLINABLE.on(argumentExpression, argumentExpression, descriptor));
            } else {
                if (allowsNonLocalReturns(argumentCallee) && !allowsNonLocalReturns(targetParameterDescriptor)) {
                    context.trace.report(Errors.NON_LOCAL_RETURN_NOT_ALLOWED.on(argumentExpression, argumentExpression, argumentCallee, descriptor));
                } else {
                    checkNonLocalReturn(context, argumentCallee, argumentExpression);
                }
            }
        }
    }

    private void checkCallWithReceiver(
            @NotNull BasicCallResolutionContext context,
            @NotNull CallableDescriptor targetDescriptor,
            @NotNull ReceiverValue receiver,
            @Nullable JetExpression expression
    ) {
        if (!receiver.exists()) return;

        CallableDescriptor varDescriptor = null;
        JetExpression receiverExpression = null;
        if (receiver instanceof ExpressionReceiver) {
            receiverExpression = ((ExpressionReceiver) receiver).getExpression();
            varDescriptor = getCalleeDescriptor(context, receiverExpression, true);
        }
        else if (receiver instanceof ExtensionReceiver) {
            ExtensionReceiver extensionReceiver = (ExtensionReceiver) receiver;
            CallableDescriptor extension = extensionReceiver.getDeclarationDescriptor();

            varDescriptor = extension.getReceiverParameter();
            assert varDescriptor != null : "Extension should have receiverParameterDescriptor: " + extension;

            receiverExpression = expression;
        }

        if (inlinableParameters.contains(varDescriptor)) {
            //check that it's invoke or inlinable extension
            checkLambdaInvokeOrExtensionCall(context, varDescriptor, targetDescriptor, receiverExpression);
        }
    }

    @Nullable
    private static CallableDescriptor getCalleeDescriptor(
            @NotNull BasicCallResolutionContext context,
            @NotNull JetExpression expression,
            boolean unwrapVariableAsFunction
    ) {
        if (!(expression instanceof JetSimpleNameExpression || expression instanceof JetThisExpression)) return null;

        ResolvedCall<?> thisCall = CallUtilPackage.getResolvedCall(expression, context.trace.getBindingContext());
        if (unwrapVariableAsFunction && thisCall instanceof VariableAsFunctionResolvedCall) {
            return ((VariableAsFunctionResolvedCall) thisCall).getVariableCall().getResultingDescriptor();
        }
        return thisCall != null ? thisCall.getResultingDescriptor() : null;
    }

    private void checkLambdaInvokeOrExtensionCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull CallableDescriptor lambdaDescriptor,
            @NotNull CallableDescriptor callDescriptor,
            @NotNull JetExpression receiverExpresssion
    ) {
        boolean inlinableCall = isInvokeOrInlineExtension(callDescriptor);
        if (!inlinableCall) {
            context.trace.report(Errors.USAGE_IS_NOT_INLINABLE.on(receiverExpresssion, receiverExpresssion, descriptor));
        } else {
            checkNonLocalReturn(context, lambdaDescriptor, receiverExpresssion);
        }
    }

    public void checkRecursion(
            @NotNull BasicCallResolutionContext context,
            @NotNull CallableDescriptor targetDescriptor,
            @NotNull JetElement expression
    ) {
        if (targetDescriptor.getOriginal() == descriptor) {
            context.trace.report(Errors.RECURSION_IN_INLINE.on(expression, expression, descriptor));
        }
    }

    private static boolean isInlinableParameter(@NotNull CallableDescriptor descriptor) {
        JetType type = descriptor.getReturnType();
        return type != null &&
               KotlinBuiltIns.getInstance().isExactFunctionOrExtensionFunctionType(type) &&
               !type.isNullable() &&
               !InlineUtil.hasNoinlineAnnotation(descriptor);
    }

    private static boolean isInvokeOrInlineExtension(@NotNull CallableDescriptor descriptor) {
        if (!(descriptor instanceof SimpleFunctionDescriptor)) {
            return false;
        }

        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        boolean isInvoke = descriptor.getName().asString().equals("invoke") &&
                           containingDeclaration instanceof ClassDescriptor &&
                           KotlinBuiltIns.getInstance().isExactFunctionOrExtensionFunctionType(((ClassDescriptor) containingDeclaration).getDefaultType());

        return isInvoke ||
               //or inline extension
               ((SimpleFunctionDescriptor) descriptor).getInlineStrategy().isInline();
    }

    private void checkVisibility(@NotNull CallableDescriptor declarationDescriptor, @NotNull JetElement expression, @NotNull BasicCallResolutionContext context){
        if (isEffectivelyPublicApiFunction && !isEffectivelyPublicApi(declarationDescriptor) && declarationDescriptor.getVisibility() != Visibilities.LOCAL) {
            context.trace.report(Errors.INVISIBLE_MEMBER_FROM_INLINE.on(expression, declarationDescriptor, descriptor));
        }
    }

    private static boolean isEffectivelyPublicApi(DeclarationDescriptorWithVisibility descriptor) {
        DeclarationDescriptorWithVisibility parent = descriptor;
        while (parent != null) {
            if (!parent.getVisibility().isPublicAPI()) {
                return false;
            }
            parent = DescriptorUtils.getParentOfType(parent, DeclarationDescriptorWithVisibility.class);
        }
        return true;
    }

    private void checkNonLocalReturn(
            @NotNull BasicCallResolutionContext context,
            @NotNull CallableDescriptor inlinableParameterDescriptor,
            @NotNull JetExpression parameterUsage
    ) {
        if (!allowsNonLocalReturns(inlinableParameterDescriptor)) return;

        if (!checkNonLocalReturnUsage(descriptor, parameterUsage, context.trace)) {
            context.trace.report(Errors.NON_LOCAL_RETURN_NOT_ALLOWED.on(parameterUsage, parameterUsage, inlinableParameterDescriptor, descriptor));
        }
    }

    @Nullable
    public static PsiElement getDeclaration(JetExpression expression) {
        do {
            expression = PsiTreeUtil.getParentOfType(expression, JetDeclaration.class);
        } while (expression instanceof JetMultiDeclaration || expression instanceof JetProperty);
        return expression;
    }

}
