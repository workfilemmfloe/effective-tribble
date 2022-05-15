/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.ui.notifications

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.search.ProjectScope
import com.intellij.util.ThreeState
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.formatter.*
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.isDefaultOfficialCodeStyle

private const val KOTLIN_UPDATE_CODE_STYLE_GROUP_ID = "Update Kotlin code style"
private const val KOTLIN_UPDATE_CODE_STYLE_PROPERTY_NAME = "update.kotlin.code.style.notified"

fun notifyKotlinStyleUpdateIfNeeded(project: Project) {
    if (!isDefaultOfficialCodeStyle) return

    @Suppress("DEPRECATION") // Suggested fix is absent in 173. BUNCH: 181
    val isProjectSettings = CodeStyleSettingsManager.getInstance(project).USE_PER_PROJECT_SETTINGS
    val settingsComponent: PropertiesComponent = if (isProjectSettings) {
        PropertiesComponent.getInstance(project)
    } else {
        PropertiesComponent.getInstance()
    }

    if (settingsComponent.getBoolean(KOTLIN_UPDATE_CODE_STYLE_PROPERTY_NAME, false)) {
        return
    }

    val notification = KotlinCodeStyleChangedNotification.create(project, isProjectSettings) ?: return
    notification.isImportant = true

    NotificationsConfiguration.getNotificationsConfiguration()
        .register(KOTLIN_UPDATE_CODE_STYLE_GROUP_ID, NotificationDisplayType.STICKY_BALLOON, true)

    if (ApplicationManager.getApplication().isUnitTestMode) {
        return
    }

    settingsComponent.setValue(KOTLIN_UPDATE_CODE_STYLE_PROPERTY_NAME, true, false)

    notification.notify(project)
}

class KotlinCodeStyleChangedNotification(val project: Project, isProjectSettings: Boolean) : Notification(
    KOTLIN_UPDATE_CODE_STYLE_GROUP_ID,
    "Kotlin Code Style",
    """
        <html>
        Default code style was updated to Kotlin Coding Conventions.
        </html>
        """.trimIndent(),
    NotificationType.WARNING,
    null
) {
    init {
        val ktFormattingSettings = ktCodeStyleSettings(project)

        if (isProjectSettings) {
            addAction(object : NotificationAction("Apply new code style") {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    notification.expire()

                    val ktSettings = ktCodeStyleSettings(project) ?: return

                    runWriteAction {
                        KotlinStyleGuideCodeStyle.apply(ktSettings.all)
                    }
                }
            })
        }

        if (ktFormattingSettings != null && ktFormattingSettings.canRestore()) {
            addAction(object : NotificationAction("Restore old settings") {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    notification.expire()

                    val ktSettings = ktCodeStyleSettings(project) ?: return

                    runWriteAction {
                        ktSettings.restore()
                    }
                }
            })
        }
    }

    companion object {
        val LOG = Logger.getInstance("KotlinCodeStyleChangedNotification")

        fun create(project: Project, isProjectSettings: Boolean): KotlinCodeStyleChangedNotification? {
            val ktFormattingSettings = ktCodeStyleSettings(project) ?: return null

            if (isProjectSettings && !ktFormattingSettings.hasDefaultLoadScheme()) {
                return null
            }

            if (findRecentFileWithOldCodeStyle(project) == null) {
                // User doesn't have recent Kotlin files, or all files are formatted with new code style
                return null
            }

            return KotlinCodeStyleChangedNotification(project, isProjectSettings)
        }

        fun findRecentFileWithOldCodeStyle(project: Project): PsiFile? {
            val (isOldStyleDetected, oldStyleFiles) = detectIsOldCodeStyleFromRecentFiles(
                project
            )
            if (isOldStyleDetected != ThreeState.YES) {
                return null
            }

            return oldStyleFiles.firstOrNull()
        }

        private data class OldCodeStyleDetectionResult(
            val result: ThreeState,
            val oldStyleFiles: Collection<PsiFile>
        ) {
            companion object {
                val UNSURE =
                    OldCodeStyleDetectionResult(
                        ThreeState.UNSURE,
                        emptyList()
                    )
            }
        }

        private fun detectIsOldCodeStyleFromRecentFiles(project: Project): OldCodeStyleDetectionResult {
            val scope = KotlinSourceFilterScope.projectSources(ProjectScope.getContentScope(project), project)
            val psiManager = PsiManager.getInstance(project)

            val recentKotlinFiles = EditorHistoryManager.getInstance(project).fileList
                .filter { it.fileType == KotlinFileType.INSTANCE }
                .filter { it in scope }
                .mapNotNull { psiManager.findFile(it) }

            if (recentKotlinFiles.isEmpty()) {
                return OldCodeStyleDetectionResult.UNSURE
            }

            val indentWhitespace =
                indentWhitespace(
                    project
                )
            if (indentWhitespace == null || indentWhitespace.isEmpty()) {
                return OldCodeStyleDetectionResult.UNSURE
            }

            val detectToFiles: Map<ThreeState, List<PsiFile>> = recentKotlinFiles
                .groupBy { file ->
                    detectOldCodeStyle(
                        file,
                        indentWhitespace
                    )
                }

            val oldStyleFiles = detectToFiles[ThreeState.YES] ?: emptyList()
            val newStyleFiles = detectToFiles[ThreeState.NO] ?: emptyList()

            return when {
                !oldStyleFiles.isEmpty() && !newStyleFiles.isEmpty() ->
                    OldCodeStyleDetectionResult.UNSURE

                !oldStyleFiles.isEmpty() ->
                    OldCodeStyleDetectionResult(
                        ThreeState.YES,
                        oldStyleFiles
                    )

                else ->
                    OldCodeStyleDetectionResult(
                        ThreeState.NO,
                        emptyList()
                    )
            }
        }

        private fun indentWhitespace(project: Project): String? {
            val indentOptions = ktCodeStyleSettings(project)?.common?.indentOptions ?: return null
            return if (indentOptions.USE_TAB_CHARACTER) "\t" else " ".repeat(indentOptions.INDENT_SIZE)
        }

        private fun detectOldCodeStyle(ktFile: PsiFile, indentWhitespace: String): ThreeState {
            val doubleIndent = indentWhitespace + indentWhitespace

            var probablyOldStyleScore = 0
            var probablyNewCodeStyle = 0

            val text = runReadAction { ktFile.text } ?: return ThreeState.UNSURE

            val lines = text.lines()
            if (lines.size <= 1) return ThreeState.UNSURE

            for (i in 1..(lines.size - 1)) {
                val previous = lines[i - 1].trimEnd()
                val next = lines[i].trimEnd()

                if (previous.endsWith("(") || previous.endsWith("?:") || previous.endsWith("=") ||
                    next.startsWith(".") || next.startsWith("?.")
                ) {
                    val previousIndent = previous.takeWhile { it.isWhitespace() }
                    val nextIndent = next.takeWhile { it.isWhitespace() }

                    val indent = if (previousIndent.isEmpty()) nextIndent else nextIndent.substringAfter(previousIndent, "")
                    if (!indent.isEmpty()) {
                        if (indent == doubleIndent) {
                            probablyOldStyleScore++
                        } else if (indent == indentWhitespace) {
                            probablyNewCodeStyle++
                        }
                    }
                }
            }

            return when {
                probablyOldStyleScore > probablyNewCodeStyle -> ThreeState.YES
                probablyOldStyleScore < probablyNewCodeStyle -> ThreeState.NO
                else -> ThreeState.UNSURE
            }
        }
    }
}