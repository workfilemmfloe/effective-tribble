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

package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.bindingContextUtil.BindingContextUtilPackage;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.NullValue;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.declaration.ClassTranslator;
import org.jetbrains.k2js.translate.expression.loopTranslator.LoopTranslatorPackage;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.general.TranslatorVisitor;
import org.jetbrains.k2js.translate.operation.BinaryOperationTranslator;
import org.jetbrains.k2js.translate.operation.UnaryOperationTranslator;
import org.jetbrains.k2js.translate.reference.*;
import org.jetbrains.k2js.translate.utils.JsAstUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.List;

import static org.jetbrains.jet.lang.resolve.BindingContextUtils.isVarCapturedInClosure;
import static org.jetbrains.k2js.translate.context.Namer.getCapturedVarAccessor;
import static org.jetbrains.k2js.translate.general.Translation.translateAsExpression;
import static org.jetbrains.k2js.translate.reference.ReferenceTranslator.translateAsFQReference;
import static org.jetbrains.k2js.translate.utils.BindingUtils.*;
import static org.jetbrains.k2js.translate.utils.ErrorReportingUtils.message;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.convertToStatement;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.newVar;
import static org.jetbrains.k2js.translate.utils.JsDescriptorUtils.getReceiverParameterForDeclaration;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.translateInitializerForProperty;

public final class ExpressionVisitor extends TranslatorVisitor<JsNode> {
    @Override
    @NotNull
    public JsNode visitConstantExpression(@NotNull JetConstantExpression expression, @NotNull TranslationContext context) {
        return translateConstantExpression(expression, context).source(expression);
    }

    @NotNull
    private static JsNode translateConstantExpression(@NotNull JetConstantExpression expression, @NotNull TranslationContext context) {
        CompileTimeConstant<?> compileTimeValue = context.bindingContext().get(BindingContext.COMPILE_TIME_VALUE, expression);

        assert compileTimeValue != null : message(expression, "Expression is not compile time value: " + expression.getText() + " ");

        if (compileTimeValue instanceof NullValue) {
            return JsLiteral.NULL;
        }

        Object value = getCompileTimeValue(context.bindingContext(), expression, compileTimeValue);
        if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return context.program().getNumberLiteral(((Number) value).intValue());
        }
        else if (value instanceof Number) {
            return context.program().getNumberLiteral(((Number) value).doubleValue());
        }
        else if (value instanceof Boolean) {
            return JsLiteral.getBoolean((Boolean) value);
        }

        //TODO: test
        if (value instanceof String) {
            return context.program().getStringLiteral((String) value);
        }
        if (value instanceof Character) {
            return context.program().getStringLiteral(value.toString());
        }

        throw new AssertionError(message(expression, "Unsupported constant expression: " + expression.getText() + " "));
    }

    @Override
    @NotNull
    public JsNode visitBlockExpression(@NotNull JetBlockExpression jetBlock, @NotNull TranslationContext context) {
        List<JetElement> statements = jetBlock.getStatements();
        JsBlock jsBlock = new JsBlock();
        for (JetElement statement : statements) {
            assert statement instanceof JetExpression : "Elements in JetBlockExpression " +
                                                        "should be of type JetExpression";
            JsNode jsNode = Translation.translateExpression((JetExpression)statement, context, jsBlock);
            JsStatement jsStatement = convertToStatement(jsNode);
            if (!JsAstUtils.isEmptyStatement(jsStatement)) {
                jsBlock.getStatements().add(jsStatement);
            }
        }
        return jsBlock;
    }

    @Override
    public JsNode visitMultiDeclaration(@NotNull JetMultiDeclaration multiDeclaration, @NotNull TranslationContext context) {
        JetExpression jetInitializer = multiDeclaration.getInitializer();
        assert jetInitializer != null : "Initializer for multi declaration must be not null";
        JsExpression initializer = Translation.translateAsExpression(jetInitializer, context);
        return MultiDeclarationTranslator.translate(multiDeclaration, context.scope().declareTemporary(), initializer, context);
    }

    @Override
    @NotNull
    public JsNode visitReturnExpression(@NotNull JetReturnExpression jetReturnExpression,
            @NotNull TranslationContext context) {
        JetExpression returned = jetReturnExpression.getReturnedExpression();
        if (returned == null) {
            return new JsReturn(null).source(jetReturnExpression);
        }
        JsExpression jsReturnExpression = translateAsExpression(returned, context);
        if (JsAstUtils.isEmptyExpression(jsReturnExpression)) {
            return context.getEmptyExpression();
        }
        return new JsReturn(jsReturnExpression).source(jetReturnExpression);
    }

    @Override
    @NotNull
    public JsNode visitParenthesizedExpression(@NotNull JetParenthesizedExpression expression,
            @NotNull TranslationContext context) {
        JetExpression expressionInside = expression.getExpression();
        if (expressionInside != null) {
            return Translation.translateExpression(expressionInside, context);
        }
        return context.getEmptyStatement();
    }

    @Override
    @NotNull
    public JsNode visitBinaryExpression(@NotNull JetBinaryExpression expression,
            @NotNull TranslationContext context) {
        return BinaryOperationTranslator.translate(expression, context);
    }

    @Override
    @NotNull
    // assume it is a local variable declaration
    public JsNode visitProperty(@NotNull JetProperty expression, @NotNull TranslationContext context) {
        VariableDescriptor descriptor = BindingContextUtils.getNotNull(context.bindingContext(), BindingContext.VARIABLE, expression);
        JsExpression initializer = translateInitializerForProperty(expression, context);
        if (initializer != null && JsAstUtils.isEmptyExpression(initializer)) {
            return context.getEmptyExpression();
        }

        JsName name = context.getNameForDescriptor(descriptor);
        if (isVarCapturedInClosure(context.bindingContext(), descriptor)) {
            JsNameRef alias = getCapturedVarAccessor(name.makeRef());
            initializer = JsAstUtils.wrapValue(alias, initializer == null ? JsLiteral.NULL : initializer);
        }

        return newVar(name, initializer).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitCallableReferenceExpression(@NotNull JetCallableReferenceExpression expression, @NotNull TranslationContext context) {
        return CallableReferenceTranslator.INSTANCE$.translate(expression, context);
    }

    @Override
    @NotNull
    public JsNode visitCallExpression(@NotNull JetCallExpression expression,
            @NotNull TranslationContext context) {
        if (InlinedCallExpressionTranslator.shouldBeInlined(expression, context) &&
            BindingContextUtilPackage.isUsedAsExpression(expression, context.bindingContext())) {
            TemporaryVariable temporaryVariable = context.declareTemporary(null);
            JsExpression callResult = CallExpressionTranslator.translate(expression, null, context).source(expression);
            context.addStatementToCurrentBlock(JsAstUtils.assignment(temporaryVariable.reference(), callResult).makeStmt());
            return temporaryVariable.reference();
        } else {
            return CallExpressionTranslator.translate(expression, null, context).source(expression);
        }
    }

    @Override
    @NotNull
    public JsNode visitIfExpression(@NotNull JetIfExpression expression, @NotNull TranslationContext context) {
        assert expression.getCondition() != null : "condition should not ne null: " + expression.getText();
        JsExpression testExpression = Translation.translateAsExpression(expression.getCondition(), context);
        if (JsAstUtils.isEmptyExpression(testExpression)) {
            return testExpression;
        }

        boolean isKotlinExpression = BindingContextUtilPackage.isUsedAsExpression(expression, context.bindingContext());

        JetExpression thenExpression = expression.getThen();
        assert thenExpression != null : "then expression should not be null: " + expression.getText();
        JetExpression elseExpression = expression.getElse();

        JsStatement thenStatement = Translation.translateAsStatementAndMergeInBlockIfNeeded(thenExpression, context);
        JsStatement elseStatement = (elseExpression != null) ? Translation.translateAsStatementAndMergeInBlockIfNeeded(elseExpression,
                                                                                                                       context) : null;

        if (isKotlinExpression) {
            JsExpression jsThenExpression = JsAstUtils.extractExpressionFromStatement(thenStatement);
            JsExpression jsElseExpression = JsAstUtils.extractExpressionFromStatement(elseStatement);
            boolean canBeJsExpression = jsThenExpression != null && jsElseExpression != null;
            if (canBeJsExpression) {
                return new JsConditional(testExpression, jsThenExpression, jsElseExpression).source(expression);
            }
        }
        JsIf ifStatement = new JsIf(testExpression, thenStatement, elseStatement);
        return ifStatement.source(expression);
    }

    @Override
    @NotNull
    public JsExpression visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression,
            @NotNull TranslationContext context) {
        return ReferenceTranslator.translateSimpleNameWithQualifier(expression, null, context).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitWhileExpression(@NotNull JetWhileExpression expression, @NotNull TranslationContext context) {
        return LoopTranslatorPackage.createWhile(false, expression, context);
    }

    @Override
    @NotNull
    public JsNode visitDoWhileExpression(@NotNull JetDoWhileExpression expression, @NotNull TranslationContext context) {
        return LoopTranslatorPackage.createWhile(true, expression, context);
    }

    @Override
    @NotNull
    public JsNode visitStringTemplateExpression(@NotNull JetStringTemplateExpression expression,
            @NotNull TranslationContext context) {
        JsStringLiteral stringLiteral = resolveAsStringConstant(expression, context);
        if (stringLiteral != null) {
            return stringLiteral;
        }
        return resolveAsTemplate(expression, context).source(expression);
    }

    @NotNull
    private static JsNode resolveAsTemplate(@NotNull JetStringTemplateExpression expression,
            @NotNull TranslationContext context) {
        return StringTemplateTranslator.translate(expression, context);
    }

    @Nullable
    private static JsStringLiteral resolveAsStringConstant(@NotNull JetExpression expression,
            @NotNull TranslationContext context) {
        Object value = getCompileTimeValue(context.bindingContext(), expression);
        if (value == null) {
            return null;
        }
        assert value instanceof String : "Compile time constant template should be a String constant.";
        String constantString = (String) value;
        return context.program().getStringLiteral(constantString);
    }

    @Override
    @NotNull
    public JsNode visitDotQualifiedExpression(@NotNull JetDotQualifiedExpression expression,
            @NotNull TranslationContext context) {
        return QualifiedExpressionTranslator.translateQualifiedExpression(expression, context);
    }

    @Override
    public JsNode visitLabeledExpression(
            @NotNull JetLabeledExpression expression, TranslationContext context
    ) {
        JetExpression baseExpression = expression.getBaseExpression();
        assert baseExpression != null;
        JsName name = context.scope().declareName(getReferencedName(expression.getTargetLabel()));
        JsStatement baseStatement = Translation.translateAsStatement(baseExpression, context);
        return new JsLabel(name, baseStatement).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitPrefixExpression(
            @NotNull JetPrefixExpression expression,
            @NotNull TranslationContext context
    ) {
        return UnaryOperationTranslator.translate(expression, context).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitPostfixExpression(@NotNull JetPostfixExpression expression,
            @NotNull TranslationContext context) {
        return UnaryOperationTranslator.translate(expression, context).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitIsExpression(@NotNull JetIsExpression expression,
            @NotNull TranslationContext context) {
        return Translation.patternTranslator(context).translateIsExpression(expression);
    }

    @Override
    @NotNull
    public JsNode visitSafeQualifiedExpression(@NotNull JetSafeQualifiedExpression expression,
            @NotNull TranslationContext context) {
        return QualifiedExpressionTranslator.translateQualifiedExpression(expression, context).source(expression);
    }

    @Override
    @Nullable
    public JsNode visitWhenExpression(@NotNull JetWhenExpression expression,
            @NotNull TranslationContext context) {
        return WhenTranslator.translate(expression, context);
    }

    @Override
    @NotNull
    public JsNode visitBinaryWithTypeRHSExpression(@NotNull JetBinaryExpressionWithTypeRHS expression,
            @NotNull TranslationContext context) {
        JsExpression jsExpression = Translation.translateAsExpression(expression.getLeft(), context);

        if (expression.getOperationReference().getReferencedNameElementType() != JetTokens.AS_KEYWORD)
            return jsExpression.source(expression);

        JetTypeReference right = expression.getRight();
        assert right != null;

        JetType rightType = BindingContextUtils.getNotNull(context.bindingContext(), BindingContext.TYPE, right);
        JetType leftType = BindingContextUtils.getNotNull(context.bindingContext(), BindingContext.EXPRESSION_TYPE, expression.getLeft());
        if (TypeUtils.isNullableType(rightType) || !TypeUtils.isNullableType(leftType)) {
            return jsExpression.source(expression);
        }

        // KT-2670
        // we actually do not care for types in js
        return TranslationUtils.sure(jsExpression, context).source(expression);
    }

    private static String getReferencedName(JetSimpleNameExpression expression) {
        String name = expression.getReferencedName();
        return name.charAt(0) == '@' ? name.substring(1) + '$' : name;
    }

    private static String getTargetLabel(JetExpressionWithLabel expression, TranslationContext context) {
        JetSimpleNameExpression labelElement = expression.getTargetLabel();
        if (labelElement == null) {
            return null;
        }
        else {
            JsName name = context.scope().findName(getReferencedName(labelElement));
            assert name != null;
            return name.getIdent();
        }
    }

    @Override
    @NotNull
    public JsNode visitBreakExpression(@NotNull JetBreakExpression expression,
            @NotNull TranslationContext context) {
        return new JsBreak(getTargetLabel(expression, context)).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitContinueExpression(@NotNull JetContinueExpression expression,
            @NotNull TranslationContext context) {
        return new JsContinue(getTargetLabel(expression, context)).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitFunctionLiteralExpression(@NotNull JetFunctionLiteralExpression expression, @NotNull TranslationContext context) {
        return new LiteralFunctionTranslator(context).translate(expression.getFunctionLiteral());
    }

    @Override
    @NotNull
    public JsNode visitNamedFunction(@NotNull JetNamedFunction expression, @NotNull TranslationContext context) {
        JsExpression alias = new LiteralFunctionTranslator(context).translate(expression);

        FunctionDescriptor descriptor = getFunctionDescriptor(context.bindingContext(), expression);
        JsName name = context.getNameForDescriptor(descriptor);

        return new JsVars(new JsVars.JsVar(name, alias)).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitThisExpression(@NotNull JetThisExpression expression, @NotNull TranslationContext context) {
        DeclarationDescriptor thisExpression =
                getDescriptorForReferenceExpression(context.bindingContext(), expression.getInstanceReference());
        assert thisExpression != null : "This expression must reference a descriptor: " + expression.getText();

        return context.getThisObject(getReceiverParameterForDeclaration(thisExpression)).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitArrayAccessExpression(@NotNull JetArrayAccessExpression expression,
            @NotNull TranslationContext context) {
        return AccessTranslationUtils.translateAsGet(expression, context);
    }

    @Override
    @NotNull
    public JsNode visitSuperExpression(@NotNull JetSuperExpression expression, @NotNull TranslationContext context) {
        DeclarationDescriptor superClassDescriptor = context.bindingContext().get(BindingContext.REFERENCE_TARGET, expression.getInstanceReference());
        assert superClassDescriptor != null: message(expression);
        return translateAsFQReference(superClassDescriptor, context);
    }

    @Override
    @NotNull
    public JsNode visitForExpression(@NotNull JetForExpression expression,
            @NotNull TranslationContext context) {
        return LoopTranslatorPackage.translateForExpression(expression, context).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitTryExpression(@NotNull JetTryExpression expression,
            @NotNull TranslationContext context) {
        return TryTranslator.translate(expression, context).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitThrowExpression(@NotNull JetThrowExpression expression,
            @NotNull TranslationContext context) {
        JetExpression thrownExpression = expression.getThrownExpression();
        assert thrownExpression != null : "Thrown expression must not be null";
        return new JsThrow(translateAsExpression(thrownExpression, context)).source(expression);
    }

    @Override
    @NotNull
    public JsNode visitObjectLiteralExpression(@NotNull JetObjectLiteralExpression expression,
            @NotNull TranslationContext context) {
        return ClassTranslator.generateObjectLiteral(expression.getObjectDeclaration(), context);
    }

    @Override
    @NotNull
    public JsNode visitObjectDeclaration(@NotNull JetObjectDeclaration expression,
            @NotNull TranslationContext context) {
        DeclarationDescriptor descriptor = getDescriptorForElement(context.bindingContext(), expression);
        JsName name = context.getNameForDescriptor(descriptor);
        JsExpression value = ClassTranslator.generateClassCreation(expression, context);
        return newVar(name, value).source(expression);
    }
}
