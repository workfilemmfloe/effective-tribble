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

package org.jetbrains.jet.lang.resolve.calls.model;

import com.google.common.collect.Maps;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.tasks.ExplicitReceiverKind;
import org.jetbrains.jet.lang.resolve.calls.tasks.ResolutionCandidate;
import org.jetbrains.jet.lang.resolve.calls.results.ResolutionStatus;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.calls.results.ResolutionStatus.UNKNOWN_STATUS;

public class ResolvedCallImpl<D extends CallableDescriptor> implements ResolvedCallWithTrace<D> {

    public static final Function<ResolvedCallWithTrace<? extends CallableDescriptor>, CallableDescriptor> MAP_TO_CANDIDATE = new Function<ResolvedCallWithTrace<? extends CallableDescriptor>, CallableDescriptor>() {
        @Override
        public CallableDescriptor fun(ResolvedCallWithTrace<? extends CallableDescriptor> resolvedCall) {
            return resolvedCall.getCandidateDescriptor();
        }
    };

    public static final Function<ResolvedCallWithTrace<? extends CallableDescriptor>, CallableDescriptor> MAP_TO_RESULT = new Function<ResolvedCallWithTrace<? extends CallableDescriptor>, CallableDescriptor>() {
        @Override
        public CallableDescriptor fun(ResolvedCallWithTrace<? extends CallableDescriptor> resolvedCall) {
            return resolvedCall.getResultingDescriptor();
        }
    };

    @NotNull
    public static <D extends CallableDescriptor> ResolvedCallImpl<D> create(@NotNull ResolutionCandidate<D> candidate, @NotNull DelegatingBindingTrace trace) {
        return new ResolvedCallImpl<D>(candidate, trace);
    }

    private final D candidateDescriptor;
    private D resultingDescriptor; // Probably substituted
    private final ReceiverValue thisObject; // receiver object of a method
    private final ReceiverValue receiverArgument; // receiver of an extension function
    private final ExplicitReceiverKind explicitReceiverKind;
    private final boolean isSafeCall;

    private final Map<TypeParameterDescriptor, JetType> typeArguments = Maps.newLinkedHashMap();
    private final Map<ValueParameterDescriptor, ResolvedValueArgument> valueArguments = Maps.newLinkedHashMap();
    private boolean someArgumentHasNoType = false;
    private final DelegatingBindingTrace trace;
    private ResolutionStatus status = UNKNOWN_STATUS;
    private boolean hasUnknownTypeParameters = false;
    private ConstraintSystem constraintSystem = null;

    private ResolvedCallImpl(@NotNull ResolutionCandidate<D> candidate, @NotNull DelegatingBindingTrace trace) {
        this.candidateDescriptor = candidate.getDescriptor();
        this.thisObject = candidate.getThisObject();
        this.receiverArgument = candidate.getReceiverArgument();
        this.explicitReceiverKind = candidate.getExplicitReceiverKind();
        this.isSafeCall = candidate.isSafeCall();
        this.trace = trace;
    }

    @Override
    @NotNull
    public ResolutionStatus getStatus() {
        return status;
    }

    public void addStatus(@NotNull ResolutionStatus status) {
        this.status = this.status.combine(status);
    }

    @Override
    public boolean hasUnknownTypeParameters() {
        return hasUnknownTypeParameters;
    }

    public void setHasUnknownTypeParameters(boolean hasUnknownTypeParameters) {
        this.hasUnknownTypeParameters = hasUnknownTypeParameters;
    }

    @Override
    @NotNull
    public DelegatingBindingTrace getTrace() {
        return trace;
    }

    @Override
    @NotNull
    public D getCandidateDescriptor() {
        return candidateDescriptor;
    }

    @Override
    @NotNull
    public D getResultingDescriptor() {
        return resultingDescriptor == null ? candidateDescriptor : resultingDescriptor;
    }

    public void setResultingSubstitutor(@NotNull TypeSubstitutor substitutor) {
        resultingDescriptor = (D) candidateDescriptor.substitute(substitutor);
        assert resultingDescriptor != null : candidateDescriptor;

        Map<ValueParameterDescriptor, ValueParameterDescriptor> parameterMap = Maps.newHashMap();
        for (ValueParameterDescriptor valueParameterDescriptor : resultingDescriptor.getValueParameters()) {
            parameterMap.put(valueParameterDescriptor.getOriginal(), valueParameterDescriptor);
        }

        Map<ValueParameterDescriptor, ResolvedValueArgument> originalValueArguments = Maps.newHashMap(valueArguments);
        valueArguments.clear();
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : originalValueArguments.entrySet()) {
            ValueParameterDescriptor substitutedVersion = parameterMap.get(entry.getKey().getOriginal());
            assert substitutedVersion != null : entry.getKey();
            valueArguments.put(substitutedVersion, entry.getValue());
        }
    }

    public void recordTypeArgument(@NotNull TypeParameterDescriptor typeParameter, @NotNull JetType typeArgument) {
        assert !typeArguments.containsKey(typeParameter) : typeParameter + " -> " + typeArgument;
        typeArguments.put(typeParameter, typeArgument);
    }

    public void setConstraintSystem(@NotNull ConstraintSystem constraintSystem) {
        this.constraintSystem = constraintSystem;
    }

    @Nullable
    public ConstraintSystem getConstraintSystem() {
        return constraintSystem;
    }

    public void recordValueArgument(@NotNull ValueParameterDescriptor valueParameter, @NotNull ResolvedValueArgument valueArgument) {
        assert !valueArguments.containsKey(valueParameter) : valueParameter + " -> " + valueArgument;
        valueArguments.put(valueParameter, valueArgument);
    }

    @Override
    @NotNull
    public ReceiverValue getReceiverArgument() {
        return receiverArgument;
    }

    @Override
    @NotNull
    public ReceiverValue getThisObject() {
        return thisObject;
    }

    @Override
    @NotNull
    public ExplicitReceiverKind getExplicitReceiverKind() {
        return explicitReceiverKind;
    }

    @Override
    @NotNull
    public Map<ValueParameterDescriptor, ResolvedValueArgument> getValueArguments() {
        return valueArguments;
    }

    @NotNull
    @Override
    public List<ResolvedValueArgument> getValueArgumentsByIndex() {
        List<ResolvedValueArgument> arguments = new ArrayList<ResolvedValueArgument>(candidateDescriptor.getValueParameters().size());
        for (int i = 0; i < candidateDescriptor.getValueParameters().size(); ++i) {
            arguments.add(null);
        }
        
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : valueArguments.entrySet()) {
            if (arguments.set(entry.getKey().getIndex(), entry.getValue()) != null) {
                throw new IllegalStateException();
            }
        }

        for (Object o : arguments) {
            if (o == null) {
                throw new IllegalStateException();
            }
        }
        
        return arguments;
    }

    public void argumentHasNoType() {
        this.someArgumentHasNoType = true;
    }

    @Override
    public boolean isDirty() {
        return someArgumentHasNoType;
    }

    @NotNull
    @Override
    public Map<TypeParameterDescriptor, JetType> getTypeArguments() {
        return typeArguments;
    }

    @Override
    public boolean isSafeCall() {
        return isSafeCall;
    }
}
