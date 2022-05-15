/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.project.Project

class ProjectEditorLiveList(val project: Project) : EditorFactoryListener {
    private val myEditorSet = HashSet<Editor>()

    init {
        val editorFactory = EditorFactory.getInstance()
        editorFactory.addEditorFactoryListener(this, project)
        editorFactory.allEditors.forEach {
            if (editorFilter(it)) {
                myEditorSet.add(it)
            }
        }

        // TODO: remove when project closed
    }

    private fun editorFilter(editor: Editor): Boolean {
        return editor.project === project
    }

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        if (editorFilter(editor)) {
            myEditorSet.add(editor)
        }
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        myEditorSet.remove(event.editor)
    }
}

