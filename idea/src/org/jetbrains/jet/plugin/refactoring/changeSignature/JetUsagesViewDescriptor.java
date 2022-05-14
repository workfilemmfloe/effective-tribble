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

package org.jetbrains.jet.plugin.refactoring.changeSignature;

import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.usageView.UsageViewDescriptor;
import org.jetbrains.annotations.NotNull;

public class JetUsagesViewDescriptor implements UsageViewDescriptor {
    private final PsiElement myElement;
    private final String myElementsHeader;

    public JetUsagesViewDescriptor(PsiElement element, String elementsHeader) {
        myElement = element;
        myElementsHeader = elementsHeader;
    }

    @NotNull
    @Override
    public PsiElement[] getElements() {
        return myElement != null ? new PsiElement[] {myElement} : new PsiElement[0];
    }

    @Override
    public String getProcessedElementsHeader() {
        return myElementsHeader;
    }

    @Override
    public String getCodeReferencesText(int usagesCount, int filesCount) {
        return RefactoringBundle.message("references.to.be.changed", UsageViewBundle.getReferencesString(usagesCount, filesCount));
    }

    @Override
    public String getCommentReferencesText(int usagesCount, int filesCount) {
        return RefactoringBundle.message("comments.elements.header", UsageViewBundle.getOccurencesString(usagesCount, filesCount));
    }
}