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

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.actions.MethodSmartStepTarget
import com.intellij.debugger.actions.SmartStepTarget
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.util.PsiFormatUtil
import com.intellij.psi.util.PsiFormatUtilBase
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.debugger.stepping.KotlinLambdaSmartStepTarget
import org.jetbrains.kotlin.idea.debugger.stepping.KotlinSmartStepIntoHandler
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.JetWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.InTextDirectivesUtils

public abstract class AbstractSmartStepIntoTest : JetLightCodeInsightFixtureTestCase() {
    private val fixture: JavaCodeInsightTestFixture
        get() = myFixture

    protected fun doTest(path: String) {
        fixture.configureByFile(path)

        val offset = fixture.getCaretOffset()
        val line = fixture.getDocument(fixture.getFile()!!)!!.getLineNumber(offset)

        val position = MockSourcePosition(_file = fixture.getFile(), _line = line, _offset = offset, _editor = fixture.getEditor())

        val actual = KotlinSmartStepIntoHandler().findSmartStepTargets(position).map { it: SmartStepTarget -> it.getLabel() ?: "" }

        val expected = InTextDirectivesUtils.findListWithPrefixes(fixture.getFile()?.getText()!!.replace("\\,", "+++"), "// EXISTS: ")
                .map { it.replace("+++", ",") }

        for (actualTargetName in actual) {
            assert(actualTargetName in expected) {
                "Unexpected step into target was found: $actualTargetName\n${renderTableWithResults(expected, actual)}" +
                "\n // EXISTS: ${actual.joinToString()}"
            }
        }

        for (expectedTargetName in expected) {
            assert(expectedTargetName in actual) {
                "Missed step into target: $expectedTargetName\n${renderTableWithResults(expected, actual)}" +
                "\n // EXISTS: ${actual.joinToString()}"
            }
        }
    }

    private fun renderTableWithResults(expected: List<String>, actual: List<String>): String {
        val sb = StringBuilder()

        val maxExtStrSize = (expected.maxBy { it.length() }?.length() ?: 0) + 5
        val longerList = (if (expected.size() < actual.size()) actual else expected).sorted()
        val shorterList = (if (expected.size() < actual.size()) expected else actual).sorted()
        for ((i, element) in longerList.withIndex()) {
            sb.append(element)
            sb.append(" ".repeat(maxExtStrSize - element.length()))
            if (i < shorterList.size()) sb.append(shorterList[i])
            sb.append("\n")
        }

        return sb.toString()
    }

    override fun getTestDataPath(): String? {
        return PluginTestCaseBase.getTestDataPathBase() + "/debugger/smartStepInto"
    }

    override fun getProjectDescriptor() = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}
