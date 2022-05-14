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

package org.jetbrains.kotlin.js.translate.expression;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.backend.js.ast.JsNumberLiteral;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.descriptorUtils.DescriptorUtilsKt;
import org.jetbrains.kotlin.js.patterns.NamePredicate;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.intrinsic.functions.factories.TopLevelFIF;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.types.KotlinType;

import static org.jetbrains.kotlin.js.translate.utils.ErrorReportingUtils.message;
import static org.jetbrains.kotlin.js.translate.utils.JsAstUtils.sum;

public final class StringTemplateTranslator extends AbstractTranslator {
    private final KtStringTemplateEntry[] expressionEntries;

    @NotNull
    public static JsExpression translate(@NotNull KtStringTemplateExpression expression,
                                         @NotNull TranslationContext context) {
        return (new StringTemplateTranslator(expression, context).translate());
    }

    private StringTemplateTranslator(@NotNull KtStringTemplateExpression expression,
                                     @NotNull TranslationContext context) {
        super(context);

        expressionEntries = expression.getEntries();
        assert expressionEntries.length != 0 : message(expression, "String template must have one or more entries.");
    }

    @NotNull
    private JsExpression translate() {
        EntryVisitor entryVisitor = new EntryVisitor();
        for (KtStringTemplateEntry entry : expressionEntries) {
            entry.accept(entryVisitor);
        }
        return entryVisitor.getResultingExpression();
    }

    private final class EntryVisitor extends KtVisitorVoid {

        @Nullable
        private JsExpression resultingExpression = null;

        void append(@NotNull JsExpression expression) {
            if (resultingExpression == null) {
                resultingExpression = expression;
            }
            else {
                resultingExpression = sum(resultingExpression, expression);
            }
        }

        @Override
        public void visitStringTemplateEntryWithExpression(@NotNull KtStringTemplateEntryWithExpression entry) {
            KtExpression entryExpression = entry.getExpression();
            assert entryExpression != null :
                    "JetStringTemplateEntryWithExpression must have not null entry expression.";
            JsExpression translatedExpression = Translation.translateAsExpression(entryExpression, context());
            if (translatedExpression instanceof JsNumberLiteral) {
                append(context().program().getStringLiteral(translatedExpression.toString()));
                return;
            }

            KotlinType type = context().bindingContext().getType(entryExpression);
            if (type == null || type.isMarkedNullable()) {
                append(TopLevelFIF.TO_STRING.apply((JsExpression) null, new SmartList<JsExpression>(translatedExpression), context()));
            }
            else if (mustCallToString(type)) {
                append(new JsInvocation(new JsNameRef("toString", translatedExpression)));
            }
            else {
                append(translatedExpression);
            }
        }

        private boolean mustCallToString(@NotNull KotlinType type) {
            Name typeName = DescriptorUtilsKt.getNameIfStandardType(type);
            if (typeName != null) {
                //TODO: this is a hacky optimization, should use some generic approach
                if (NamePredicate.STRING.apply(typeName)) {
                    return false;
                }
                else if (NamePredicate.PRIMITIVE_NUMBERS.apply(typeName)) {
                    return resultingExpression == null;
                }
            }
            return expressionEntries.length == 1;
        }

        @Override
        public void visitLiteralStringTemplateEntry(@NotNull KtLiteralStringTemplateEntry entry) {
            appendText(entry.getText());
        }

        @Override
        public void visitEscapeStringTemplateEntry(@NotNull KtEscapeStringTemplateEntry entry) {
            appendText(entry.getUnescapedValue());
        }

        private void appendText(@NotNull String text) {
            append(program().getStringLiteral(text));
        }

        @NotNull
        public JsExpression getResultingExpression() {
            assert resultingExpression != null;
            return resultingExpression;
        }
    }
}
