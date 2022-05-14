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

import com.google.common.collect.Sets;
import com.intellij.openapi.util.Ref;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.PossiblyBareType;
import org.jetbrains.kotlin.resolve.TypeResolutionContext;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory;
import org.jetbrains.kotlin.resolve.calls.util.CallMaker;
import org.jetbrains.kotlin.resolve.scopes.WritableScope;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.util.Collections;
import java.util.Set;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;
import static org.jetbrains.kotlin.types.TypeUtils.isIntersectionEmpty;
import static org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.newWritableScopeImpl;

public class PatternMatchingTypingVisitor extends ExpressionTypingVisitor {
    protected PatternMatchingTypingVisitor(@NotNull ExpressionTypingInternals facade) {
        super(facade);
    }

    @Override
    public JetTypeInfo visitIsExpression(@NotNull JetIsExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT);
        JetExpression leftHandSide = expression.getLeftHandSide();
        JetTypeInfo typeInfo = facade.safeGetTypeInfo(leftHandSide, context.replaceScope(context.scope));
        JetType knownType = typeInfo.getType();
        DataFlowInfo dataFlowInfo = typeInfo.getDataFlowInfo();
        if (expression.getTypeReference() != null) {
            DataFlowValue dataFlowValue = DataFlowValueFactory.createDataFlowValue(leftHandSide, knownType,
                                                                                   context.trace.getBindingContext());
            DataFlowInfo conditionInfo = checkTypeForIs(context, knownType, expression.getTypeReference(), dataFlowValue).thenInfo;
            DataFlowInfo newDataFlowInfo = conditionInfo.and(dataFlowInfo);
            context.trace.record(BindingContext.DATAFLOW_INFO_AFTER_CONDITION, expression, newDataFlowInfo);
        }
        return DataFlowUtils.checkType(KotlinBuiltIns.getInstance().getBooleanType(), expression, contextWithExpectedType, dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitWhenExpression(@NotNull JetWhenExpression expression, ExpressionTypingContext context) {
        return visitWhenExpression(expression, context, false);
    }

    public JetTypeInfo visitWhenExpression(JetWhenExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        DataFlowUtils.recordExpectedType(contextWithExpectedType.trace, expression, contextWithExpectedType.expectedType);

        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT);
        // TODO :change scope according to the bound value in the when header
        JetExpression subjectExpression = expression.getSubjectExpression();

        JetType subjectType;
        if (subjectExpression == null) {
            subjectType = ErrorUtils.createErrorType("Unknown type");
        }
        else {
            JetTypeInfo typeInfo = facade.safeGetTypeInfo(subjectExpression, context);
            subjectType = typeInfo.getType();
            context = context.replaceDataFlowInfo(typeInfo.getDataFlowInfo());
        }
        DataFlowValue subjectDataFlowValue = subjectExpression != null
                ? DataFlowValueFactory.createDataFlowValue(subjectExpression, subjectType, context.trace.getBindingContext())
                : DataFlowValue.NULL;

        // TODO : exhaustive patterns

        Set<JetType> expressionTypes = Sets.newHashSet();
        DataFlowInfo commonDataFlowInfo = null;
        DataFlowInfo elseDataFlowInfo = context.dataFlowInfo;
        for (JetWhenEntry whenEntry : expression.getEntries()) {
            DataFlowInfos infosForCondition = getDataFlowInfosForEntryCondition(
                    whenEntry, context.replaceDataFlowInfo(elseDataFlowInfo), subjectExpression, subjectType, subjectDataFlowValue);
            elseDataFlowInfo = elseDataFlowInfo.and(infosForCondition.elseInfo);

            JetExpression bodyExpression = whenEntry.getExpression();
            if (bodyExpression != null) {
                WritableScope scopeToExtend = newWritableScopeImpl(context, "Scope extended in when entry");
                ExpressionTypingContext newContext = contextWithExpectedType
                        .replaceScope(scopeToExtend).replaceDataFlowInfo(infosForCondition.thenInfo).replaceContextDependency(INDEPENDENT);
                CoercionStrategy coercionStrategy = isStatement ? CoercionStrategy.COERCION_TO_UNIT : CoercionStrategy.NO_COERCION;
                JetTypeInfo typeInfo = components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(
                        scopeToExtend, Collections.singletonList(bodyExpression), coercionStrategy, newContext, context.trace);
                JetType type = typeInfo.getType();
                if (type != null) {
                    expressionTypes.add(type);
                }
                if (commonDataFlowInfo == null) {
                    commonDataFlowInfo = typeInfo.getDataFlowInfo();
                }
                else {
                    commonDataFlowInfo = commonDataFlowInfo.or(typeInfo.getDataFlowInfo());
                }
            }
        }

        if (commonDataFlowInfo == null) {
            commonDataFlowInfo = context.dataFlowInfo;
        }

        if (!expressionTypes.isEmpty()) {
            return DataFlowUtils.checkImplicitCast(CommonSupertypes.commonSupertype(expressionTypes), expression, contextWithExpectedType, isStatement, commonDataFlowInfo);
        }
        return JetTypeInfo.create(null, commonDataFlowInfo);
    }

    @NotNull
    private DataFlowInfos getDataFlowInfosForEntryCondition(
            @NotNull JetWhenEntry whenEntry,
            @NotNull ExpressionTypingContext context,
            @Nullable JetExpression subjectExpression,
            @NotNull JetType subjectType,
            @NotNull DataFlowValue subjectDataFlowValue
    ) {
        if (whenEntry.isElse()) {
            return new DataFlowInfos(context.dataFlowInfo);
        }

        DataFlowInfos infos = null;
        for (JetWhenCondition condition : whenEntry.getConditions()) {
            DataFlowInfos conditionInfos = checkWhenCondition(subjectExpression, subjectType, condition,
                                                              context, subjectDataFlowValue);
            if (infos != null) {
                infos = new DataFlowInfos(infos.thenInfo.or(conditionInfos.thenInfo), infos.elseInfo.and(conditionInfos.elseInfo));
            }
            else {
                infos = conditionInfos;
            }
        }
        return infos != null ? infos : new DataFlowInfos(context.dataFlowInfo);
    }

    private DataFlowInfos checkWhenCondition(
            @Nullable final JetExpression subjectExpression,
            final JetType subjectType,
            JetWhenCondition condition,
            final ExpressionTypingContext context,
            final DataFlowValue subjectDataFlowValue
    ) {
        final Ref<DataFlowInfos> newDataFlowInfo = new Ref<DataFlowInfos>(noChange(context));
        condition.accept(new JetVisitorVoid() {
            @Override
            public void visitWhenConditionInRange(@NotNull JetWhenConditionInRange condition) {
                JetExpression rangeExpression = condition.getRangeExpression();
                if (rangeExpression == null) return;
                if (subjectExpression == null) {
                    context.trace.report(EXPECTED_CONDITION.on(condition));
                    DataFlowInfo dataFlowInfo = facade.getTypeInfo(rangeExpression, context).getDataFlowInfo();
                    newDataFlowInfo.set(new DataFlowInfos(dataFlowInfo, dataFlowInfo));
                    return;
                }
                ValueArgument argumentForSubject = CallMaker.makeExternalValueArgument(subjectExpression);
                JetTypeInfo typeInfo = facade.checkInExpression(condition, condition.getOperationReference(),
                                                                argumentForSubject, rangeExpression, context);
                DataFlowInfo dataFlowInfo = typeInfo.getDataFlowInfo();
                newDataFlowInfo.set(new DataFlowInfos(dataFlowInfo, dataFlowInfo));
                if (!KotlinBuiltIns.getInstance().getBooleanType().equals(typeInfo.getType())) {
                    context.trace.report(TYPE_MISMATCH_IN_RANGE.on(condition));
                }
            }

            @Override
            public void visitWhenConditionIsPattern(@NotNull JetWhenConditionIsPattern condition) {
                if (subjectExpression == null) {
                    context.trace.report(EXPECTED_CONDITION.on(condition));
                }
                if (condition.getTypeReference() != null) {
                    DataFlowInfos result = checkTypeForIs(context, subjectType, condition.getTypeReference(), subjectDataFlowValue);
                    if (condition.isNegated()) {
                        newDataFlowInfo.set(new DataFlowInfos(result.elseInfo, result.thenInfo));
                    }
                    else {
                        newDataFlowInfo.set(result);
                    }
                }
            }

            @Override
            public void visitWhenConditionWithExpression(@NotNull JetWhenConditionWithExpression condition) {
                JetExpression expression = condition.getExpression();
                if (expression != null) {
                    newDataFlowInfo.set(checkTypeForExpressionCondition(context, expression, subjectType, subjectExpression == null,
                                                                        subjectDataFlowValue));
                }
            }

            @Override
            public void visitJetElement(@NotNull JetElement element) {
                context.trace.report(UNSUPPORTED.on(element, getClass().getCanonicalName()));
            }
        });
        return newDataFlowInfo.get();
    }

    private static class DataFlowInfos {
        private final DataFlowInfo thenInfo;
        private final DataFlowInfo elseInfo;

        private DataFlowInfos(DataFlowInfo thenInfo, DataFlowInfo elseInfo) {
            this.thenInfo = thenInfo;
            this.elseInfo = elseInfo;
        }

        private DataFlowInfos(DataFlowInfo info) {
            this(info, info);
        }
    }

    private DataFlowInfos checkTypeForExpressionCondition(
            ExpressionTypingContext context,
            JetExpression expression,
            JetType subjectType,
            boolean conditionExpected,
            DataFlowValue subjectDataFlowValue
    ) {
        if (expression == null) {
            return noChange(context);
        }
        JetTypeInfo typeInfo = facade.getTypeInfo(expression, context);
        JetType type = typeInfo.getType();
        if (type == null) {
            return noChange(context);
        }
        context = context.replaceDataFlowInfo(typeInfo.getDataFlowInfo());
        if (conditionExpected) {
            JetType booleanType = KotlinBuiltIns.getInstance().getBooleanType();
            if (!JetTypeChecker.DEFAULT.equalTypes(booleanType, type)) {
                context.trace.report(TYPE_MISMATCH_IN_CONDITION.on(expression, type));
            }
            else {
                DataFlowInfo ifInfo = DataFlowUtils.extractDataFlowInfoFromCondition(expression, true, context);
                DataFlowInfo elseInfo = DataFlowUtils.extractDataFlowInfoFromCondition(expression, false, context);
                return new DataFlowInfos(ifInfo, elseInfo);
            }
            return noChange(context);
        }
        checkTypeCompatibility(context, type, subjectType, expression);
        DataFlowValue expressionDataFlowValue =
                DataFlowValueFactory.createDataFlowValue(expression, type, context.trace.getBindingContext());
        DataFlowInfos result = noChange(context);
        result = new DataFlowInfos(
                result.thenInfo.equate(subjectDataFlowValue, expressionDataFlowValue),
                result.elseInfo.disequate(subjectDataFlowValue, expressionDataFlowValue)
        );
        return result;
    }

    private DataFlowInfos checkTypeForIs(
            ExpressionTypingContext context,
            JetType subjectType,
            JetTypeReference typeReferenceAfterIs,
            DataFlowValue subjectDataFlowValue
    ) {
        if (typeReferenceAfterIs == null) {
            return noChange(context);
        }
        TypeResolutionContext typeResolutionContext = new TypeResolutionContext(context.scope, context.trace, true, /*allowBareTypes=*/ true);
        PossiblyBareType possiblyBareTarget = components.expressionTypingServices.getTypeResolver().resolvePossiblyBareType(typeResolutionContext, typeReferenceAfterIs);
        JetType targetType = TypeReconstructionUtil.reconstructBareType(typeReferenceAfterIs, possiblyBareTarget, subjectType, context.trace);

        if (TypesPackage.isDynamic(targetType)) {
            context.trace.report(DYNAMIC_NOT_ALLOWED.on(typeReferenceAfterIs));
        }

        if (!subjectType.isMarkedNullable() && targetType.isMarkedNullable()) {
            JetTypeElement element = typeReferenceAfterIs.getTypeElement();
            assert element instanceof JetNullableType : "element must be instance of " + JetNullableType.class.getName();
            JetNullableType nullableType = (JetNullableType) element;
            context.trace.report(Errors.USELESS_NULLABLE_CHECK.on(nullableType));
        }
        checkTypeCompatibility(context, targetType, subjectType, typeReferenceAfterIs);
        if (CastDiagnosticsUtil.isCastErased(subjectType, targetType, JetTypeChecker.DEFAULT)) {
            context.trace.report(Errors.CANNOT_CHECK_FOR_ERASED.on(typeReferenceAfterIs, targetType));
        }
        return new DataFlowInfos(context.dataFlowInfo.establishSubtyping(subjectDataFlowValue, targetType), context.dataFlowInfo);
    }

    private static DataFlowInfos noChange(ExpressionTypingContext context) {
        return new DataFlowInfos(context.dataFlowInfo, context.dataFlowInfo);
    }

    /*
     * (a: SubjectType) is Type
     */
    private static void checkTypeCompatibility(
            @NotNull ExpressionTypingContext context,
            @Nullable JetType type,
            @NotNull JetType subjectType,
            @NotNull JetElement reportErrorOn
    ) {
        // TODO : Take smart casts into account?
        if (type == null) {
            return;
        }
        if (isIntersectionEmpty(type, subjectType)) {
            context.trace.report(INCOMPATIBLE_TYPES.on(reportErrorOn, type, subjectType));
            return;
        }

        // check if the pattern is essentially a 'null' expression
        if (KotlinBuiltIns.isNullableNothing(type) && !TypeUtils.isNullableType(subjectType)) {
            context.trace.report(SENSELESS_NULL_IN_WHEN.on(reportErrorOn));
        }
    }
}
