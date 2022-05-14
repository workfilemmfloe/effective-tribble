/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.jet.lang.resolve.calls.model.ArgumentMapping;
import org.jetbrains.jet.lang.resolve.calls.model.ArgumentMatch;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.types.lang.InlineUtil;

public class InlineDescriptorUtils {

    public static boolean checkNonLocalReturnUsage(@NotNull DeclarationDescriptor fromFunction, @NotNull JetExpression startExpression, @NotNull BindingTrace trace) {
        PsiElement containingFunction = PsiTreeUtil.getParentOfType(startExpression, JetClassOrObject.class, JetDeclarationWithBody.class);
        if (containingFunction == null) {
            return false;
        }

        DeclarationDescriptor containingFunctionDescriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, containingFunction);
        if (containingFunctionDescriptor == null) {
            return false;
        }

        BindingContext bindingContext = trace.getBindingContext();

        while (containingFunction instanceof JetFunctionLiteral && fromFunction != containingFunctionDescriptor) {
            //JetFunctionLiteralExpression
            containingFunction = containingFunction.getParent();
            if (!isInlineLambda((JetFunctionLiteralExpression) containingFunction, bindingContext, true)) {
                return false;
            }

            containingFunctionDescriptor = getContainingClassOrFunctionDescriptor(containingFunctionDescriptor, true);

            containingFunction = containingFunctionDescriptor != null
                                 ? DescriptorToSourceUtils.descriptorToDeclaration(containingFunctionDescriptor)
                                 : null;
        }

        return fromFunction == containingFunctionDescriptor;
    }

    public static boolean isInlineLambda(
            @NotNull JetFunctionLiteralExpression lambdaExpression,
            @NotNull BindingContext bindingContext,
            boolean checkNonLocalReturn
    ) {
        JetExpression call = JetPsiUtil.getParentCallIfPresent(lambdaExpression);
        if (call != null) {
            ResolvedCall<?> resolvedCall = CallUtilPackage.getResolvedCall(call, bindingContext);
            CallableDescriptor resultingDescriptor = resolvedCall == null ? null : resolvedCall.getResultingDescriptor();
            if (resultingDescriptor instanceof SimpleFunctionDescriptor &&
                ((SimpleFunctionDescriptor) resultingDescriptor).getInlineStrategy().isInline()) {
                ValueArgument argument = CallUtilPackage.getValueArgumentForExpression(resolvedCall.getCall(), lambdaExpression);
                if (argument != null) {
                    ArgumentMapping mapping = resolvedCall.getArgumentMapping(argument);
                    if (mapping instanceof ArgumentMatch) {
                        ValueParameterDescriptor parameter = ((ArgumentMatch) mapping).getValueParameter();
                        if (!InlineUtil.hasNoinlineAnnotation(parameter)) {
                            return !checkNonLocalReturn || allowsNonLocalReturns(parameter);
                        }
                    }
                }
            }
        }
        return false;
    }

    @Nullable
    public static DeclarationDescriptor getContainingClassOrFunctionDescriptor(@NotNull DeclarationDescriptor descriptor, boolean strict) {
        DeclarationDescriptor currentDescriptor = strict ? descriptor.getContainingDeclaration() : descriptor;
        while (currentDescriptor != null) {
            if (currentDescriptor instanceof FunctionDescriptor || currentDescriptor instanceof ClassDescriptor) {
                return currentDescriptor;
            }
            currentDescriptor = currentDescriptor.getContainingDeclaration();
        }

        return null;
    }

    public static boolean allowsNonLocalReturns(@NotNull CallableDescriptor lambdaDescriptor) {
        if (lambdaDescriptor instanceof ValueParameterDescriptor) {
            if (InlineUtil.hasOnlyLocalReturn((ValueParameterDescriptor) lambdaDescriptor)) {
                //annotated
                return false;
            }
        }
        return true;
    }
}
