/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.refactoring.introduce.introduceVariable

import com.intellij.ide.DataManager
import com.intellij.psi.PsiElement
import org.jetbrains.jet.JetTestCaseBuilder
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.plugin.refactoring.extractFunction.ExtractKotlinFunctionHandler
import java.io.File
import org.jetbrains.jet.plugin.refactoring.extractFunction.selectElements
import org.jetbrains.jet.lang.psi.JetTreeVisitorVoid
import com.intellij.psi.PsiComment
import com.intellij.refactoring.BaseRefactoringProcessor.ConflictsInTestsException
import org.jetbrains.jet.JetTestUtils
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import org.jetbrains.jet.InTextDirectivesUtils
import org.jetbrains.jet.renderer.DescriptorRenderer
import kotlin.test.assertEquals
import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.jet.plugin.refactoring.extractFunction.ExtractKotlinFunctionHandlerHelper
import org.jetbrains.jet.plugin.refactoring.extractFunction.ExtractionGeneratorOptions
import org.jetbrains.jet.plugin.refactoring.extractFunction.ExtractableCodeDescriptor
import org.jetbrains.jet.testing.ConfigLibraryUtil
import org.jetbrains.jet.plugin.PluginTestCaseBase
import org.jetbrains.jet.plugin.refactoring.extractFunction.ExtractionData
import org.jetbrains.jet.plugin.refactoring.extractFunction.ExtractionOptions
import org.jetbrains.jet.lang.psi.JetDeclaration
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.jet.lang.psi.JetPackageDirective
import org.jetbrains.jet.utils.emptyOrSingletonList

public abstract class AbstractJetExtractionTest() : JetLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = LightCodeInsightFixtureTestCase.JAVA_LATEST

    val fixture: JavaCodeInsightTestFixture get() = myFixture

    protected fun doIntroduceVariableTest(path: String) {
        doTest(path) { file ->
            KotlinIntroduceVariableHandler().invoke(
                    fixture.getProject(),
                    fixture.getEditor(),
                    file,
                    DataManager.getInstance().getDataContext(fixture.getEditor().getComponent())
            )
        }
    }

    protected fun doExtractFunctionTest(path: String) {
        doTest(path) { file ->
            var explicitPreviousSibling: PsiElement? = null
            file.accept(
                    object: JetTreeVisitorVoid() {
                        override fun visitComment(comment: PsiComment) {
                            if (comment.getText() == "// SIBLING:") {
                                val parent = comment.getParent()
                                if (parent is JetDeclaration) {
                                    explicitPreviousSibling = parent
                                }
                                else {
                                    explicitPreviousSibling = PsiTreeUtil.skipSiblingsForward(
                                            comment,
                                            javaClass<PsiWhiteSpace>(),
                                            javaClass<PsiComment>(),
                                            javaClass<JetPackageDirective>()
                                    )
                                }
                            }
                        }
                    }
            )

            val fileText = file.getText() ?: ""
            val expectedDescriptors =
                    InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "// PARAM_DESCRIPTOR: ").joinToString()
            val expectedTypes =
                    InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "// PARAM_TYPES: ").map { "[$it]" }.joinToString()
            val extractAsProperty = InTextDirectivesUtils.isDirectiveDefined(fileText, "// EXTRACT_AS_PROPERTY")

            val extractionOptions = InTextDirectivesUtils.findListWithPrefixes(fileText, "// OPTIONS: ").let {
                if (it.isNotEmpty()) {
                    [suppress("CAST_NEVER_SUCCEEDS")]
                    val args = it.map { it.toBoolean() }.copyToArray() as Array<Any?>
                    javaClass<ExtractionOptions>().getConstructors()[0].newInstance(*args) as ExtractionOptions
                } else ExtractionOptions.DEFAULT
            }

            val renderer = DescriptorRenderer.DEBUG_TEXT

            val editor = fixture.getEditor()
            selectElements(editor, file) {(elements, previousSibling) ->
                ExtractKotlinFunctionHandler(
                        helper = object : ExtractKotlinFunctionHandlerHelper() {
                            override fun adjustExtractionData(data: ExtractionData): ExtractionData {
                                return data.copy(options = extractionOptions)
                            }

                            override fun adjustGeneratorOptions(options: ExtractionGeneratorOptions): ExtractionGeneratorOptions {
                                return options.copy(extractAsProperty = extractAsProperty)
                            }

                            override fun adjustDescriptor(descriptor: ExtractableCodeDescriptor): ExtractableCodeDescriptor {
                                val allParameters = emptyOrSingletonList(descriptor.receiverParameter) + descriptor.parameters
                                val actualDescriptors = allParameters.map { renderer.render(it.originalDescriptor) }.joinToString()
                                val actualTypes = allParameters.map {
                                    it.parameterTypeCandidates.map { renderer.renderType(it) }.joinToString(", ", "[", "]")
                                }.joinToString()

                                assertEquals(expectedDescriptors, actualDescriptors, "Expected descriptors mismatch.")
                                assertEquals(expectedTypes, actualTypes, "Expected types mismatch.")

                                return descriptor
                            }
                        }
                ).doInvoke(editor, file, elements, explicitPreviousSibling ?: previousSibling)
            }
        }
    }

    protected fun doTest(path: String, action: (JetFile) -> Unit) {
        val mainFile = File(path)
        val afterFile = File("$path.after")
        val conflictFile = File("$path.conflicts")

        fixture.setTestDataPath("${JetTestCaseBuilder.getHomeDirectory()}/${mainFile.getParent()}")

        val file = fixture.configureByFile(mainFile.getName()) as JetFile

        if (InTextDirectivesUtils.findStringWithPrefixes(file.getText(), "// WITH_RUNTIME") != null) {
            ConfigLibraryUtil.configureKotlinRuntime(myModule, PluginTestCaseBase.fullJdk())
        }

        try {
            action(file)

            assert(!conflictFile.exists())
            JetTestUtils.assertEqualsToFile(afterFile, file.getText()!!)
        }
        catch(e: Exception) {
            val message = if (e is ConflictsInTestsException) e.getMessages().sort().joinToString(" ") else e.getMessage()
            JetTestUtils.assertEqualsToFile(conflictFile, message?.replace("\n", " ") ?: e.javaClass.getName())
        }
    }
}
