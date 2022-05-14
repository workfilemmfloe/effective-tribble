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

package org.jetbrains.kotlin.idea.quickfix.replaceJavaClass

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.quickfix.JetIntentionAction
import org.jetbrains.kotlin.idea.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.quickfix.JetWholeProjectForEachElementOfTypeFix
import org.jetbrains.kotlin.idea.quickfix.JetWholeProjectModalAction
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionFactory
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionForFirstParentOfType
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import java.util.ArrayList

public class ReplaceJavaClassAsAnnotationParameterFix(
        annotationClass: JetClass
) : JetIntentionAction<JetClass>(annotationClass) {

    override fun getText() = JetBundle.message("replace.java.class.parameter")
    override fun getFamilyName() = JetBundle.message("replace.java.class.parameter.family")

    override fun invoke(project: Project, editor: Editor?, file: JetFile?) {
        processTasks(createReplacementTasksForAnnotationClass(element))
    }

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) =
                diagnostic.createIntentionForFirstParentOfType(::ReplaceJavaClassAsAnnotationParameterFix)

        public fun createWholeProjectFixFactory(): JetSingleIntentionActionFactory = createIntentionFactory {
            JetWholeProjectForEachElementOfTypeFix.createForMultiTask(
                    tasksFactory = ::createReplacementTasksForAnnotationClass,
                    tasksProcessor = ::processTasks,
                    modalTitle = JetBundle.message("replace.java.class.parameter.in.whole.project.modal.title"),
                    name = JetBundle.message("replace.java.class.parameter.in.whole.project"),
                    familyName = JetBundle.message("replace.java.class.parameter.in.whole.project.family")
            )
        }
    }
}
