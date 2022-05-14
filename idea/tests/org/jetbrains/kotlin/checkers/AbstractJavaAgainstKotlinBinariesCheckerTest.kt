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

package org.jetbrains.kotlin.checkers

import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.util.io.FileUtilRt
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.MockLibraryUtil

abstract class AbstractJavaAgainstKotlinBinariesCheckerTest : AbstractJavaAgainstKotlinCheckerTest() {
    override fun setUp() {
        super.setUp()
        val testName = getTestName(false)
        if (testName.startsWith("AllFilesPresentIn")) {
            return
        }
        val libraryName = "libFor" + testName
        val libraryJar = MockLibraryUtil.compileLibraryToJar(
                PluginTestCaseBase.getTestDataPathBase() + "/kotlinAndJavaChecker/javaAgainstKotlin/" + getTestName(false) + ".kt",
                libraryName, false, false
        )
        val jarUrl = "jar://" + FileUtilRt.toSystemIndependentName(libraryJar.absolutePath) + "!/"
        ModuleRootModificationUtil.addModuleLibrary(module, jarUrl)
    }

    fun doTest(path: String) {
        doTest(true, true, path.replace(".kt", ".java"))
    }
}