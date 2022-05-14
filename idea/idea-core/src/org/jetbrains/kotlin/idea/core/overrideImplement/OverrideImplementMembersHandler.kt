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

package org.jetbrains.kotlin.idea.core.overrideImplement

import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.util.MemberChooser
import com.intellij.lang.LanguageCodeInsightActionHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.quickfix.generateMembers
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

public abstract class OverrideImplementMembersHandler : LanguageCodeInsightActionHandler {

    public fun collectMembersToGenerate(classOrObject: JetClassOrObject): Collection<OverrideMemberChooserObject> {
        val descriptor = classOrObject.resolveToDescriptor() as? ClassDescriptor ?: return emptySet()
        return collectMembersToGenerate(descriptor, classOrObject.project)
    }

    protected abstract fun collectMembersToGenerate(descriptor: ClassDescriptor, project: Project): Collection<OverrideMemberChooserObject>

    private fun showOverrideImplementChooser(project: Project, members: Array<OverrideMemberChooserObject>): MemberChooser<OverrideMemberChooserObject>? {
        val chooser = MemberChooser(members, true, true, project)
        chooser.title = getChooserTitle()
        chooser.show()
        if (chooser.exitCode != DialogWrapper.OK_EXIT_CODE) return null
        return chooser
    }

    protected abstract fun getChooserTitle(): String

    override fun isValidFor(editor: Editor, file: PsiFile): Boolean {
        if (file !is JetFile) return false
        val elementAtCaret = file.findElementAt(editor.caretModel.offset)
        val classOrObject = elementAtCaret?.getNonStrictParentOfType<JetClassOrObject>()
        return classOrObject != null
    }

    protected abstract fun getNoMembersFoundHint(): String

    public fun invoke(project: Project, editor: Editor, file: PsiFile, implementAll: Boolean) {
        val elementAtCaret = file.findElementAt(editor.caretModel.offset)
        val classOrObject = elementAtCaret?.getNonStrictParentOfType<JetClassOrObject>()!!

        val members = collectMembersToGenerate(classOrObject)
        if (members.isEmpty() && !implementAll) {
            HintManager.getInstance().showErrorHint(editor, getNoMembersFoundHint())
            return
        }

        val selectedElements = if (implementAll) {
            members
        }
        else {
            val chooser = showOverrideImplementChooser(project, members.toTypedArray()) ?: return
            chooser.selectedElements ?: return
        }
        if (selectedElements.isEmpty()) return

        PsiDocumentManager.getInstance(project).commitAllDocuments()

        generateMembers(editor, classOrObject, selectedElements)
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) = invoke(project, editor, file, false)

    override fun startInWriteAction(): Boolean = false

    companion object {
        public fun generateMembers(editor: Editor, classOrObject: JetClassOrObject, selectedElements: Collection<OverrideMemberChooserObject>) {
            val project = classOrObject.project
            generateMembers(editor, classOrObject, selectedElements.map { chooser -> { chooser.generateMember(project) } })
        }
    }
}
