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

package org.jetbrains.kotlin.resolve.calls;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.ChainedTemporaryBindingTrace;
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace;
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace;
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.kotlin.resolve.calls.context.CallCandidateResolutionContext;
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency;
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCallImpl;
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCallImpl;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsImpl;
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind;
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionCandidate;
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionTask;
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategyForInvoke;
import org.jetbrains.kotlin.resolve.calls.util.DelegatingCall;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.JetType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

/**
 * CallTransformer treats specially 'variable as function' call case, other cases keeps unchanged (base realization).
 *
 * For the call 'b.foo(1)' where foo is a variable that has method 'invoke' (for example of function type)
 * CallTransformer creates two contexts, two calls in each, and performs second ('invoke') call resolution:
 *
 *   context#1. calls: 'b.foo' 'invoke(1)'
 *   context#2. calls: 'foo'   'b.invoke(1)'
 *
 * If success VariableAsFunctionResolvedCall is created.
 */
public class CallTransformer<D extends CallableDescriptor, F extends D> {
    private CallTransformer() {}

    /**
     * Returns two contexts for 'variable as function' case (in FUNCTION_CALL_TRANSFORMER), one context otherwise
     */
    @NotNull
    public Collection<CallCandidateResolutionContext<D>> createCallContexts(@NotNull ResolutionCandidate<D> candidate,
            @NotNull ResolutionTask<D, F> task,
            @NotNull TemporaryBindingTrace candidateTrace
    ) {
        ResolvedCallImpl<D> candidateCall = ResolvedCallImpl.create(candidate, candidateTrace, task.tracing, task.dataFlowInfoForArguments);
        return Collections.singleton(CallCandidateResolutionContext.create(candidateCall, task, candidateTrace, task.tracing));
    }

    /**
     * Returns collection of resolved calls for 'invoke' for 'variable as function' case (in FUNCTION_CALL_TRANSFORMER),
     * the resolved call from callCandidateResolutionContext otherwise
     */
    @NotNull
    public Collection<MutableResolvedCall<F>> transformCall(@NotNull CallCandidateResolutionContext<D> callCandidateResolutionContext,
            @NotNull CallResolver callResolver,
            @NotNull ResolutionTask<D, F> task
    ) {
        return Collections.singleton((MutableResolvedCall<F>) callCandidateResolutionContext.candidateCall);
    }


    public static CallTransformer<CallableDescriptor, CallableDescriptor> MEMBER_CALL_TRANSFORMER = new CallTransformer<CallableDescriptor, CallableDescriptor>();

    public static CallTransformer<VariableDescriptor, VariableDescriptor> VARIABLE_CALL_TRANSFORMER = new CallTransformer<VariableDescriptor, VariableDescriptor>();

    public static CallTransformer<CallableDescriptor, FunctionDescriptor> FUNCTION_CALL_TRANSFORMER = new CallTransformer<CallableDescriptor, FunctionDescriptor>() {
        @NotNull
        @Override
        public Collection<CallCandidateResolutionContext<CallableDescriptor>> createCallContexts(@NotNull ResolutionCandidate<CallableDescriptor> candidate,
                @NotNull ResolutionTask<CallableDescriptor, FunctionDescriptor> task, @NotNull TemporaryBindingTrace candidateTrace) {

            if (candidate.getDescriptor() instanceof FunctionDescriptor) {
                return super.createCallContexts(candidate, task, candidateTrace);
            }

            assert candidate.getDescriptor() instanceof VariableDescriptor;

            boolean hasReceiver = candidate.getExtensionReceiver().exists();
            Call variableCall = stripCallArguments(task.call);
            ResolutionCandidate<CallableDescriptor> variableCandidate = ResolutionCandidate.create(
                    variableCall,
                    candidate.getDescriptor(),
                    candidate.getDispatchReceiver(),
                    candidate.getExtensionReceiver(),
                    candidate.getExplicitReceiverKind(),
                    null);
            if (!hasReceiver) {
                CallCandidateResolutionContext<CallableDescriptor> context = CallCandidateResolutionContext.create(
                        ResolvedCallImpl.create(variableCandidate, candidateTrace, task.tracing, task.dataFlowInfoForArguments),
                        task, candidateTrace, task.tracing, variableCall);
                return Collections.singleton(context);
            }
            CallCandidateResolutionContext<CallableDescriptor> contextWithReceiver = createContextWithChainedTrace(
                    variableCandidate, variableCall, candidateTrace, task, ReceiverValue.NO_RECEIVER);

            Call variableCallWithoutReceiver = stripReceiver(variableCall);
            ResolutionCandidate<CallableDescriptor> candidateWithoutReceiver = ResolutionCandidate.create(
                    variableCallWithoutReceiver,
                    candidate.getDescriptor(),
                    candidate.getDispatchReceiver(),
                    ReceiverValue.NO_RECEIVER,
                    ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, null);

            CallCandidateResolutionContext<CallableDescriptor> contextWithoutReceiver = createContextWithChainedTrace(
                    candidateWithoutReceiver, variableCallWithoutReceiver, candidateTrace, task, variableCall.getExplicitReceiver());

            return Lists.newArrayList(contextWithReceiver, contextWithoutReceiver);
        }

        private CallCandidateResolutionContext<CallableDescriptor> createContextWithChainedTrace(
                @NotNull ResolutionCandidate<CallableDescriptor> candidate, @NotNull Call call, @NotNull TemporaryBindingTrace temporaryTrace,
                @NotNull ResolutionTask<CallableDescriptor, FunctionDescriptor> task, @NotNull ReceiverValue receiverValue
        ) {
            ChainedTemporaryBindingTrace chainedTrace = ChainedTemporaryBindingTrace.create(temporaryTrace, "chained trace to resolve candidate", candidate);
            ResolvedCallImpl<CallableDescriptor> resolvedCall = ResolvedCallImpl.create(candidate, chainedTrace, task.tracing, task.dataFlowInfoForArguments);
            return CallCandidateResolutionContext.create(resolvedCall, task, chainedTrace, task.tracing, call, receiverValue);
        }

        private Call stripCallArguments(@NotNull Call call) {
            return new DelegatingCall(call) {
                @Override
                public JetValueArgumentList getValueArgumentList() {
                    return null;
                }

                @NotNull
                @Override
                public List<? extends ValueArgument> getValueArguments() {
                    return Collections.emptyList();
                }

                @NotNull
                @Override
                public List<JetFunctionLiteralArgument> getFunctionLiteralArguments() {
                    return Collections.emptyList();
                }

                @NotNull
                @Override
                public List<JetTypeProjection> getTypeArguments() {
                    return Collections.emptyList();
                }

                @Override
                public JetTypeArgumentList getTypeArgumentList() {
                    return null;
                }

                @NotNull
                @Override
                public JetElement getCallElement() {
                    JetExpression calleeExpression = getCalleeExpression();
                    assert calleeExpression != null : "No callee expression: " + getCallElement().getText();

                    return calleeExpression;
                }
            };
        }

        private Call stripReceiver(@NotNull Call variableCall) {
            return new DelegatingCall(variableCall) {
                @Nullable
                @Override
                public ASTNode getCallOperationNode() {
                    return null;
                }

                @NotNull
                @Override
                public ReceiverValue getExplicitReceiver() {
                    return ReceiverValue.NO_RECEIVER;
                }
            };
        }

        @NotNull
        @Override
        public Collection<MutableResolvedCall<FunctionDescriptor>> transformCall(
                @NotNull CallCandidateResolutionContext<CallableDescriptor> context,
                @NotNull CallResolver callResolver,
                @NotNull ResolutionTask<CallableDescriptor, FunctionDescriptor> task
        ) {
            CallableDescriptor descriptor = context.candidateCall.getCandidateDescriptor();
            if (descriptor instanceof FunctionDescriptor) {
                return super.transformCall(context, callResolver, task);
            }

            assert descriptor instanceof VariableDescriptor;
            JetType returnType = descriptor.getReturnType();
            if (returnType == null) {
                return Collections.emptyList();
            }

            final MutableResolvedCall<VariableDescriptor> variableResolvedCall = (MutableResolvedCall)context.candidateCall;

            JetExpression calleeExpression = task.call.getCalleeExpression();
            if (calleeExpression == null) return Collections.emptyList();

            ExpressionReceiver variableReceiver = new ExpressionReceiver(calleeExpression, variableResolvedCall.getResultingDescriptor().getType());
            Call functionCall = new CallForImplicitInvoke(context.explicitExtensionReceiverForInvoke, variableReceiver, task.call);

            DelegatingBindingTrace variableCallTrace = context.candidateCall.getTrace();
            BasicCallResolutionContext basicCallResolutionContext = BasicCallResolutionContext.create(
                    context.replaceBindingTrace(variableCallTrace).replaceContextDependency(ContextDependency.DEPENDENT),
                    functionCall, context.checkArguments, context.dataFlowInfoForArguments);

            // 'invoke' call resolve
            TracingStrategyForInvoke tracingForInvoke = new TracingStrategyForInvoke(
                    calleeExpression, functionCall, variableReceiver.getType());
            OverloadResolutionResults<FunctionDescriptor> results = callResolver.resolveCallForInvoke(
                    basicCallResolutionContext, tracingForInvoke);
            Collection<MutableResolvedCall<FunctionDescriptor>> calls = ((OverloadResolutionResultsImpl<FunctionDescriptor>)results).getResultingCalls();

            return Collections2.transform(calls, new Function<MutableResolvedCall<FunctionDescriptor>, MutableResolvedCall<FunctionDescriptor>>() {
                @Override
                public MutableResolvedCall<FunctionDescriptor> apply(MutableResolvedCall<FunctionDescriptor> functionResolvedCall) {
                    return new VariableAsFunctionResolvedCallImpl(functionResolvedCall, variableResolvedCall);
                }
            });
        }
    };

    public static class CallForImplicitInvoke extends DelegatingCall {
        private final Call outerCall;
        private final ReceiverValue explicitExtensionReceiver;
        private final ExpressionReceiver calleeExpressionAsDispatchReceiver;
        private final JetSimpleNameExpression fakeInvokeExpression;

        public CallForImplicitInvoke(
                @NotNull ReceiverValue explicitExtensionReceiver,
                @NotNull ExpressionReceiver calleeExpressionAsDispatchReceiver,
                @NotNull Call call
        ) {
            super(call);
            this.outerCall = call;
            this.explicitExtensionReceiver = explicitExtensionReceiver;
            this.calleeExpressionAsDispatchReceiver = calleeExpressionAsDispatchReceiver;
            this.fakeInvokeExpression = (JetSimpleNameExpression) JetPsiFactory(call.getCallElement()).createExpression( "invoke");
        }

        @Nullable
        @Override
        public ASTNode getCallOperationNode() {
            // if an explicit receiver corresponds to the implicit invoke, there is a corresponding call operation node:
            // a.b() or a?.b() (where b has an extension function type);
            // otherwise it's implicit
            return explicitExtensionReceiver.exists() ? super.getCallOperationNode() : null;
        }

        @NotNull
        @Override
        public ReceiverValue getExplicitReceiver() {
            return explicitExtensionReceiver;
        }

        @NotNull
        @Override
        public ExpressionReceiver getDispatchReceiver() {
            return calleeExpressionAsDispatchReceiver;
        }

        @Override
        public JetExpression getCalleeExpression() {
            return fakeInvokeExpression;
        }

        @NotNull
        @Override
        public CallType getCallType() {
            return CallType.INVOKE;
        }

        @NotNull
        public Call getOuterCall() {
            return outerCall;
        }
    }
}
