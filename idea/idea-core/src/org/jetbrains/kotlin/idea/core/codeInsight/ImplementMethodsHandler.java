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

package org.jetbrains.kotlin.idea.core.codeInsight;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.resolve.OverrideResolver;

import java.util.Set;

public class ImplementMethodsHandler extends OverrideImplementMethodsHandler implements IntentionAction {
    @Override
    protected Set<CallableMemberDescriptor> collectMethodsToGenerate(@NotNull ClassDescriptor descriptor) {
        return OverrideResolver.getMissingImplementations(descriptor);
    }

    @Override
    protected String getChooserTitle() {
        return "Implement Members";
    }

    @Override
    protected String getNoMethodsFoundHint() {
        return "No methods to implement have been found";
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("implement.members");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("implement.members");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return isValidFor(editor, file);
    }
}
