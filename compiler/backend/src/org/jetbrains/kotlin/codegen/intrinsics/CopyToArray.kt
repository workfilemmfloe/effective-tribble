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

package org.jetbrains.kotlin.codegen.intrinsics

import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.getType
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

public class CopyToArray : IntrinsicMethod() {

    override fun toCallable(fd: FunctionDescriptor, isSuper: Boolean, resolvedCall: ResolvedCall<*>, codegen: ExpressionCodegen): Callable {
        return object : IntrinsicCallable(getType(javaClass<Array<Any>>()), listOf(), null, Type.getType(javaClass<Collection<*>>())) {
            override fun invokeIntrinsic(v: InstructionAdapter) {
                v.dup()
                v.invokeinterface("java/util/Collection", "size", "()I")
                codegen.newArrayInstruction(resolvedCall.getResultingDescriptor().getReturnType()!!)
                v.invokeinterface("java/util/Collection", "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;")
            }
        }
    }
}