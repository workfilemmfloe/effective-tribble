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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.ui.EditorNotifications
import com.intellij.ui.EditorNotificationPanel
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.fileEditor.FileEditor
import org.jetbrains.kotlin.idea.KotlinFileType
import com.intellij.psi.PsiManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtFile
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.ActivateToolWindowAction
import com.intellij.notification.EventLog

private val ERROR_HIGHLIGHT_PANEL_KEY = Key.create<EditorNotificationPanel>("kotlin.error.highlight.panel.key")
private val HAS_ERRORS_IN_HIGHLIHTING_KEY = Key.create<Boolean>("kotlin.has.errors.in.higlighting.key")

fun updateHighlightingResult(file: KtFile, hasErrors: Boolean) {
    if (hasErrors != file.getUserData(HAS_ERRORS_IN_HIGHLIHTING_KEY)) {
        file.putUserData(HAS_ERRORS_IN_HIGHLIHTING_KEY, hasErrors)

        val vFile = file.virtualFile
        if (vFile != null) {
            EditorNotifications.getInstance(file.project)?.updateNotifications(vFile)
        }
    }
}

class ErrorDuringFileAnalyzeNotificationProvider(val project: Project) : EditorNotifications.Provider<EditorNotificationPanel>() {
    private class HighlightingErrorNotificationPanel : EditorNotificationPanel() {
        init {
            setText("Kotlin internal error occurred in this file. Highlighting may be inadequate.")
            myLabel.icon = AllIcons.General.Error
            createActionLabel("Open Event Log", ActivateToolWindowAction.getActionIdForToolWindow(EventLog.LOG_TOOL_WINDOW_ID))
        }
    }

    override fun getKey() = ERROR_HIGHLIGHT_PANEL_KEY

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor): EditorNotificationPanel? {
        if (file.fileType != KotlinFileType.INSTANCE) {
            return null
        }

        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return null

        if (psiFile !is KtFile) {
            return null
        }

        val hasErrors = psiFile.getUserData(HAS_ERRORS_IN_HIGHLIHTING_KEY) ?: false

        return if (hasErrors) HighlightingErrorNotificationPanel() else null
    }
}
