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

package org.jetbrains.kotlin.codegen.when;

import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.ExpressionCodegen;
import org.jetbrains.kotlin.codegen.binding.CodegenBinding;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.constants.IntegerValueConstant;
import org.jetbrains.kotlin.resolve.constants.NullValue;
import org.jetbrains.kotlin.resolve.constants.StringValue;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

public class SwitchCodegenUtil {
    public static boolean checkAllItemsAreConstantsSatisfying(
            @NotNull JetWhenExpression expression,
            @NotNull BindingContext bindingContext,
            Function1<ConstantValue<?>, Boolean> predicate
    ) {
        for (JetWhenEntry entry : expression.getEntries()) {
            for (JetWhenCondition condition : entry.getConditions()) {
                if (!(condition instanceof JetWhenConditionWithExpression)) {
                    return false;
                }

                // ensure that expression is constant
                JetExpression patternExpression = ((JetWhenConditionWithExpression) condition).getExpression();

                if (patternExpression == null) return false;

                ConstantValue<?> constant = ExpressionCodegen.getCompileTimeConstant(patternExpression, bindingContext);
                if (constant == null || !predicate.invoke(constant)) {
                    return false;
                }
            }
        }

        return true;
    }

    @NotNull
    public static Iterable<ConstantValue<?>> getAllConstants(
            @NotNull JetWhenExpression expression,
            @NotNull BindingContext bindingContext
    ) {
        List<ConstantValue<?>> result = new ArrayList<ConstantValue<?>>();

        for (JetWhenEntry entry : expression.getEntries()) {
            addConstantsFromEntry(result, entry, bindingContext);
        }

        return result;
    }

    private static void addConstantsFromEntry(
            @NotNull List<ConstantValue<?>> result,
            @NotNull JetWhenEntry entry,
            @NotNull BindingContext bindingContext
    ) {
        for (JetWhenCondition condition : entry.getConditions()) {
            if (!(condition instanceof JetWhenConditionWithExpression)) continue;

            JetExpression patternExpression = ((JetWhenConditionWithExpression) condition).getExpression();

            assert patternExpression != null : "expression in when should not be null";
            result.add(ExpressionCodegen.getCompileTimeConstant(patternExpression, bindingContext));
        }
    }

    @NotNull
    public static Iterable<ConstantValue<?>> getConstantsFromEntry(
            @NotNull JetWhenEntry entry,
            @NotNull BindingContext bindingContext
    ) {
        List<ConstantValue<?>> result = new ArrayList<ConstantValue<?>>();
        addConstantsFromEntry(result, entry, bindingContext);
        return result;
    }

    @Nullable
    public static SwitchCodegen buildAppropriateSwitchCodegenIfPossible(
            @NotNull JetWhenExpression expression,
            boolean isStatement,
            @NotNull ExpressionCodegen codegen
    ) {
        BindingContext bindingContext = codegen.getBindingContext();
        if (!isThereConstantEntriesButNulls(expression, bindingContext)) {
            return null;
        }

        Type subjectType = codegen.expressionType(expression.getSubjectExpression());

        WhenByEnumsMapping mapping = codegen.getBindingContext().get(CodegenBinding.MAPPING_FOR_WHEN_BY_ENUM, expression);

        if (mapping != null) {
            return new EnumSwitchCodegen(expression, isStatement, codegen, mapping);
        }

        if (isIntegralConstantsSwitch(expression, subjectType, bindingContext)) {
            return new IntegralConstantsSwitchCodegen(expression, isStatement, codegen);
        }

        if (isStringConstantsSwitch(expression, subjectType, bindingContext)) {
            return new StringSwitchCodegen(expression, isStatement, codegen);
        }

        return null;
    }

    private static boolean isThereConstantEntriesButNulls(
            @NotNull JetWhenExpression expression,
            @NotNull BindingContext bindingContext
    ) {
        for (ConstantValue<?> constant : getAllConstants(expression, bindingContext)) {
            if (constant != null && !(constant instanceof NullValue)) return true;
        }

        return false;
    }

    private static boolean isIntegralConstantsSwitch(
            @NotNull JetWhenExpression expression,
            @NotNull Type subjectType,
            @NotNull BindingContext bindingContext
    ) {
        int typeSort = subjectType.getSort();

        if (typeSort != Type.INT && typeSort != Type.CHAR && typeSort != Type.SHORT && typeSort != Type.BYTE) {
            return false;
        }

        return checkAllItemsAreConstantsSatisfying(expression, bindingContext, new Function1<ConstantValue<?>, Boolean>() {
            @Override
            public Boolean invoke(
                    @NotNull ConstantValue<?> constant
            ) {
                return constant instanceof IntegerValueConstant;
            }
        });
    }

    private static boolean isStringConstantsSwitch(
            @NotNull JetWhenExpression expression,
            @NotNull Type subjectType,
            @NotNull BindingContext bindingContext
    ) {

        if (!subjectType.getClassName().equals(String.class.getName())) {
            return false;
        }

        return checkAllItemsAreConstantsSatisfying(expression, bindingContext, new Function1<ConstantValue<?>, Boolean>() {
            @Override
            public Boolean invoke(
                    @NotNull ConstantValue<?> constant
            ) {
                return constant instanceof StringValue || constant instanceof NullValue;
            }
        });
    }
}
