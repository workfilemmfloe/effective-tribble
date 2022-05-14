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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.scopes.WritableScope;
import org.jetbrains.kotlin.types.DeferredType;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryPackage;
import org.jetbrains.kotlin.util.ReenteringLazyValueComputationException;
import org.jetbrains.kotlin.utils.KotlinFrontEndException;

import static org.jetbrains.kotlin.diagnostics.Errors.TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM;
import static org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilPackage.recordScopeAndDataFlowInfo;

public class ExpressionTypingVisitorDispatcher extends JetVisitor<JetTypeInfo, ExpressionTypingContext> implements ExpressionTypingInternals {

    private static final Logger LOG = Logger.getInstance(ExpressionTypingVisitor.class);

    @NotNull
    public static ExpressionTypingFacade create(@NotNull ExpressionTypingComponents components) {
        return new ExpressionTypingVisitorDispatcher(components, null);
    }

    @NotNull
    public static ExpressionTypingInternals createForBlock(
            @NotNull ExpressionTypingComponents components,
            @NotNull WritableScope writableScope
    ) {
        return new ExpressionTypingVisitorDispatcher(components, writableScope);
    }

    private final ExpressionTypingComponents components;
    private final BasicExpressionTypingVisitor basic;
    private final ExpressionTypingVisitorForStatements statements;
    private final FunctionsTypingVisitor functions;
    private final ControlStructureTypingVisitor controlStructures;
    private final PatternMatchingTypingVisitor patterns;

    private ExpressionTypingVisitorDispatcher(@NotNull ExpressionTypingComponents components, WritableScope writableScope) {
        this.components = components;
        basic = new BasicExpressionTypingVisitor(this);
        controlStructures = new ControlStructureTypingVisitor(this);
        patterns = new PatternMatchingTypingVisitor(this);
        functions = new FunctionsTypingVisitor(this);
        if (writableScope != null) {
            this.statements = new ExpressionTypingVisitorForStatements(this, writableScope, basic, controlStructures, patterns, functions);
        }
        else {
            this.statements = null;
        }
    }

    @Override
    @NotNull
    public ExpressionTypingComponents getComponents() {
        return components;
    }

    @NotNull
    @Override
    public JetTypeInfo checkInExpression(
            @NotNull JetElement callElement,
            @NotNull JetSimpleNameExpression operationSign,
            @NotNull ValueArgument leftArgument,
            @Nullable JetExpression right,
            @NotNull ExpressionTypingContext context
    ) {
        return basic.checkInExpression(callElement, operationSign, leftArgument, right, context);
    }

    @Override
    @NotNull
    public final JetTypeInfo safeGetTypeInfo(@NotNull JetExpression expression, ExpressionTypingContext context) {
        JetTypeInfo typeInfo = getTypeInfo(expression, context);
        if (typeInfo.getType() != null) {
            return typeInfo;
        }
        return typeInfo.replaceType(ErrorUtils.createErrorType("Type for " + expression.getText())).replaceDataFlowInfo(
                context.dataFlowInfo);
    }

    @Override
    @NotNull
    public final JetTypeInfo getTypeInfo(@NotNull JetExpression expression, ExpressionTypingContext context) {
        return getTypeInfo(expression, context, this);
    }

    @Override
    @NotNull
    public final JetTypeInfo getTypeInfo(@NotNull JetExpression expression, ExpressionTypingContext context, boolean isStatement) {
        if (!isStatement) return getTypeInfo(expression, context);
        if (statements != null) {
            return getTypeInfo(expression, context, statements);
        }
        return getTypeInfo(expression, context, createStatementVisitor(context));
    }
    
    private ExpressionTypingVisitorForStatements createStatementVisitor(ExpressionTypingContext context) {
        return new ExpressionTypingVisitorForStatements(this,
                                                        ExpressionTypingUtils.newWritableScopeImpl(context, "statement scope"),
                                                        basic, controlStructures, patterns, functions);
    }

    @Override
    public void checkStatementType(@NotNull JetExpression expression, ExpressionTypingContext context) {
        expression.accept(createStatementVisitor(context), context);
    }

    @NotNull
    static private JetTypeInfo getTypeInfo(@NotNull JetExpression expression, ExpressionTypingContext context, JetVisitor<JetTypeInfo, ExpressionTypingContext> visitor) {
        try {
            JetTypeInfo recordedTypeInfo = BindingContextUtils.getRecordedTypeInfo(expression, context.trace.getBindingContext());
            if (recordedTypeInfo != null) {
                return recordedTypeInfo;
            }
            JetTypeInfo result;
            try {
                result = expression.accept(visitor, context);
                // Some recursive definitions (object expressions) must put their types in the cache manually:
                if (Boolean.TRUE.equals(context.trace.get(BindingContext.PROCESSED, expression))) {
                    JetType type = context.trace.getBindingContext().getType(expression);
                    return result.replaceType(type);
                }

                if (result.getType() instanceof DeferredType) {
                    result = result.replaceType(((DeferredType) result.getType()).getDelegate());
                }
                context.trace.record(BindingContext.EXPRESSION_TYPE_INFO, expression, result);
            }
            catch (ReenteringLazyValueComputationException e) {
                context.trace.report(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.on(expression));
                result = TypeInfoFactoryPackage.createTypeInfo(context);
            }

            context.trace.record(BindingContext.PROCESSED, expression);
            recordScopeAndDataFlowInfo(context.replaceDataFlowInfo(result.getDataFlowInfo()), expression);
            return result;
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (KotlinFrontEndException e) {
            throw e;
        }
        catch (Throwable e) {
            context.trace.report(Errors.EXCEPTION_FROM_ANALYZER.on(expression, e));
            logOrThrowException(expression, e);
            return TypeInfoFactoryPackage.createTypeInfo(
                    ErrorUtils.createErrorType(e.getClass().getSimpleName() + " from analyzer"), 
                    context
            );        
        }
    }

    private static void logOrThrowException(@NotNull JetExpression expression, Throwable e) {
        try {
            // This trows AssertionError in CLI and reports the error in the IDE
            LOG.error(
                    "Exception while analyzing expression at " + DiagnosticUtils.atLocation(expression) + ":\n" + expression.getText() + "\n",
                    e
            );
        }
        catch (AssertionError errorFromLogger) {
            // If we ended up here, we are in CLI, and the initial exception needs to be rethrown,
            // simply throwing AssertionError causes its being wrapped over and over again
            throw new KotlinFrontEndException(errorFromLogger.getMessage(), e);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetTypeInfo visitFunctionLiteralExpression(@NotNull JetFunctionLiteralExpression expression, ExpressionTypingContext data) {
        return expression.accept(functions, data);
    }

    @Override
    public JetTypeInfo visitNamedFunction(@NotNull JetNamedFunction function, ExpressionTypingContext data) {
        return function.accept(functions, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetTypeInfo visitThrowExpression(@NotNull JetThrowExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitReturnExpression(@NotNull JetReturnExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitContinueExpression(@NotNull JetContinueExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitIfExpression(@NotNull JetIfExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitTryExpression(@NotNull JetTryExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitForExpression(@NotNull JetForExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitWhileExpression(@NotNull JetWhileExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitDoWhileExpression(@NotNull JetDoWhileExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitBreakExpression(@NotNull JetBreakExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetTypeInfo visitIsExpression(@NotNull JetIsExpression expression, ExpressionTypingContext data) {
        return expression.accept(patterns, data);
    }

    @Override
    public JetTypeInfo visitWhenExpression(@NotNull JetWhenExpression expression, ExpressionTypingContext data) {
        return expression.accept(patterns, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetTypeInfo visitJetElement(@NotNull JetElement element, ExpressionTypingContext data) {
        return element.accept(basic, data);
    }
}
