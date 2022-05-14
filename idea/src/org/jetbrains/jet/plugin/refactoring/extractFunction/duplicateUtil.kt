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

package org.jetbrains.jet.plugin.refactoring.extractFunction

import org.jetbrains.jet.plugin.util.psi.patternMatching.JetPsiRange
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Editor
import java.util.ArrayList
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.ScrollType
import org.jetbrains.jet.plugin.refactoring.JetRefactoringBundle
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.util.Ref
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.ui.ReplacePromptDialog
import com.intellij.find.FindManager
import org.jetbrains.jet.plugin.util.application.executeWriteCommand
import com.intellij.refactoring.util.duplicates.MethodDuplicatesHandler
import com.intellij.refactoring.RefactoringBundle

public fun JetPsiRange.highlight(project: Project, editor: Editor): RangeHighlighter? {
    val textRange = getTextRange()
    val highlighters = ArrayList<RangeHighlighter>()
    val attributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES)!!
    HighlightManager.getInstance(project).addRangeHighlight(
            editor, textRange.getStartOffset(), textRange.getEndOffset(), attributes, true, highlighters
    )
    return highlighters.firstOrNull()
}

public fun JetPsiRange.preview(project: Project, editor: Editor): RangeHighlighter? {
    return highlight(project, editor)?.let {
        val startOffset = getTextRange().getStartOffset()
        val foldedRegions =
                CodeFoldingManager.getInstance(project)
                        .getFoldRegionsAtOffset(editor, startOffset)
                        .filter { !it.isExpanded() }
        if (!foldedRegions.empty) {
            editor.getFoldingModel().runBatchFoldingOperation { foldedRegions.forEach { it.setExpanded(true) } }
        }
        editor.getScrollingModel().scrollTo(editor.offsetToLogicalPosition(startOffset), ScrollType.MAKE_VISIBLE)

        it
    }
}

public fun processDuplicates(
        duplicateReplacers: Map<JetPsiRange, () -> Unit>,
        project: Project,
        editor: Editor
) {
    val size = duplicateReplacers.size
    if (size == 0) return

    if (size == 1) {
        duplicateReplacers.keySet().first().preview(project, editor)
    }

    val answer = if (ApplicationManager.getApplication()!!.isUnitTestMode())
        Messages.YES
    else
        Messages.showYesNoDialog(
                project,
                JetRefactoringBundle.message(
                        "0.has.detected.1.code.fragments.in.this.file.that.can.be.replaced.with.a.call.to.extracted.declaration",
                        ApplicationNamesInfo.getInstance()!!.getProductName(),
                        duplicateReplacers.size()
                ),
                "Process Duplicates",
                Messages.getQuestionIcon()
        )
    if (answer != Messages.YES) return

    var showAll = false
    for ((i, entry) in duplicateReplacers.entrySet().withIndices()) {
        val (pattern, replacer) = entry
        if (!pattern.isValid()) continue

        val highlighter = pattern.preview(project, editor)
        if (!ApplicationManager.getApplication()!!.isUnitTestMode()) {
            if (size > 1 && !showAll) {
                val promptDialog = ReplacePromptDialog(false, RefactoringBundle.message("process.duplicates.title", i + 1, size), project)
                promptDialog.show()
                when(promptDialog.getExitCode()) {
                    FindManager.PromptResult.ALL -> showAll = true
                    FindManager.PromptResult.SKIP -> continue
                    FindManager.PromptResult.CANCEL -> return
                }
            }
        }
        highlighter?.let { HighlightManager.getInstance(project).removeSegmentHighlighter(editor, it) }

        project.executeWriteCommand(MethodDuplicatesHandler.REFACTORING_NAME, replacer)
    }
}
