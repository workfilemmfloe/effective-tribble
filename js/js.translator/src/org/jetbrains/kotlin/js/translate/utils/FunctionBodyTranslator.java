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

package org.jetbrains.kotlin.js.translate.utils;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.utils.mutator.Mutator;
import org.jetbrains.kotlin.psi.KtDeclarationWithBody;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getDefaultArgument;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.*;
import static org.jetbrains.kotlin.js.translate.utils.mutator.LastExpressionMutator.mutateLastExpression;

public final class FunctionBodyTranslator extends AbstractTranslator {

    @NotNull
    public static JsBlock translateFunctionBody(@NotNull FunctionDescriptor descriptor,
                                                @NotNull KtDeclarationWithBody declarationWithBody,
                                                @NotNull TranslationContext functionBodyContext) {
        return (new FunctionBodyTranslator(descriptor, declarationWithBody, functionBodyContext)).translate();
    }

    @NotNull
    public static List<JsStatement> setDefaultValueForArguments(@NotNull FunctionDescriptor descriptor,
            @NotNull TranslationContext functionBodyContext) {
        List<ValueParameterDescriptor> valueParameters = descriptor.getValueParameters();

        List<JsStatement> result = new ArrayList<JsStatement>(valueParameters.size());
        for (ValueParameterDescriptor valueParameter : valueParameters) {
            if (!DescriptorUtilsKt.hasDefaultValue(valueParameter)) continue;

            JsNameRef jsNameRef = functionBodyContext.getNameForDescriptor(valueParameter).makeRef();
            KtExpression defaultArgument = getDefaultArgument(valueParameter);
            JsBlock defaultArgBlock = new JsBlock();
            JsExpression defaultValue = Translation.translateAsExpression(defaultArgument, functionBodyContext, defaultArgBlock);
            JsStatement assignStatement = assignment(jsNameRef, defaultValue).makeStmt();
            JsStatement thenStatement = JsAstUtils.mergeStatementInBlockIfNeeded(assignStatement, defaultArgBlock);
            JsBinaryOperation checkArgIsUndefined = equality(jsNameRef, functionBodyContext.namer().getUndefinedExpression());
            JsIf jsIf = JsAstUtils.newJsIf(checkArgIsUndefined, thenStatement);
            result.add(jsIf);
        }

        return result;
    }

    @NotNull
    private final FunctionDescriptor descriptor;
    @NotNull
    private final KtDeclarationWithBody declaration;

    private FunctionBodyTranslator(@NotNull FunctionDescriptor descriptor,
                                   @NotNull KtDeclarationWithBody declaration,
                                   @NotNull TranslationContext context) {
        super(context);
        this.descriptor = descriptor;
        this.declaration = declaration;
    }

    @NotNull
    private JsBlock translate() {
        KtExpression jetBodyExpression = declaration.getBodyExpression();
        assert jetBodyExpression != null : "Cannot translate a body of an abstract function.";
        JsBlock jsBlock = new JsBlock(setDefaultValueForArguments(descriptor, context()));
        jsBlock.getStatements().addAll(mayBeWrapWithReturn(Translation.translateExpression(jetBodyExpression, context(), jsBlock)).getStatements());
        return jsBlock;
    }

    @NotNull
    private JsBlock mayBeWrapWithReturn(@NotNull JsNode body) {
        if (!mustAddReturnToGeneratedFunctionBody()) {
            return convertToBlock(body);
        }
        return convertToBlock(lastExpressionReturned(body));
    }

    private boolean mustAddReturnToGeneratedFunctionBody() {
        KotlinType functionReturnType = descriptor.getReturnType();
        assert functionReturnType != null : "Function return typed type must be resolved.";
        return (!declaration.hasBlockBody()) && (!KotlinBuiltIns.isUnit(functionReturnType));
    }

    @NotNull
    private static JsNode lastExpressionReturned(@NotNull JsNode body) {
        return mutateLastExpression(body, new Mutator() {
            @Override
            @NotNull
            public JsNode mutate(@NotNull JsNode node) {
                if (!(node instanceof JsExpression)) {
                    return node;
                }
                return new JsReturn((JsExpression)node);
            }
        });
    }
}
