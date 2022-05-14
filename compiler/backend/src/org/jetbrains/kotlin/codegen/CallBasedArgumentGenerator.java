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

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.ValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.*;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.List;

import static org.jetbrains.kotlin.codegen.AsmUtil.pushDefaultValueOnStack;

public class CallBasedArgumentGenerator extends ArgumentGenerator {
    private final ExpressionCodegen codegen;
    private final CallGenerator callGenerator;
    private final List<ValueParameterDescriptor> valueParameters;
    private final List<Type> valueParameterTypes;

    public CallBasedArgumentGenerator(
            @NotNull ExpressionCodegen codegen,
            @NotNull CallGenerator callGenerator,
            @NotNull List<ValueParameterDescriptor> valueParameters,
            @NotNull List<Type> valueParameterTypes
    ) {
        this.codegen = codegen;
        this.callGenerator = callGenerator;
        this.valueParameters = valueParameters;
        this.valueParameterTypes = valueParameterTypes;

        assert valueParameters.size() == valueParameterTypes.size() :
                "Value parameters and their types mismatch in sizes: " + valueParameters.size() + " != " + valueParameterTypes.size();
    }


    @NotNull
    @Override
    public DefaultCallMask generate(
            @NotNull List<? extends ResolvedValueArgument> valueArgumentsByIndex,
            @NotNull List<? extends ResolvedValueArgument> valueArgs
    ) {
        boolean shouldMarkLineNumbers = this.codegen.isShouldMarkLineNumbers();
        this.codegen.setShouldMarkLineNumbers(false);
        DefaultCallMask masks = super.generate(valueArgumentsByIndex, valueArgs);
        this.codegen.setShouldMarkLineNumbers(shouldMarkLineNumbers);
        return masks;
    }

    @Override
    protected void generateExpression(int i, @NotNull ExpressionValueArgument argument) {
        ValueParameterDescriptor parameter = valueParameters.get(i);
        Type type = valueParameterTypes.get(i);
        ValueArgument valueArgument = argument.getValueArgument();
        assert valueArgument != null;
        KtExpression argumentExpression = valueArgument.getArgumentExpression();
        assert argumentExpression != null : valueArgument.asElement().getText();
        callGenerator.genValueAndPut(parameter, argumentExpression, type, i);
    }

    @Override
    protected void generateDefault(int i, @NotNull DefaultValueArgument argument) {
        ValueParameterDescriptor parameter = valueParameters.get(i);
        Type type = valueParameterTypes.get(i);
        pushDefaultValueOnStack(type, codegen.v);
        callGenerator.afterParameterPut(type, null, parameter, i);
    }

    @Override
    protected void generateVararg(int i, @NotNull VarargValueArgument argument) {
        ValueParameterDescriptor parameter = valueParameters.get(i);
        Type type = valueParameterTypes.get(i);
        codegen.genVarargs(argument, parameter.getType());
        callGenerator.afterParameterPut(type, null, parameter, i);
    }

    @Override
    protected void reorderArgumentsIfNeeded(@NotNull List<ArgumentAndDeclIndex> actualArgsWithDeclIndex) {
        callGenerator.reorderArgumentsIfNeeded(actualArgsWithDeclIndex, valueParameterTypes);
    }
}
