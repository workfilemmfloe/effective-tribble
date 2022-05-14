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

package org.jetbrains.kotlin.resolve.calls.model

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace

public interface VariableAsFunctionResolvedCall {
    public val functionCall: ResolvedCall<FunctionDescriptor>
    public val variableCall: ResolvedCall<VariableDescriptor>
}

class VariableAsFunctionResolvedCallImpl(
        override val functionCall: MutableResolvedCall<FunctionDescriptor>,
        override val variableCall: MutableResolvedCall<VariableDescriptor>
) : VariableAsFunctionResolvedCall, MutableResolvedCall<FunctionDescriptor> by functionCall {

    override fun markCallAsCompleted() {
        functionCall.markCallAsCompleted()
        variableCall.markCallAsCompleted()
    }

    override fun isCompleted(): Boolean = functionCall.isCompleted() && variableCall.isCompleted()

    override fun getStatus(): ResolutionStatus = variableCall.getStatus().combine(functionCall.getStatus())

    override fun getTrace(): DelegatingBindingTrace {
        //functionCall.trace is temporary trace above variableCall.trace and is committed already
        return variableCall.getTrace()
    }

}
