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

package org.jetbrains.jet.lang.resolve.calls.tasks;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.jet.lang.types.ErrorUtils;

import java.util.Collection;

import static org.jetbrains.jet.lang.diagnostics.Errors.UNRESOLVED_REFERENCE;
import static org.jetbrains.jet.lang.diagnostics.Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;

public class TracingStrategyImpl extends AbstractTracingStrategy {
    private final JetReferenceExpression reference;

    private TracingStrategyImpl(@NotNull JetReferenceExpression reference, @NotNull Call call) {
        super(reference, call);
        this.reference = reference;
    }

    @NotNull
    public static TracingStrategy create(@NotNull JetReferenceExpression reference, @NotNull Call call) {
        return new TracingStrategyImpl(reference, call);
    }

    @Override
    public <D extends CallableDescriptor> void bindReference(@NotNull BindingTrace trace, @NotNull ResolvedCallWithTrace<D> resolvedCall) {
        CallableDescriptor descriptor = resolvedCall.getCandidateDescriptor();
        if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
            descriptor = ((VariableAsFunctionResolvedCall) resolvedCall).getVariableCall().getCandidateDescriptor();
        }
        DeclarationDescriptor storedReference = trace.get(REFERENCE_TARGET, reference);
        if (storedReference == null || !ErrorUtils.isError(descriptor)) {
            trace.record(REFERENCE_TARGET, reference, descriptor);
        }
    }

    @Override
    public <D extends CallableDescriptor> void bindResolvedCall(@NotNull BindingTrace trace, @NotNull ResolvedCallWithTrace<D> resolvedCall) {
        trace.record(RESOLVED_CALL, call.getCalleeExpression(), resolvedCall);
        trace.record(CALL, call.getCalleeExpression(), call);
    }

    @Override
    public void unresolvedReference(@NotNull BindingTrace trace) {
        trace.report(UNRESOLVED_REFERENCE.on(reference, reference));
    }

    @Override
    public <D extends CallableDescriptor> void unresolvedReferenceWrongReceiver(@NotNull BindingTrace trace, @NotNull Collection<ResolvedCallWithTrace<D>> candidates) {
        trace.report(UNRESOLVED_REFERENCE_WRONG_RECEIVER.on(reference, candidates));
    }
}
