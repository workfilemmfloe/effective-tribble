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

package org.jetbrains.jet.lang.diagnostics;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PositioningStrategy<E extends PsiElement> {
    @NotNull
    public List<TextRange> markDiagnostic(@NotNull ParametrizedDiagnostic<? extends E> diagnostic) {
        return mark(diagnostic.getPsiElement());
    }

    @NotNull
    protected List<TextRange> mark(@NotNull E element) {
        return markElement(element);
    }

    public boolean isValid(@NotNull E element) {
        return !hasSyntaxErrors(element);
    }

    @NotNull
    protected static List<TextRange> markElement(@NotNull PsiElement element) {
        return Collections.singletonList(new TextRange(getStartOffset(element), getEndOffset(element)));
    }

    @NotNull
    protected static List<TextRange> markNode(@NotNull ASTNode node) {
        return markElement(node.getPsi());
    }

    @NotNull
    protected static List<TextRange> markRange(@NotNull TextRange range) {
        return Collections.singletonList(range);
    }

    @NotNull
    protected static List<TextRange> markRange(@NotNull PsiElement from, @NotNull PsiElement to) {
        return markRange(new TextRange(getStartOffset(from), getEndOffset(to)));
    }

    private static int getStartOffset(@NotNull PsiElement element) {
        PsiElement child = element.getFirstChild();
        if (child != null) {
            while (child instanceof PsiComment || child instanceof PsiWhiteSpace) {
                child = child.getNextSibling();
            }
            if (child != null) {
                return getStartOffset(child);
            }
        }
        return element.getTextRange().getStartOffset();
    }

    private static int getEndOffset(@NotNull PsiElement element) {
        PsiElement child = element.getLastChild();
        if (child != null) {
            while (child instanceof PsiComment || child instanceof PsiWhiteSpace) {
                child = child.getPrevSibling();
            }
            if (child != null) {
                return getEndOffset(child);
            }
        }
        return element.getTextRange().getEndOffset();
    }

    protected static boolean hasSyntaxErrors(@NotNull PsiElement psiElement) {
        if (psiElement instanceof PsiErrorElement) return true;

        PsiElement[] children = psiElement.getChildren();
        if (children.length > 0 && hasSyntaxErrors(children[children.length - 1])) return true;

        return false;
    }
}
