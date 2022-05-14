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

package org.jetbrains.jet.plugin.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lexer.JetTokens;

/**
 * Special contributor for getting completion of type for extensions receiver.
 */
public class JetExtensionReceiverTypeContributor extends CompletionContributor {
    // A way to add reference into file at completion place
    public static final String DUMMY_IDENTIFIER = "KotlinExtensionDummy.fake() {}";

    public static final ElementPattern<? extends PsiElement> ACTIVATION_PATTERN =
            PlatformPatterns.psiElement().afterLeaf(
                    JetTokens.FUN_KEYWORD.toString(),
                    JetTokens.VAL_KEYWORD.toString(),
                    JetTokens.VAR_KEYWORD.toString());

    public JetExtensionReceiverTypeContributor() {
        extend(CompletionType.BASIC, ACTIVATION_PATTERN, new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(
                    @NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result
            ) {
                if (parameters.getInvocationCount() > 0) {
                    JetCompletionContributor.doSimpleReferenceCompletion(parameters, result);
                }

                result.stopHere();
            }
        });
    }
}
