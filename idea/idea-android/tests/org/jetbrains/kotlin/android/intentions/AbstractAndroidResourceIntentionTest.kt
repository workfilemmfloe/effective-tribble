/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android.intentions

import com.android.SdkConstants
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.PathUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.android.KotlinAndroidTestCase
import org.jetbrains.kotlin.idea.jsonUtils.getString
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils
import org.jetbrains.kotlin.psi.KtFile
import java.io.File


abstract class AbstractAndroidResourceIntentionTest : KotlinAndroidTestCase() {

    fun doTest(path: String) {
        val configFile = File(path)
        val testDataPath = configFile.parent

        myFixture.testDataPath = testDataPath

        val config = JsonParser().parse(FileUtil.loadFile(File(path), true)) as JsonObject

        val intentionClass = config.getString("intentionClass")
        val isApplicableExpected = if (config.has("isApplicable")) config.get("isApplicable").asBoolean else true
        val rFile = if (config.has("rFile")) config.get("rFile").asString else null
        val resDirectory = if (config.has("resDirectory")) config.get("resDirectory").asString else null

        if (rFile != null) {
            myFixture.copyFileToProject(rFile, "gen/" + PathUtil.getFileName(rFile))
        }
        else {
            if (File(testDataPath + "/R.java").isFile) {
                myFixture.copyFileToProject("R.java", "gen/R.java")
            }
        }

        if (resDirectory != null) {
            myFixture.copyDirectoryToProject(resDirectory, "res")
        }
        else {
            if (File(testDataPath + "/res").isDirectory) {
                myFixture.copyDirectoryToProject("res", "res")
            }
        }

        val sourceFile = myFixture.copyFileToProject("main.kt", "src/main.kt")
        myFixture.configureFromExistingVirtualFile(sourceFile)

        DirectiveBasedActionUtils.checkForUnexpectedErrors(myFixture.file as KtFile)

        val intentionAction = Class.forName(intentionClass).newInstance() as IntentionAction

        TestCase.assertEquals(isApplicableExpected, intentionAction.isAvailable(myFixture.project, myFixture.editor, myFixture.file))
        if (!isApplicableExpected) {
            return
        }

        val element = getTargetElement()
        element?.putUserData(CREATE_XML_RESOURCE_PARAMETERS_NAME_KEY, "resource_id")

        myFixture.launchAction(intentionAction)

        FileDocumentManager.getInstance().saveAllDocuments()
        DirectiveBasedActionUtils.checkForUnexpectedErrors(myFixture.file as KtFile)

        myFixture.checkResultByFile("/expected/main.kt")
        assertResourcesEqual(testDataPath + "/expected/res")
    }

    fun assertResourcesEqual(expectedPath: String) {
        PlatformTestUtil.assertDirectoriesEqual(LocalFileSystem.getInstance().findFileByPath(expectedPath), getResourceDirectory())
    }

    fun getResourceDirectory() = LocalFileSystem.getInstance().findFileByPath(myFixture.tempDirPath + "/res")

    fun getTargetElement() = myFixture.file.findElementAt(myFixture.caretOffset)?.parent

    override fun createManifest() {
        myFixture.copyFileToProject("idea/testData/android/AndroidManifest.xml", SdkConstants.FN_ANDROID_MANIFEST_XML)
    }
}