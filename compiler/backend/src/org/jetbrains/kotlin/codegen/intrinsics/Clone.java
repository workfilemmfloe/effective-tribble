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

package org.jetbrains.kotlin.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.ExpressionCodegen;
import org.jetbrains.kotlin.codegen.StackValue;
import org.jetbrains.kotlin.psi.JetElement;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetSuperExpression;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

import static org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage.getResolvedCallWithAssert;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE;

public class Clone extends IntrinsicMethod {
    @NotNull
    @Override
    protected Type generateImpl(
            @NotNull ExpressionCodegen codegen,
            @NotNull InstructionAdapter v,
            @NotNull Type returnType,
            @Nullable PsiElement element,
            @NotNull List<JetExpression> arguments,
            @NotNull StackValue receiver
    ) {
        ResolvedCall<?> resolvedCall = getResolvedCallWithAssert(((JetElement) element), codegen.getBindingContext());
        StackValue.receiver(resolvedCall, receiver, codegen, null).put(OBJECT_TYPE, v);
        if (isSuperCall(resolvedCall)) {
            v.invokespecial("java/lang/Object", "clone", "()Ljava/lang/Object;", false);
        }
        else {
            v.invokevirtual("java/lang/Object", "clone", "()Ljava/lang/Object;", false);
        }
        return OBJECT_TYPE;
    }

    private static boolean isSuperCall(@NotNull ResolvedCall<?> resolvedCall) {
        ReceiverValue dispatchReceiver = resolvedCall.getDispatchReceiver();
        return dispatchReceiver instanceof ExpressionReceiver &&
               ((ExpressionReceiver) dispatchReceiver).getExpression() instanceof JetSuperExpression;
    }
}
