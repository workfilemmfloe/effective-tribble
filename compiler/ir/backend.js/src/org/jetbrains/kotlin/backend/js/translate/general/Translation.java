/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.js.translate.general;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.js.backend.ast.*;
import org.jetbrains.kotlin.js.backend.ast.metadata.MetadataProperties;
import org.jetbrains.kotlin.js.facade.exceptions.TranslationRuntimeException;
import org.jetbrains.kotlin.backend.js.translate.context.TemporaryVariable;
import org.jetbrains.kotlin.backend.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.backend.js.translate.expression.ExpressionVisitor;
import org.jetbrains.kotlin.backend.js.translate.expression.PatternTranslator;
import org.jetbrains.kotlin.backend.js.translate.utils.JsAstUtils;
import org.jetbrains.kotlin.backend.js.translate.utils.TranslationUtils;
import org.jetbrains.kotlin.backend.js.translate.utils.mutator.AssignToExpressionMutator;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtUnaryExpression;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilsKt;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.constants.NullValue;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;

import static org.jetbrains.kotlin.backend.js.translate.utils.JsAstUtils.convertToStatement;
import static org.jetbrains.kotlin.backend.js.translate.utils.mutator.LastExpressionMutator.mutateLastExpression;

/**
 * This class provides a interface which all translators use to interact with each other.
 * Goal is to simplify interaction between translators.
 */
public final class Translation {
    private Translation() {
    }

    @NotNull
    public static PatternTranslator patternTranslator(@NotNull TranslationContext context) {
        return PatternTranslator.newInstance(context);
    }

    @NotNull
    public static JsNode translateExpression(@NotNull KtExpression expression, @NotNull TranslationContext context) {
        return translateExpression(expression, context, context.dynamicContext().jsBlock());
    }

    @NotNull
    public static JsNode translateExpression(@NotNull KtExpression expression, @NotNull TranslationContext context, @NotNull JsBlock block) {
        JsExpression aliasForExpression = context.aliasingContext().getAliasForExpression(expression);
        if (aliasForExpression != null) {
            return aliasForExpression;
        }

        CompileTimeConstant<?> compileTimeValue = ConstantExpressionEvaluator.getConstant(expression, context.bindingContext());
        if (compileTimeValue != null) {
            KotlinType type = context.bindingContext().getType(expression);
            if (type != null) {
                if (KotlinBuiltIns.isLong(type) || (KotlinBuiltIns.isInt(type) && expression instanceof KtUnaryExpression)) {
                    JsExpression constantResult = translateConstant(compileTimeValue, expression, context);
                    if (constantResult != null) return constantResult.source(expression);
                }
            }
        }

        TranslationContext innerContext = context.innerBlock();
        JsNode result = doTranslateExpression(expression, innerContext);
        context.moveVarsFrom(innerContext);
        block.getStatements().addAll(innerContext.dynamicContext().jsBlock().getStatements());

        return result;
    }

    @Nullable
    public static JsExpression translateConstant(
            @NotNull CompileTimeConstant compileTimeValue,
            @NotNull KtExpression expression,
            @NotNull TranslationContext context
    ) {
        KotlinType expectedType = context.bindingContext().getType(expression);
        ConstantValue<?> constant = compileTimeValue.toConstantValue(expectedType != null ? expectedType : TypeUtils.NO_EXPECTED_TYPE);
        JsExpression result = translateConstantWithoutType(constant);
        if (result != null) {
            MetadataProperties.setType(result, expectedType);
        }
        return result;
    }

    @Nullable
    private static JsExpression translateConstantWithoutType(@NotNull ConstantValue<?> constant) {
        if (constant instanceof NullValue) {
            return new JsNullLiteral();
        }
        Object value = constant.getValue();
        if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return new JsIntLiteral(((Number) value).intValue());
        }
        else if (value instanceof Long) {
            return JsAstUtils.newLong((Long) value);
        }
        else if (value instanceof Float) {
            float floatValue = (Float) value;
            double doubleValue;
            if (Float.isInfinite(floatValue) || Float.isNaN(floatValue)) {
                doubleValue = floatValue;
            }
            else {
                doubleValue = Double.parseDouble(Float.toString(floatValue));
            }
            return new JsDoubleLiteral(doubleValue);
        }
        else if (value instanceof Number) {
            return new JsDoubleLiteral(((Number) value).doubleValue());
        }
        else if (value instanceof Boolean) {
            return new JsBooleanLiteral((Boolean) value);
        }

        //TODO: test
        if (value instanceof String) {
            return new JsStringLiteral((String) value);
        }
        if (value instanceof Character) {
            return new JsIntLiteral(((Character) value).charValue());
        }

        return null;
    }

    @NotNull
    private static JsNode doTranslateExpression(KtExpression expression, TranslationContext context) {
        try {
            return expression.accept(new ExpressionVisitor(), context);
        }
        catch (TranslationRuntimeException e) {
            throw e;
        }
        catch (RuntimeException | AssertionError e) {
            throw new TranslationRuntimeException(expression, e);
        }
    }

    @NotNull
    public static JsExpression translateAsExpression(@NotNull KtExpression expression, @NotNull TranslationContext context) {
        return translateAsExpression(expression, context, context.dynamicContext().jsBlock());
    }

    @NotNull
    public static JsExpression translateAsExpression(
            @NotNull KtExpression expression,
            @NotNull TranslationContext context,
            @NotNull JsBlock block
    ) {
        JsNode jsNode = translateExpression(expression, context, block);

        if (jsNode instanceof JsExpression) {
            JsExpression jsExpression = (JsExpression) jsNode;
            KotlinType type = context.bindingContext().getType(expression);
            if (MetadataProperties.getType(jsExpression) == null) {
                MetadataProperties.setType(jsExpression, type);
            }
            else if (type != null) {
                jsExpression = TranslationUtils.coerce(context, jsExpression, type);
            }
            return jsExpression;
        }

        assert jsNode instanceof JsStatement : "Unexpected node of type: " + jsNode.getClass().toString();
        if (BindingContextUtilsKt.isUsedAsExpression(expression, context.bindingContext())) {
            TemporaryVariable result = context.declareTemporary(null, expression);
            AssignToExpressionMutator saveResultToTemporaryMutator = new AssignToExpressionMutator(result.reference());
            block.getStatements().add(mutateLastExpression(jsNode, saveResultToTemporaryMutator));
            JsExpression tmpVar = result.reference();
            MetadataProperties.setType(tmpVar, context.bindingContext().getType(expression));
            return tmpVar;
        }

        block.getStatements().add(convertToStatement(jsNode));
        return new JsNullLiteral().source(expression);
    }

    @NotNull
    public static JsStatement translateAsStatement(@NotNull KtExpression expression, @NotNull TranslationContext context) {
        return translateAsStatement(expression, context, context.dynamicContext().jsBlock());
    }

    @NotNull
    public static JsStatement translateAsStatement(
            @NotNull KtExpression expression,
            @NotNull TranslationContext context,
            @NotNull JsBlock block) {
        return convertToStatement(translateExpression(expression, context, block));
    }

    @NotNull
    public static JsStatement translateAsStatementAndMergeInBlockIfNeeded(
            @NotNull KtExpression expression,
            @NotNull TranslationContext context
    ) {
        JsBlock block = new JsBlock();
        JsNode node = translateExpression(expression, context, block);
        return JsAstUtils.mergeStatementInBlockIfNeeded(convertToStatement(node), block);
    }

}
