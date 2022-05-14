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
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.PropertyCodegen;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.JvmPrimitiveType;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.List;

public class PropertyOfProgressionOrRange implements IntrinsicMethod {
    private final FqName ownerClass;
    private final Name propertyName;

    public PropertyOfProgressionOrRange(@NotNull FqName ownerClass, @NotNull Name propertyName) {
        this.ownerClass = ownerClass;
        this.propertyName = propertyName;
    }

    @Override
    public StackValue generate(
            ExpressionCodegen codegen,
            InstructionAdapter v,
            @NotNull Type expectedType,
            PsiElement element,
            List<JetExpression> arguments,
            StackValue receiver,
            @NotNull GenerationState state
    ) {
        String ownerInternalName = JvmClassName.byFqNameWithoutInnerClasses(this.ownerClass).getInternalName();
        JvmClassName wrapperClass = JvmPrimitiveType.getByAsmType(expectedType).getWrapper();
        String getterName = PropertyCodegen.getterName(propertyName);

        receiver.put(AsmTypeConstants.OBJECT_TYPE, v);
        v.invokevirtual(ownerInternalName, getterName, "()" + wrapperClass.getDescriptor());
        StackValue.coerce(wrapperClass.getAsmType(), expectedType, v);
        return StackValue.onStack(expectedType);
    }
}
