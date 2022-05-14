/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.intentions.declarations;

import com.intellij.codeInsight.editorActions.JoinRawLinesHandlerDelegate;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import kotlin.Function1;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.psi.psiUtil.PsiUtilPackage;

public class JetDeclarationJoinLinesHandler implements JoinRawLinesHandlerDelegate {
    private static final Function1<PsiElement, Boolean> IS_APPLICABLE = new Function1<PsiElement, Boolean>() {
        @Override
        public Boolean invoke(@Nullable PsiElement input) {
            return input != null && DeclarationUtils.checkAndGetPropertyAndInitializer(input) != null;
        }
    };

    @Override
    public int tryJoinRawLines(Document document, PsiFile file, int start, int end) {
        PsiElement element = JetPsiUtil.skipSiblingsBackwardByPredicate(file.findElementAt(start), DeclarationUtils.SKIP_DELIMITERS);

        //noinspection unchecked
        PsiElement target = PsiUtilPackage.getParentByTypesAndPredicate(element, false, ArrayUtil.EMPTY_CLASS_ARRAY, IS_APPLICABLE);
        if (target == null) return -1;

        return DeclarationUtils.joinPropertyDeclarationWithInitializer(target).getTextRange().getStartOffset();
    }

    @Override
    public int tryJoinLines(Document document, PsiFile file, int start, int end) {
        return -1;
    }
}