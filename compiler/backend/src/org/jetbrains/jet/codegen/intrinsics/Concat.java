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

package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

import static org.jetbrains.jet.codegen.AsmUtil.genInvokeAppendMethod;
import static org.jetbrains.jet.codegen.AsmUtil.genStringBuilderConstructor;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.JAVA_STRING_TYPE;

public class Concat extends IntrinsicMethod {
    @NotNull
    @Override
    public Type generateImpl(
            @NotNull ExpressionCodegen codegen,
            @NotNull InstructionAdapter v,
            @NotNull Type returnType,
            PsiElement element,
            @NotNull List<JetExpression> arguments,
            @NotNull StackValue receiver
    ) {
        if (receiver == StackValue.none()) {
            // LHS + RHS
            genStringBuilderConstructor(v);
            codegen.invokeAppend(arguments.get(0));
            codegen.invokeAppend(arguments.get(1));
        }
        else {
            // LHS.plus(RHS)
            receiver.put(AsmTypeConstants.OBJECT_TYPE, v);
            genStringBuilderConstructor(v);
            v.swap();
            genInvokeAppendMethod(v, returnType);
            codegen.invokeAppend(arguments.get(0));
        }

        v.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        return JAVA_STRING_TYPE;
    }
}
