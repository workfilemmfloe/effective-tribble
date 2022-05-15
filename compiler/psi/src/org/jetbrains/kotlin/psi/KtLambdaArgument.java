/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

public class KtLambdaArgument extends KtValueArgument implements LambdaArgument {
    public KtLambdaArgument(@NotNull ASTNode node) {
        super(node);
    }

    public KtLambdaArgument(@NotNull KotlinPlaceHolderStub<KtLambdaArgument> stub) {
        super(stub, KtStubElementTypes.LAMBDA_ARGUMENT);
    }

    @Nullable
    @Override
    public KtLambdaExpression getLambdaExpression() {
        KtExpression expression = getArgumentExpression();
        if (expression == null) {
            return null;
        }

        return KtElementUtilsKt.unpackFunctionLiteral(expression, false);
    }
}
