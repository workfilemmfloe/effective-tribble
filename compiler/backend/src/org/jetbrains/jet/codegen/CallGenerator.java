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

package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;

public interface CallGenerator {

    class DefaultCallGenerator implements CallGenerator {

        private final ExpressionCodegen codegen;

        public DefaultCallGenerator(ExpressionCodegen codegen) {
            this.codegen = codegen;
        }

        @Override
        public void genCall(
                @NotNull CallableMethod callableMethod,
                ResolvedCall<?> resolvedCall,
                boolean callDefault,
                @NotNull ExpressionCodegen codegen
        ) {
            if (!callDefault) {
                callableMethod.invokeWithNotNullAssertion(codegen.v, codegen.getState(), resolvedCall);
            }
            else {
                callableMethod.invokeDefaultWithNotNullAssertion(codegen.v, codegen.getState(), resolvedCall);
            }
        }

        @Override
        public void genCallWithoutAssertions(
                @NotNull CallableMethod method, @NotNull ExpressionCodegen codegen
        ) {
            method.invokeWithoutAssertions(codegen.v);
        }

        @Override
        public void afterParameterPut(@NotNull Type type, StackValue stackValue, @NotNull ValueParameterDescriptor valueParameterDescriptor) {

        }

        @Override
        public void putHiddenParams() {

        }

        @Override
        public void genValueAndPut(
                @NotNull ValueParameterDescriptor valueParameterDescriptor,
                @NotNull JetExpression argumentExpression,
                @NotNull Type parameterType
        ) {
            StackValue value = codegen.gen(argumentExpression);
            value.put(parameterType, codegen.v);
        }

        @Override
        public void putCapturedValueOnStack(
                @NotNull StackValue stackValue, @NotNull Type valueType, int paramIndex
        ) {
            stackValue.put(stackValue.type, codegen.v);
        }

        @Override
        public void putValueIfNeeded(
                @Nullable ValueParameterDescriptor valueParameterDescriptor, @NotNull Type parameterType, @NotNull StackValue value
        ) {
            value.put(value.type, codegen.v);
        }
    }

    void genCall(@NotNull CallableMethod callableMethod, @Nullable ResolvedCall<?> resolvedCall, boolean callDefault, @NotNull ExpressionCodegen codegen);

    void genCallWithoutAssertions(@NotNull CallableMethod callableMethod, @NotNull ExpressionCodegen codegen);

    void afterParameterPut(@NotNull Type type, StackValue stackValue, @NotNull ValueParameterDescriptor valueParameterDescriptor);

    void genValueAndPut(
            @NotNull ValueParameterDescriptor valueParameterDescriptor,
            @NotNull JetExpression argumentExpression,
            @NotNull Type parameterType
    );

    void putValueIfNeeded(@Nullable ValueParameterDescriptor valueParameterDescriptor, @NotNull Type parameterType, @NotNull StackValue value);

    void putCapturedValueOnStack(
            @NotNull StackValue stackValue,
            @NotNull Type valueType, int paramIndex
    );

    void putHiddenParams();
}
