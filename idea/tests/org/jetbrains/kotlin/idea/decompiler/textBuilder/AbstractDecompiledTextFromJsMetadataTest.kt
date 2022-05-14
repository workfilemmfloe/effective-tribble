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

package org.jetbrains.kotlin.idea.decompiler.textBuilder

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.decompiler.KotlinJavascriptMetaFile
import org.jetbrains.kotlin.idea.js.KotlinJavaScriptLibraryManager
import org.jetbrains.kotlin.idea.test.ModuleKind
import org.jetbrains.kotlin.idea.test.configureAs
import org.jetbrains.kotlin.idea.vfilefinder.JsVirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import kotlin.test.assertTrue

public abstract class AbstractDecompiledTextFromJsMetadataTest : AbstractDecompiledTextBaseTest(true) {

    protected override fun getFileToDecompile(): VirtualFile {
        val className = getTestName(false)
        val virtualFileFinder = JsVirtualFileFinder.SERVICE.getInstance(getProject())
        val classId = ClassId(FqName(TEST_PACKAGE), FqName(className), false)
        return virtualFileFinder.findVirtualFileWithHeader(classId)!!
    }

    protected override fun checkPsiFile(psiFile: PsiFile) =
            assertTrue(psiFile is KotlinJavascriptMetaFile, "Expecting decompiled kotlin javascript file, was: " + psiFile.javaClass)

    override fun setUp() {
        super.setUp()
        myModule!!.configureAs(ModuleKind.KOTLIN_JAVASCRIPT)
        KotlinJavaScriptLibraryManager.getInstance(getProject()).syncUpdateProjectLibrary()
    }
}
