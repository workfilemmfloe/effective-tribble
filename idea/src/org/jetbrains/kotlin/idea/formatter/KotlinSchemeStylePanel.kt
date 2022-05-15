/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.psi.codeStyle.CodeStyleSettings
import javax.swing.JPanel

class KotlinSchemeStylePanel(settings: CodeStyleSettings) : CodeStyleAbstractPanel(settings) {
    var wasKotlinSet: Boolean = false

    override fun getRightMargin() = throw UnsupportedOperationException()
    override fun createHighlighter(scheme: EditorColorsScheme) = throw UnsupportedOperationException()
    override fun getFileType() = throw UnsupportedOperationException()
    override fun getPreviewText(): String? = null

    override fun apply(settings: CodeStyleSettings) {
        settings.kotlinCustomSettings.KOTLIN_OFFICIAL_CODE_STYLE = wasKotlinSet
    }

    override fun isModified(settings: CodeStyleSettings): Boolean {
        return wasKotlinSet != settings.kotlinCustomSettings.KOTLIN_OFFICIAL_CODE_STYLE
    }

    override fun getPanel() = JPanel()

    override fun resetImpl(settings: CodeStyleSettings) {
        wasKotlinSet = settings.kotlinCustomSettings.KOTLIN_OFFICIAL_CODE_STYLE
    }

    override fun onSomethingChanged() {
        // Do not update anything, as there're no controls
    }
}
