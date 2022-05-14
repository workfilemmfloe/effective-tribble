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

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters1;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.util.ShortenReferences;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetParameter;
import org.jetbrains.kotlin.psi.JetTypeReference;
import org.jetbrains.kotlin.types.JetType;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

public class ChangeTypeFix extends JetIntentionAction<JetTypeReference> {
    private final JetType type;
    private final String renderedType;

    public ChangeTypeFix(@NotNull JetTypeReference element, JetType type) {
        super(element);
        this.type = type;
        renderedType = IdeDescriptorRenderers.SOURCE_CODE.renderType(type);
    }

    @NotNull
    @Override
    public String getText() {
        String currentTypeText = element.getText();
        return JetBundle.message("change.type", currentTypeText, QuickFixUtil.renderTypeWithFqNameOnClash(type, currentTypeText));
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.type.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        JetTypeReference newTypeRef = (JetTypeReference) element.replace(JetPsiFactory(file).createType(renderedType));
        ShortenReferences.DEFAULT.process(newTypeRef);
    }

    @NotNull
    public static JetSingleIntentionActionFactory createFactoryForExpectedParameterTypeMismatch() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(@NotNull Diagnostic diagnostic) {
                DiagnosticWithParameters1<JetParameter, JetType> diagnosticWithParameters = Errors.EXPECTED_PARAMETER_TYPE_MISMATCH.cast(diagnostic);
                JetTypeReference typeReference = diagnosticWithParameters.getPsiElement().getTypeReference();
                assert typeReference != null : "EXPECTED_PARAMETER_TYPE_MISMATCH reported on parameter without explicitly declared type";
                return new ChangeTypeFix(typeReference, diagnosticWithParameters.getA());
            }
        };
    }

    @NotNull
    public static JetSingleIntentionActionFactory createFactoryForExpectedReturnTypeMismatch() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(@NotNull Diagnostic diagnostic) {
                DiagnosticWithParameters1<JetTypeReference, JetType> diagnosticWithParameters = Errors.EXPECTED_RETURN_TYPE_MISMATCH.cast(diagnostic);
                return new ChangeTypeFix(diagnosticWithParameters.getPsiElement(), diagnosticWithParameters.getA());
            }
        };
    }
}
