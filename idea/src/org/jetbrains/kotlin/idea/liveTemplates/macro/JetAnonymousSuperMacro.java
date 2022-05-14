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

package org.jetbrains.kotlin.idea.liveTemplates.macro;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Macro;
import com.intellij.codeInsight.template.Result;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ClassKind;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter;
import org.jetbrains.kotlin.resolve.scopes.JetScope;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class JetAnonymousSuperMacro extends Macro {
    @Override
    public String getName() {
        return "anonymousSuper";
    }

    @Override
    public String getPresentableName() {
        return JetBundle.message("macro.fun.anonymousSuper");
    }

    @Override
    public Result calculateResult(@NotNull Expression[] params, ExpressionContext context) {
        Editor editor = context.getEditor();
        if (editor != null) {
            AnonymousTemplateEditingListener.registerListener(editor, context.getProject());
        }

        PsiNamedElement[] vars = getSupertypes(params, context);
        if (vars == null || vars.length == 0) return null;
        return new JetPsiElementResult(vars[0]);
    }

    @Override
    public LookupElement[] calculateLookupItems(@NotNull Expression[] params, ExpressionContext context) {
        PsiNamedElement[] vars = getSupertypes(params, context);
        if (vars == null || vars.length < 2) return null;
        Set<LookupElement> set = new LinkedHashSet<LookupElement>();
        for (PsiNamedElement var : vars) {
            set.add(LookupElementBuilder.create(var));
        }
        return set.toArray(new LookupElement[set.size()]);
    }

    @Nullable
    private static PsiNamedElement[] getSupertypes(Expression[] params, ExpressionContext context) {
        if (params.length != 0) return null;

        Project project = context.getProject();
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(context.getEditor().getDocument());
        if (!(psiFile instanceof JetFile)) return null;

        JetExpression expression = PsiTreeUtil.getParentOfType(psiFile.findElementAt(context.getStartOffset()), JetExpression.class);
        if (expression == null) return null;

        BindingContext bc = ResolutionUtils.analyze(expression, BodyResolveMode.FULL);
        JetScope scope = bc.get(BindingContext.RESOLUTION_SCOPE, expression);
        if (scope == null) return null;

        List<PsiNamedElement> result = new ArrayList<PsiNamedElement>();

        for (DeclarationDescriptor descriptor : scope.getDescriptors(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS, JetScope.Companion.getALL_NAME_FILTER())) {
            if (!(descriptor instanceof ClassDescriptor)) continue;
            ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
            if (!classDescriptor.getModality().isOverridable()) continue;
            ClassKind kind = classDescriptor.getKind();
            if (kind == ClassKind.INTERFACE || kind == ClassKind.CLASS) {
                PsiElement declaration = DescriptorToSourceUtils.descriptorToDeclaration(descriptor);
                if (declaration != null) {
                    result.add((PsiNamedElement) declaration);
                }
            }
        }

        return result.toArray(new PsiNamedElement[result.size()]);
    }
}
