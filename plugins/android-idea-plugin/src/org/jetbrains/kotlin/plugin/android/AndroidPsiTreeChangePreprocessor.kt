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

package org.jetbrains.kotlin.plugin.android

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import org.jetbrains.kotlin.lang.resolve.android.AndroidResourceManager

public class AndroidPsiTreeChangePreprocessor : PsiTreeChangePreprocessor, SimpleModificationTracker() {

    class object {
        private val HANDLED_EVENTS = setOf(
                PsiTreeChangeEventImpl.PsiEventType.CHILD_ADDED,
                PsiTreeChangeEventImpl.PsiEventType.CHILD_MOVED,
                PsiTreeChangeEventImpl.PsiEventType.CHILD_REMOVED,
                PsiTreeChangeEventImpl.PsiEventType.CHILD_REPLACED,
                PsiTreeChangeEventImpl.PsiEventType.CHILDREN_CHANGED)
    }

    override fun treeChanged(event: PsiTreeChangeEventImpl) {
        if (event.getCode() in HANDLED_EVENTS) {
            val file = event.getFile()
            if (file != null) {
                val project = file.getProject()

                val projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex()
                val module = projectFileIndex.getModuleForFile(file.getVirtualFile())
                if (module != null) {
                    val resourceManager = AndroidResourceManager.getInstance(module)
                    val mainResDirectory = resourceManager.getMainResDirectory()
                    val baseDirectory = file.getParent()?.getParent()?.getVirtualFile()

                    //File from res/ directory was modified
                    if (mainResDirectory == baseDirectory && file.isLayoutXmlFile()) {
                        incModificationCount()
                    }
                }
            }
        }
    }

    private fun PsiFile.isLayoutXmlFile(): Boolean {
        if (getFileType() != XmlFileType.INSTANCE) return false
        return getParent()?.getName()?.startsWith("layout") ?: false
    }

}