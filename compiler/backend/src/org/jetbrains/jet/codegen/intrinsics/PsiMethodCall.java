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

package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.CallableMethod;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.OwnerKind;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.psi.JetCallElement;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.List;

/**
 * @author yole
 * @author alex.tkachman
 */
public class PsiMethodCall implements IntrinsicMethod {
    private final SimpleFunctionDescriptor method;

    public PsiMethodCall(SimpleFunctionDescriptor method) {
        this.method = method;
    }

    @Override
    public StackValue generate(
            ExpressionCodegen codegen, InstructionAdapter v, @NotNull Type expectedType, PsiElement element,
            List<JetExpression> arguments, StackValue receiver, @NotNull GenerationState state
    ) {
        final CallableMethod callableMethod =
                state.getTypeMapper().mapToCallableMethod(method, false, false, OwnerKind.IMPLEMENTATION);
        if(element instanceof JetBinaryExpression) {
            codegen.invokeMethodWithArguments(callableMethod, receiver, ((JetBinaryExpression)element).getOperationReference());
        }  else {
            codegen.invokeMethodWithArguments(callableMethod, (JetCallElement) element, receiver);
        }
        return StackValue.onStack(callableMethod.getSignature().getAsmMethod().getReturnType());
    }
}
