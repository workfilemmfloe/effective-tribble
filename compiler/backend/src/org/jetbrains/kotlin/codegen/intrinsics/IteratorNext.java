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
import org.jetbrains.kotlin.codegen.ExpressionCodegen;
import org.jetbrains.kotlin.codegen.StackValue;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME;

public class IteratorNext extends IntrinsicMethod {
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
        String name;
        if (returnType == Type.CHAR_TYPE) {
            name = "Char";
        }
        else if (returnType == Type.BOOLEAN_TYPE) {
            name = "Boolean";
        }
        else if (returnType == Type.BYTE_TYPE) {
            name = "Byte";
        }
        else if (returnType == Type.SHORT_TYPE) {
            name = "Short";
        }
        else if (returnType == Type.INT_TYPE) {
            name = "Int";
        }
        else if (returnType == Type.LONG_TYPE) {
            name = "Long";
        }
        else if (returnType == Type.FLOAT_TYPE) {
            name = "Float";
        }
        else if (returnType == Type.DOUBLE_TYPE) {
            name = "Double";
        }
        else {
            throw new UnsupportedOperationException();
        }
        receiver.put(AsmTypes.OBJECT_TYPE, v);
        v.invokevirtual(BUILT_INS_PACKAGE_FQ_NAME + "/" + name + "Iterator", "next" + name, "()" + returnType.getDescriptor(), false);
        return returnType;
    }
}
