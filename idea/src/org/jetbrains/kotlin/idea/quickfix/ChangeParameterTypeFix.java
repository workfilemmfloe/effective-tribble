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

package org.jetbrains.kotlin.idea.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.util.ShortenReferences;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.types.JetType;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

public class ChangeParameterTypeFix extends JetIntentionAction<JetParameter> {
    private final JetType type;
    private final String containingDeclarationName;
    private final boolean isPrimaryConstructorParameter;

    public ChangeParameterTypeFix(@NotNull JetParameter element, @NotNull JetType type) {
        super(element);
        this.type = type;
        JetNamedDeclaration declaration = PsiTreeUtil.getParentOfType(element, JetNamedDeclaration.class);
        isPrimaryConstructorParameter = declaration instanceof JetClass;
        FqName declarationFQName = declaration == null ? null : declaration.getFqName();
        containingDeclarationName = declarationFQName == null ? declaration.getName() : declarationFQName.asString();
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return super.isAvailable(project, editor, file) && containingDeclarationName != null;
    }

    @NotNull
    @Override
    public String getText() {
        String renderedType = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type);
        return isPrimaryConstructorParameter ?
            JetBundle.message("change.primary.constructor.parameter.type", element.getName(), containingDeclarationName, renderedType) :
            JetBundle.message("change.function.parameter.type", element.getName(), containingDeclarationName, renderedType);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.type.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        JetTypeReference newTypeRef = JetPsiFactory(file).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type));
        newTypeRef = element.setTypeReference(newTypeRef);
        assert newTypeRef != null;
        ShortenReferences.DEFAULT.process(newTypeRef);
    }
}
