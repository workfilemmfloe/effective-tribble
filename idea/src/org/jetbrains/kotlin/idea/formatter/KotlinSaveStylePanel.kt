/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.ui.ListCellRendererWrapper
import javax.swing.JList
import javax.swing.JPanel

class KotlinSaveStylePanel(settings: CodeStyleSettings) : CodeStyleAbstractPanel(settings) {
    override fun getRightMargin() = throw UnsupportedOperationException()
    override fun createHighlighter(scheme: EditorColorsScheme) = throw UnsupportedOperationException()
    override fun getFileType() = throw UnsupportedOperationException()
    override fun getPreviewText(): String? = null

    override fun getTabTitle(): String = "Load/Save"

    private data class SaveItem(val label: String, val id: String?)

    private val saveDefaultsItems = listOf(
        SaveItem("<ide defaults>", null),
        SaveItem(KotlinObsoleteCodeStyle.CODE_STYLE_TITLE, KotlinObsoleteCodeStyle.CODE_STYLE_ID),
        SaveItem(KotlinStyleGuideCodeStyle.CODE_STYLE_TITLE, KotlinStyleGuideCodeStyle.CODE_STYLE_ID)
    )

    private var selectedId: String?
        get() {
            val (_, id) = saveDefaultsComboBox.selectedItem as SaveItem
            return id
        }
        set(value) {
            saveDefaultsComboBox.selectedItem = saveDefaultsItems.firstOrNull { (_, id) -> id == value } ?: saveDefaultsItems.first()
        }

    private val loadSaveCodeStyleForm = LoadSaveCodeStyleForm().apply {
        saveDefaultsItems.forEach {
            saveDefaultsComboBox.addItem(it)
        }

        saveDefaultsComboBox.setRenderer(object : ListCellRendererWrapper<SaveItem>() {
            override fun customize(list: JList<*>?, value: SaveItem, index: Int, selected: Boolean, hasFocus: Boolean) {
                setText(value.label)
            }
        })
    }

    private val saveDefaultsComboBox get() = loadSaveCodeStyleForm.saveDefaultsComboBox

    override fun apply(settings: CodeStyleSettings) {
        settings.kotlinCustomSettings.CODE_STYLE_DEFAULTS = selectedId
        settings.kotlinCommonSettings.CODE_STYLE_DEFAULTS = selectedId
        settings.kotlinCustomSettings.OVERRIDE_PROJECT_DEFAULTS = loadSaveCodeStyleForm.useDefaultObsoleteCodeCheckBox.isSelected
    }

    override fun isModified(settings: CodeStyleSettings): Boolean {
        return selectedId != settings.kotlinCustomSettings.CODE_STYLE_DEFAULTS ||
                selectedId != settings.kotlinCommonSettings.CODE_STYLE_DEFAULTS ||
                loadSaveCodeStyleForm.useDefaultObsoleteCodeCheckBox.isSelected != settings.kotlinCustomSettings.OVERRIDE_PROJECT_DEFAULTS
    }

    override fun getPanel(): JPanel = loadSaveCodeStyleForm.panel

    override fun resetImpl(settings: CodeStyleSettings) {
        selectedId = settings.kotlinCustomSettings.CODE_STYLE_DEFAULTS ?: settings.kotlinCommonSettings.CODE_STYLE_DEFAULTS
        loadSaveCodeStyleForm.useDefaultObsoleteCodeCheckBox.isSelected = settings.kotlinCustomSettings.OVERRIDE_PROJECT_DEFAULTS
    }

    override fun onSomethingChanged() {
        // There's no way settings are going to be changed from other tabs without calling resetImpl
    }
}
