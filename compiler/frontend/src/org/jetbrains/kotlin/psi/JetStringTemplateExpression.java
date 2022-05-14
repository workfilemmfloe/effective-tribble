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

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.JetNodeTypes;
import org.jetbrains.kotlin.lexer.JetTokens;

public class JetStringTemplateExpression extends JetExpressionImpl implements PsiLanguageInjectionHost {
    private static final TokenSet TOKENS_SUITABLE_FOR_INJECTION = TokenSet.create(JetNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY, JetNodeTypes.ESCAPE_STRING_TEMPLATE_ENTRY);

    public JetStringTemplateExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitStringTemplateExpression(this, data);
    }

    @NotNull
    public JetStringTemplateEntry[] getEntries() {
        return findChildrenByClass(JetStringTemplateEntry.class);
    }

    @Override
    public boolean isValidHost() {
        ASTNode node = getNode();
        ASTNode child = node.getFirstChildNode().getTreeNext();
        while (child != null) {
            if (child.getElementType() == JetTokens.CLOSING_QUOTE) return true;
            if (!TOKENS_SUITABLE_FOR_INJECTION.contains(child.getElementType())) return false;
            child = child.getTreeNext();
        }
        return false;
    }

    @Override
    public PsiLanguageInjectionHost updateText(@NotNull String text) {
        return ElementManipulators.handleContentChange(this, text);
    }

    @NotNull
    @Override
    public LiteralTextEscaper<? extends PsiLanguageInjectionHost> createLiteralTextEscaper() {
        return new KotlinStringLiteralTextEscaper(this);
    }
}
