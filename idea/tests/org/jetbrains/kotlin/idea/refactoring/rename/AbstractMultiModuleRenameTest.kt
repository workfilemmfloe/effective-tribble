/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.jsonUtils.getString
import org.jetbrains.kotlin.idea.test.KotlinMultiFileTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import java.io.File

abstract class AbstractMultiModuleRenameTest : KotlinMultiFileTestCase() {
    override fun getTestRoot(): String = "/refactoring/renameMultiModule/"
    override fun getTestDataPath(): String = PluginTestCaseBase.getTestDataPathBase()

    fun doTest(path: String) {
        val renameParamsObject = loadTestConfiguration(File(path))

        val file = renameParamsObject.getString("file")
        val newName = renameParamsObject.getString("newName")

        isMultiModule = true

        doTestCommittingDocuments { rootDir, _ ->
            val mainFile = rootDir.findFileByRelativePath(file)!!
            val psiFile = PsiManager.getInstance(project).findFile(mainFile)

            runRenameProcessor(project, newName, psiFile, renameParamsObject, true, true)
        }
    }

    protected fun doTestCommittingDocuments(action : (VirtualFile, VirtualFile?) -> Unit) {
        super.doTest({ rootDir, rootAfter ->
            action(rootDir, rootAfter)

            PsiDocumentManager.getInstance(project!!).commitAllDocuments()
            FileDocumentManager.getInstance().saveAllDocuments()
        }, getTestDirName(true))
    }

    protected fun getTestDirName(lowercaseFirstLetter : Boolean) : String {
        val testName = getTestName(lowercaseFirstLetter)
        return testName.substring(0, testName.indexOf('_'))
    }
}
