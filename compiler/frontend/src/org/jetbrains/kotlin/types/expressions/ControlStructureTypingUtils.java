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

package org.jetbrains.kotlin.types.expressions;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.calls.CallResolver;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemStatus;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintsUtil;
import org.jetbrains.kotlin.resolve.calls.inference.InferenceErrorData;
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind;
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionCandidate;
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy;
import org.jetbrains.kotlin.resolve.calls.util.CallMaker;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.Variance;

import java.util.*;

import static org.jetbrains.kotlin.resolve.BindingContext.CALL;
import static org.jetbrains.kotlin.resolve.BindingContext.RESOLVED_CALL;
import static org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.EXPECTED_TYPE_POSITION;

public class ControlStructureTypingUtils {
    private static final Logger LOG = Logger.getInstance(ControlStructureTypingUtils.class);

    private final CallResolver callResolver;
    private final DataFlowAnalyzer dataFlowAnalyzer;
    private final ModuleDescriptor moduleDescriptor;

    public ControlStructureTypingUtils(
            @NotNull CallResolver callResolver,
            @NotNull DataFlowAnalyzer dataFlowAnalyzer,
            @NotNull ModuleDescriptor moduleDescriptor
    ) {
        this.callResolver = callResolver;
        this.dataFlowAnalyzer = dataFlowAnalyzer;
        this.moduleDescriptor = moduleDescriptor;
    }

    /*package*/ ResolvedCall<FunctionDescriptor> resolveSpecialConstructionAsCall(
            @NotNull Call call,
            @NotNull String constructionName,
            @NotNull List<String> argumentNames,
            @NotNull List<Boolean> isArgumentNullable,
            @NotNull ExpressionTypingContext context,
            @Nullable MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        SimpleFunctionDescriptorImpl function = createFunctionDescriptorForSpecialConstruction(
                constructionName.toUpperCase(), argumentNames, isArgumentNullable);
        TracingStrategy tracing = createTracingForSpecialConstruction(call, constructionName, context);
        ResolutionCandidate<CallableDescriptor> resolutionCandidate = ResolutionCandidate.<CallableDescriptor>create(call, function);
        OverloadResolutionResults<FunctionDescriptor> results = callResolver.resolveCallWithKnownCandidate(
                call, tracing, context, resolutionCandidate, dataFlowInfoForArguments);
        assert results.isSingleResult() : "Not single result after resolving one known candidate";
        return results.getResultingCall();
    }

    private SimpleFunctionDescriptorImpl createFunctionDescriptorForSpecialConstruction(
            @NotNull String constructionName,
            @NotNull List<String> argumentNames,
            @NotNull List<Boolean> isArgumentNullable
    ) {
        assert argumentNames.size() == isArgumentNullable.size();

        Name specialFunctionName = Name.identifier("<SPECIAL-FUNCTION-FOR-" + constructionName + "-RESOLVE>");

        SimpleFunctionDescriptorImpl function = SimpleFunctionDescriptorImpl.create(
                moduleDescriptor, Annotations.Companion.getEMPTY(), specialFunctionName, CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE
        );

        TypeParameterDescriptor typeParameter = TypeParameterDescriptorImpl.createWithDefaultBound(
                function, Annotations.Companion.getEMPTY(), false, Variance.INVARIANT,
                Name.identifier("<TYPE-PARAMETER-FOR-" + constructionName + "-RESOLVE>"), 0);

        KotlinType type = typeParameter.getDefaultType();
        KotlinType nullableType = TypeUtils.makeNullable(type);

        List<ValueParameterDescriptor> valueParameters = new ArrayList<ValueParameterDescriptor>(argumentNames.size());
        for (int i = 0; i < argumentNames.size(); i++) {
            KotlinType argumentType = isArgumentNullable.get(i) ? nullableType : type;
            ValueParameterDescriptorImpl valueParameter = new ValueParameterDescriptorImpl(
                    function, null, i, Annotations.Companion.getEMPTY(), Name.identifier(argumentNames.get(i)),
                    argumentType,
                    /* declaresDefaultValue = */ false,
                    /* isCrossinline = */ false,
                    /* isNoinline = */ false,
                    null, SourceElement.NO_SOURCE
            );
            valueParameters.add(valueParameter);
        }
        function.initialize(
                null,
                null,
                Lists.newArrayList(typeParameter),
                valueParameters,
                type,
                Modality.FINAL,
                Visibilities.PUBLIC
        );
        return function;
    }

    /*package*/ static MutableDataFlowInfoForArguments createIndependentDataFlowInfoForArgumentsForCall(
            final Map<ValueArgument, DataFlowInfo> dataFlowInfoForArgumentsMap
    ) {
        return new MutableDataFlowInfoForArguments() {
            private DataFlowInfo initialDataFlowInfo;

            @Override
            public void setInitialDataFlowInfo(@NotNull DataFlowInfo dataFlowInfo) {
                this.initialDataFlowInfo = dataFlowInfo;
            }

            @Override
            public void updateInfo(@NotNull ValueArgument valueArgument, @NotNull DataFlowInfo dataFlowInfo) {
                //todo
            }

            @NotNull
            @Override
            public DataFlowInfo getInfo(@NotNull ValueArgument valueArgument) {
                return dataFlowInfoForArgumentsMap.get(valueArgument);
            }

            @NotNull
            @Override
            public DataFlowInfo getResultInfo() {
                //todo merge and use
                return initialDataFlowInfo;
            }
        };
    }

    public static MutableDataFlowInfoForArguments createDataFlowInfoForArgumentsForIfCall(
            @NotNull Call callForIf,
            @NotNull DataFlowInfo thenInfo,
            @NotNull DataFlowInfo elseInfo
    ) {
        Map<ValueArgument, DataFlowInfo> dataFlowInfoForArgumentsMap = Maps.newHashMap();
        dataFlowInfoForArgumentsMap.put(callForIf.getValueArguments().get(0), thenInfo);
        dataFlowInfoForArgumentsMap.put(callForIf.getValueArguments().get(1), elseInfo);
        return createIndependentDataFlowInfoForArgumentsForCall(dataFlowInfoForArgumentsMap);
    }

    /*package*/ static Call createCallForSpecialConstruction(
            @NotNull final KtExpression expression,
            @NotNull final KtExpression calleeExpression,
            @NotNull List<? extends KtExpression> arguments
    ) {
        final List<ValueArgument> valueArguments = Lists.newArrayList();
        for (KtExpression argument : arguments) {
            valueArguments.add(CallMaker.makeValueArgument(argument));
        }
        return new Call() {
            @Nullable
            @Override
            public ASTNode getCallOperationNode() {
                return expression.getNode();
            }

            @NotNull
            @Override
            public ReceiverValue getExplicitReceiver() {
                return ReceiverValue.NO_RECEIVER;
            }

            @NotNull
            @Override
            public ReceiverValue getDispatchReceiver() {
                return ReceiverValue.NO_RECEIVER;
            }

            @Nullable
            @Override
            public KtExpression getCalleeExpression() {
                return calleeExpression;
            }

            @Nullable
            @Override
            public KtValueArgumentList getValueArgumentList() {
                return null;
            }

            @NotNull
            @Override
            public List<? extends ValueArgument> getValueArguments() {
                return valueArguments;
            }

            @NotNull
            @Override
            public List<FunctionLiteralArgument> getFunctionLiteralArguments() {
                return Collections.emptyList();
            }

            @NotNull
            @Override
            public List<KtTypeProjection> getTypeArguments() {
                return Collections.emptyList();
            }

            @Nullable
            @Override
            public KtTypeArgumentList getTypeArgumentList() {
                return null;
            }

            @NotNull
            @Override
            public KtElement getCallElement() {
                return expression;
            }

            @NotNull
            @Override
            public CallType getCallType() {
                return CallType.DEFAULT;
            }
        };
    }

    @NotNull
    /*package*/ TracingStrategy createTracingForSpecialConstruction(
            final @NotNull Call call,
            @NotNull String constructionName,
            final @NotNull ExpressionTypingContext context
    ) {
        class CheckTypeContext {
            public BindingTrace trace;
            public KotlinType expectedType;

            CheckTypeContext(@NotNull BindingTrace trace, @NotNull KotlinType expectedType) {
                this.trace = trace;
                this.expectedType = expectedType;
            }

            CheckTypeContext makeTypeNullable() {
                if (TypeUtils.noExpectedType(expectedType)) return this;
                return new CheckTypeContext(trace, TypeUtils.makeNullable(expectedType));
            }
        }

        final KtVisitor<Boolean, CheckTypeContext> checkTypeVisitor = new KtVisitor<Boolean, CheckTypeContext>() {

            private boolean checkExpressionType(@NotNull KtExpression expression, CheckTypeContext c) {
                JetTypeInfo typeInfo = BindingContextUtils.getRecordedTypeInfo(expression, c.trace.getBindingContext());
                if (typeInfo == null) return false;

                Ref<Boolean> hasError = Ref.create();
                dataFlowAnalyzer.checkType(
                        typeInfo.getType(),
                        expression,
                        context
                                .replaceExpectedType(c.expectedType)
                                .replaceDataFlowInfo(typeInfo.getDataFlowInfo())
                                .replaceBindingTrace(c.trace),
                        hasError
                );
                return hasError.get();
            }

            private boolean checkExpressionTypeRecursively(@Nullable KtExpression expression, CheckTypeContext c) {
                if (expression == null) return false;
                return expression.accept(this, c);
            }

            private boolean checkSubExpressions(
                    KtExpression firstSub, KtExpression secondSub, KtExpression expression,
                    CheckTypeContext firstContext, CheckTypeContext secondContext, CheckTypeContext context
            ) {
                boolean errorWasReported = checkExpressionTypeRecursively(firstSub, firstContext);
                errorWasReported |= checkExpressionTypeRecursively(secondSub, secondContext);
                return errorWasReported || checkExpressionType(expression, context);
            }

            @Override
            public Boolean visitIfExpression(@NotNull KtIfExpression ifExpression, CheckTypeContext c) {
                KtExpression thenBranch = ifExpression.getThen();
                KtExpression elseBranch = ifExpression.getElse();
                if (thenBranch == null || elseBranch == null) {
                    return checkExpressionType(ifExpression, c);
                }
                return checkSubExpressions(thenBranch, elseBranch, ifExpression, c, c, c);
            }

            @Override
            public Boolean visitBlockExpression(@NotNull KtBlockExpression expression, CheckTypeContext c) {
                if (expression.getStatements().isEmpty()) {
                    return checkExpressionType(expression, c);
                }
                KtExpression lastStatement = KtPsiUtil.getLastStatementInABlock(expression);
                if (lastStatement != null) {
                    return checkExpressionTypeRecursively(lastStatement, c);
                }
                return false;
            }

            @Override
            public Boolean visitPostfixExpression(@NotNull KtPostfixExpression expression, CheckTypeContext c) {
                if (expression.getOperationReference().getReferencedNameElementType() == KtTokens.EXCLEXCL) {
                    return checkExpressionTypeRecursively(expression.getBaseExpression(), c.makeTypeNullable());
                }
                return super.visitPostfixExpression(expression, c);
            }

            @Override
            public Boolean visitBinaryExpression(@NotNull KtBinaryExpression expression, CheckTypeContext c) {
                if (expression.getOperationReference().getReferencedNameElementType() == KtTokens.ELVIS) {

                    return checkSubExpressions(expression.getLeft(), expression.getRight(), expression, c.makeTypeNullable(), c, c);
                }
                return super.visitBinaryExpression(expression, c);
            }

            @Override
            public Boolean visitExpression(@NotNull KtExpression expression, CheckTypeContext c) {
                return checkExpressionType(expression, c);
            }
        };

        return new ThrowingOnErrorTracingStrategy("resolve " + constructionName + " as a call") {
            @Override
            public <D extends CallableDescriptor> void bindReference(
                    @NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall
            ) {
                //do nothing
            }

            @Override
            public void bindCall(@NotNull BindingTrace trace, @NotNull Call call) {
                trace.record(CALL, call.getCalleeExpression(), call);
            }

            @Override
            public <D extends CallableDescriptor> void bindResolvedCall(
                    @NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall
            ) {
                trace.record(RESOLVED_CALL, call, resolvedCall);
            }

            @Override
            public void typeInferenceFailed(
                    @NotNull BindingTrace trace, @NotNull InferenceErrorData data
            ) {
                ConstraintSystem constraintSystem = data.constraintSystem;
                ConstraintSystemStatus status = constraintSystem.getStatus();
                assert !status.isSuccessful() : "Report error only for not successful constraint system";

                if (status.hasErrorInConstrainingTypes() || status.hasUnknownParameters()) {
                    return;
                }
                KtExpression expression = (KtExpression) call.getCallElement();
                if (status.hasOnlyErrorsDerivedFrom(EXPECTED_TYPE_POSITION) || status.hasConflictingConstraints()
                        || status.hasTypeInferenceIncorporationError()) { // todo after KT-... remove this line
                    expression.accept(checkTypeVisitor, new CheckTypeContext(trace, data.expectedType));
                    return;
                }
                KtDeclaration parentDeclaration = PsiTreeUtil.getParentOfType(expression, KtNamedDeclaration.class);
                logError("Expression: " + (parentDeclaration != null ? parentDeclaration.getText() : expression.getText()) +
                         "\nConstraint system status: \n" + ConstraintsUtil.getDebugMessageForStatus(status));
            }
        };
    }
    
    private abstract static class ThrowingOnErrorTracingStrategy implements TracingStrategy {
        private final String debugName;

        protected ThrowingOnErrorTracingStrategy(String debugName) {
            this.debugName = debugName;
        }

        private void logError() {
            logError(null);
        }

        protected void logError(@Nullable String additionalInformation) {
            String errorMessage = "Resolution error of this type shouldn't occur for " + debugName;
            if (additionalInformation != null) {
                errorMessage += ".\n" + additionalInformation;
            }
            LOG.error(errorMessage);
        }

        @Override
        public void unresolvedReference(@NotNull BindingTrace trace) {
            logError();
        }

        @Override
        public <D extends CallableDescriptor> void unresolvedReferenceWrongReceiver(
                @NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates
        ) {
            logError();
        }

        @Override
        public <D extends CallableDescriptor> void recordAmbiguity(
                @NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates
        ) {
            logError();
        }

        @Override
        public void missingReceiver(
                @NotNull BindingTrace trace, @NotNull ReceiverParameterDescriptor expectedReceiver
        ) {
            logError();
        }

        @Override
        public void wrongReceiverType(
                @NotNull BindingTrace trace, @NotNull ReceiverParameterDescriptor receiverParameter, @NotNull ReceiverValue receiverArgument
        ) {
            logError();
        }

        @Override
        public void noReceiverAllowed(@NotNull BindingTrace trace) {
            logError();
        }

        @Override
        public void noValueForParameter(
                @NotNull BindingTrace trace, @NotNull ValueParameterDescriptor valueParameter
        ) {
            logError();
        }

        @Override
        public void wrongNumberOfTypeArguments(@NotNull BindingTrace trace, int expectedTypeArgumentCount) {
            logError();
        }

        @Override
        public <D extends CallableDescriptor> void ambiguity(
                @NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> descriptors
        ) {
            logError();
        }

        @Override
        public <D extends CallableDescriptor> void noneApplicable(
                @NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> descriptors
        ) {
            logError();
        }

        @Override
        public <D extends CallableDescriptor> void cannotCompleteResolve(
                @NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> descriptors
        ) {
            logError();
        }

        @Override
        public void instantiationOfAbstractClass(@NotNull BindingTrace trace) {
            logError();
        }

        @Override
        public void abstractSuperCall(@NotNull BindingTrace trace) {
            logError();
        }

        @Override
        public void nestedClassAccessViaInstanceReference(
                @NotNull BindingTrace trace, @NotNull ClassDescriptor classDescriptor,
                @NotNull ExplicitReceiverKind explicitReceiverKind
        ) {
            logError();
        }

        @Override
        public void unsafeCall(
                @NotNull BindingTrace trace, @NotNull KotlinType type, boolean isCallForImplicitInvoke
        ) {
            logError();
        }

        @Override
        public void unnecessarySafeCall(
                @NotNull BindingTrace trace, @NotNull KotlinType type
        ) {
            logError();
        }

        @Override
        public void invisibleMember(
                @NotNull BindingTrace trace, @NotNull DeclarationDescriptorWithVisibility descriptor
        ) {
            logError();
        }

        @Override
        public void typeInferenceFailed(
                @NotNull BindingTrace trace, @NotNull InferenceErrorData inferenceErrorData
        ) {
            logError();
        }

        @Override
        public void nonExtensionFunctionCalledAsExtension(@NotNull BindingTrace trace) {
            logError();
        }
    }
}
