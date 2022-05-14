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

package org.jetbrains.jet.resolve

import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.JetTestCaseBuilder
import org.jetbrains.jet.plugin.JetWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import java.util.HashSet
import org.jetbrains.jet.JetTestUtils
import java.io.File
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.junit.Assert
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.renderer.DescriptorRenderer
import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.plugin.caches.resolve.getResolutionFacade
import org.jetbrains.jet.lang.psi.psiUtil.parents
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import org.jetbrains.jet.lang.psi.JetPsiFactory
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.JetReferenceExpression
import org.jetbrains.jet.lang.psi.psiUtil.getReceiverExpression

public abstract class AbstractPartialBodyResolveTest : JetLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath() = JetTestCaseBuilder.getHomeDirectory()
    override fun getProjectDescriptor() = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    public fun doTest(testPath: String) {
        myFixture.configureByFile(testPath)

        val file = myFixture.getFile() as JetFile
        val editor = myFixture.getEditor()
        val selectionModel = editor.getSelectionModel()
        val expression = if (selectionModel.hasSelection()) {
            PsiTreeUtil.findElementOfClassAtRange(file, selectionModel.getSelectionStart(), selectionModel.getSelectionEnd(), javaClass<JetExpression>())
                ?: error("No JetExpression at selection range")
        }
        else {
            val offset = editor.getCaretModel().getOffset()
            val element = file.findElementAt(offset)
            element.getParentByType(javaClass<JetSimpleNameExpression>()) ?: error("No JetSimpleNameExpression at caret")
        }

        val resolutionFacade = file.getResolutionFacade()

        // optimized resolve
        val (target1, type1, processedStatements1) = doResolve(expression, resolutionFacade.analyzeWithPartialBodyResolve(expression))

        // full body resolve
        val (target2, type2, processedStatements2) = doResolve(expression, resolutionFacade.analyze(expression))

        val set = HashSet(processedStatements2)
        assert (set.containsAll(processedStatements1))
        set.removeAll(processedStatements1)

        val builder = StringBuilder()

        if (expression is JetReferenceExpression) {
            builder.append("Resolve target: ${target2.presentation(type2)}\n")
        }
        else {
            builder.append("Expression type:${type2.presentation()}\n")
        }
        builder.append("----------------------------------------------\n")

        val skippedStatements = set
                .filter { !it.parents(withItself = false).any { it in set } } // do not include skipped statements which are inside other skipped statement
                .sortBy { it.getTextOffset() }

        CommandProcessor.getInstance().executeCommand(
                {
                    ApplicationManager.getApplication().runWriteAction {
                        for (statement in skippedStatements) {
                            statement.replace(JetPsiFactory(getProject()).createComment("/* STATEMENT DELETED: ${statement.compactPresentation()} */"))
                        }
                    }
                },
                "",
                null
        )
        val fileText = file.getText()
        if (selectionModel.hasSelection()) {
            val start = selectionModel.getSelectionStart()
            val end = selectionModel.getSelectionEnd()
            builder.append(fileText.substring(0, start))
            builder.append("<selection>")
            builder.append(fileText.substring(start, end))
            builder.append("<selection>")
            builder.append(fileText.substring(end))
        }
        else {
            val newCaretOffset = editor.getCaretModel().getOffset()
            builder.append(fileText.substring(0, newCaretOffset))
            builder.append("<caret>")
            builder.append(fileText.substring(newCaretOffset))
        }

        JetTestUtils.assertEqualsToFile(File(testPath.substringBeforeLast('.') + ".dump"), builder.toString())

        Assert.assertEquals(target2.presentation(null), target1.presentation(null))
        Assert.assertEquals(type2.presentation(), type1.presentation())
    }

    private data class ResolveData(
            val target: DeclarationDescriptor?,
            val type: JetType?,
            val processedStatements: Collection<JetExpression>
    )

    private fun doResolve(expression: JetExpression, bindingContext: BindingContext): ResolveData {
        val target = if (expression is JetReferenceExpression) bindingContext[BindingContext.REFERENCE_TARGET, expression] else null

        val processedStatements = bindingContext.getSliceContents(BindingContext.PROCESSED)
                .filter { it.value }
                .map { it.key }
                .filter { it.getParent() is JetBlockExpression }

        val receiver = (expression as? JetSimpleNameExpression)?.getReceiverExpression()
        val expressionWithType = if (receiver != null) {
            expression.getParent() as? JetExpression ?: expression
        }
        else {
            expression
        }
        val type = bindingContext[BindingContext.EXPRESSION_TYPE, expressionWithType]

        return ResolveData(target, type, processedStatements)
    }

    private fun DeclarationDescriptor?.presentation(type: JetType?): String {
        if (this == null) return "null"

        val s = DescriptorRenderer.COMPACT.render(this)

        val renderType = this is VariableDescriptor && type != this.getReturnType()
        if (!renderType) return s
        return "$s smart-cast to ${type.presentation()}"
    }

    private fun JetType?.presentation()
            = if (this != null) DescriptorRenderer.COMPACT.renderType(this) else "unknown type"

    private fun JetExpression.compactPresentation(): String {
        val text = getText()
        val builder = StringBuilder()
        var dropSpace = false
        for (c in text) {
            when (c) {
                ' ', '\n', '\r' -> {
                    if (!dropSpace) builder.append(' ')
                    dropSpace = true
                }

                else -> {
                    builder.append(c)
                    dropSpace = false
                }
            }
        }
        return builder.toString()
    }
}