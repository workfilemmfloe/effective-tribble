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

package org.jetbrains.jet.codegen.intrinsics

import com.intellij.psi.PsiElement
import org.jetbrains.jet.codegen.ExpressionCodegen
import org.jetbrains.jet.codegen.StackValue
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.org.objectweb.asm.Type

import org.jetbrains.jet.codegen.AsmUtil.genIncrement
import org.jetbrains.jet.codegen.AsmUtil.isPrimitive

public class Increment(private val myDelta: Int) : LazyIntrinsicMethod() {

    override fun generateImpl(codegen: ExpressionCodegen, returnType: Type, element: PsiElement?, arguments: List<JetExpression>, receiver: StackValue): StackValue {
        assert(isPrimitive(returnType)) { "Return type of Increment intrinsic should be of primitive type : " + returnType }

        if (arguments.size() > 0) {
            val operand = arguments.get(0)
            val stackValue = codegen.genQualified(receiver, operand)
            if (stackValue is StackValue.Local && Type.INT_TYPE == stackValue.type) {
                return StackValue.preIncrementForLocalVar((stackValue as StackValue.Local).index, myDelta)
            }
            else {
                return StackValue.preIncrement(returnType, stackValue, myDelta, this, null, codegen)
            }
        }
        else {
            return StackValue.operation(returnType) {
                receiver.put(returnType, it)
                genIncrement(returnType, myDelta, it)
            }
        }
    }
}
