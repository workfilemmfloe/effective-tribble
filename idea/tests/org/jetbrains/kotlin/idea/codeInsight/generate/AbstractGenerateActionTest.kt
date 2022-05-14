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

package org.jetbrains.kotlin.idea.codeInsight.generate

import com.intellij.codeInsight.actions.CodeInsightAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.testFramework.PlatformTestUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.JetWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.JetTestUtils
import java.io.File

abstract class AbstractGenerateActionTest : JetLightCodeInsightFixtureTestCase() {
    protected open fun doTest(path: String) {
        val fileText = FileUtil.loadFile(File(path), true)

        val conflictFile = File("$path.messages")

        try {
            ConfigLibraryUtil.configureLibrariesByDirective(myModule, PlatformTestUtil.getCommunityPath(), fileText)

            val mainFile = File(path)
            val mainFileName = mainFile.name
            val fileNameBase = mainFile.nameWithoutExtension
            val rootDir = mainFile.parentFile
            rootDir
                    .list { file, name ->
                        name.startsWith(fileNameBase) && name != mainFileName && (name.endsWith(".kt") || name.endsWith(".java"))
                    }
                    .forEach {
                        myFixture.configureByFile(File(rootDir, it).path.replace(File.separator, "/"))
                    }
            myFixture.configureByFile(path)

            val actionClassName = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// ACTION_CLASS: ")
            val action = Class.forName(actionClassName).newInstance() as CodeInsightAction

            val isApplicableExpected = !InTextDirectivesUtils.isDirectiveDefined(fileText, "// NOT_APPLICABLE")

            val presentation = myFixture.testAction(action)
            TestCase.assertEquals(isApplicableExpected, presentation.isEnabled)

            assert(!conflictFile.exists()) { "Conflict file $conflictFile should not exist" }

            if (isApplicableExpected) {
                val afterFile = File("$path.after")
                TestCase.assertTrue(afterFile.exists())
                myFixture.checkResult(FileUtil.loadFile(afterFile, true))
            }
        }
        catch (e: CommonRefactoringUtil.RefactoringErrorHintException) {
            JetTestUtils.assertEqualsToFile(conflictFile, e.getMessage()!!)
        }
        finally {
            ConfigLibraryUtil.unconfigureLibrariesByDirective(myModule, fileText)
        }
    }

    override fun getProjectDescriptor() = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}
