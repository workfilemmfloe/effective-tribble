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

package org.jetbrains.kotlin.idea.test

import com.intellij.openapi.editor.Document
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.psi.PsiDocumentManager
import com.intellij.refactoring.MultiFileTestCase
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.KotlinTestUtils

public abstract class KotlinMultiFileTestCase : MultiFileTestCase() {
    protected fun extractCaretOffset(doc: Document): Int {
        val offset = runWriteAction {
            val text = StringBuilder(doc.getText())
            val offset = text.indexOf("<caret>")

            if (offset >= 0) {
                text.delete(offset, offset + "<caret>".length)
                doc.setText(text.toString())
            }

            offset
        }

        PsiDocumentManager.getInstance(myProject).commitAllDocuments()
        PsiDocumentManager.getInstance(myProject).doPostponedOperationsAndUnblockDocument(doc)

        return offset
    }

    override fun setUp() {
        super.setUp()
        VfsRootAccess.allowRootAccess(KotlinTestUtils.getHomeDirectory())
    }

    override fun tearDown() {
        VfsRootAccess.disallowRootAccess(KotlinTestUtils.getHomeDirectory())
        super.tearDown()
    }
}