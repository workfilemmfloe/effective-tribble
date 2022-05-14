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

package org.jetbrains.kotlin.idea.intentions;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.idea.util.ShortenReferences;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.types.JetType;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

public class ReconstructTypeInCastOrIsAction extends PsiElementBaseIntentionAction {
    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("replace.by.reconstructed.type.family.name");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        JetTypeReference typeRef = PsiTreeUtil.getTopmostParentOfType(element, JetTypeReference.class);
        assert typeRef != null : "Must be checked by isAvailable(): " + element;

        JetType type = getReconstructedType(typeRef);
        JetTypeReference newType = JetPsiFactory(typeRef).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type));
        JetTypeReference replaced = (JetTypeReference) typeRef.replace(newType);
        ShortenReferences.DEFAULT.process(replaced);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        JetTypeReference typeRef = PsiTreeUtil.getTopmostParentOfType(element, JetTypeReference.class);
        if (typeRef == null) return false;

        // Only user types (like Foo) are interesting
        JetTypeElement typeElement = typeRef.getTypeElement();
        if (!(typeElement instanceof JetUserType)) return false;

        // If there are generic arguments already, there's nothing to reconstruct
        if (!((JetUserType) typeElement).getTypeArguments().isEmpty()) return false;

        // We must be on the RHS of as/as?/is/!is or inside an is/!is-condition in when()
        JetExpression outerExpression = PsiTreeUtil.getParentOfType(typeRef, JetExpression.class);
        if (!(outerExpression instanceof JetBinaryExpressionWithTypeRHS)) {
            JetWhenConditionIsPattern outerIsCondition = PsiTreeUtil.getParentOfType(typeRef, JetWhenConditionIsPattern.class);
            if (outerIsCondition == null) return false;
        }

        JetType type = getReconstructedType(typeRef);
        if (type == null || type.isError()) return false;

        // No type parameters expected => nothing to reconstruct
        if (type.getConstructor().getParameters().isEmpty()) return false;

        setText(JetBundle.message("replace.by.reconstructed.type", type));

        return true;
    }

    private static JetType getReconstructedType(JetTypeReference typeRef) {
        return ResolvePackage.analyze(typeRef, BodyResolveMode.FULL).get(BindingContext.TYPE, typeRef);
    }
}
