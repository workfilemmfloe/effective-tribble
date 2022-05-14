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

package org.jetbrains.kotlin.types.expressions

import com.google.common.collect.Lists
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.TraceEntryFilter
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.util.slicedMap.WritableSlice
import java.util.*

public class FakeCallResolver(
        private val project: Project,
        private val callResolver: CallResolver
) {
    public fun resolveFakeCall(
            context: ExpressionTypingContext,
            receiver: ReceiverValue,
            name: Name,
            callElement: JetExpression?,
            vararg argumentTypes: JetType
    ): OverloadResolutionResults<FunctionDescriptor> {
        val traceWithFakeArgumentInfo = TemporaryBindingTrace.create(context.trace, "trace to store fake argument for", name)
        val fakeArguments = ArrayList<JetExpression>()
        for (type in argumentTypes) {
            fakeArguments.add(ExpressionTypingUtils.createFakeExpressionOfType(project, traceWithFakeArgumentInfo, "fakeArgument" + fakeArguments.size(), type))
        }
        return makeAndResolveFakeCall(receiver, context.replaceBindingTrace(traceWithFakeArgumentInfo), fakeArguments, name, callElement).second
    }

    public fun resolveFakeCall(
            context: ExpressionTypingContext,
            receiver: ReceiverValue,
            name: Name,
            callElement: JetExpression
    ): OverloadResolutionResults<FunctionDescriptor> {
        return resolveFakeCall(receiver, context, emptyList(), name, callElement)
    }

    public fun resolveFakeCall(
            receiver: ReceiverValue,
            context: ExpressionTypingContext,
            valueArguments: List<JetExpression>,
            name: Name,
            callElement: JetExpression
    ): OverloadResolutionResults<FunctionDescriptor> {
        return makeAndResolveFakeCall(receiver, context, valueArguments, name, callElement).second
    }

    public fun makeAndResolveFakeCall(
            receiver: ReceiverValue,
            context: ExpressionTypingContext,
            valueArguments: List<JetExpression>,
            name: Name,
            callElement: JetExpression?
    ): Pair<Call, OverloadResolutionResults<FunctionDescriptor>> {
        val fake = JetPsiFactory(project).createSimpleName("fake")
        val fakeTrace = TemporaryBindingTrace.create(context.trace, "trace to resolve fake call for", name)
        val call = CallMaker.makeCallWithExpressions(callElement ?: fake, receiver, null, fake, valueArguments)
        val results = callResolver.resolveCallWithGivenName(context.replaceBindingTrace(fakeTrace), call, fake, name)
        if (results.isSuccess()) {
            fakeTrace.commit({ _, key ->
                // excluding all entries related to fake expression
                key != fake
            }, true)
        }
        return Pair(call, results)
    }

}