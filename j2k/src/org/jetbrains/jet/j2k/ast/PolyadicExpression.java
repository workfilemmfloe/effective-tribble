/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.util.AstUtil;

import java.util.List;

/**
 * @author ignatov
 */
public class PolyadicExpression extends Expression {
    private final List<Expression> myExpressions;
    private final String myToken;
    private final List<String> myConversions;

    public PolyadicExpression(List<Expression> expressions, String token, List<String> conversions) {
        super();
        myExpressions = expressions;
        myToken = token;
        myConversions = conversions;
    }

    @NotNull
    @Override
    public String toKotlin() {
        List<String> expressionsWithConversions = AstUtil.applyConversions(AstUtil.nodesToKotlin(myExpressions), myConversions);
        return AstUtil.join(expressionsWithConversions, SPACE + myToken + SPACE);
    }
}
