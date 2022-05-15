/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import com.google.common.collect.Lists
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LineExtensionInfo
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.TextRange
import com.intellij.rt.execution.junit.FileComparisonFailure
import com.intellij.testFramework.VfsTestUtil
import com.intellij.testFramework.utils.inlays.InlayHintsChecker
import com.intellij.testFramework.utils.inlays.InlayInfo
import junit.framework.ComparisonFailure
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.TagsTestDataUtil
import org.junit.Assert

class LambdaReturnValueHintsTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): KotlinLightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    private class LineExtensionInfoTag(offset: Int, data: LineExtensionInfo) :
        TagsTestDataUtil.TagInfo<LineExtensionInfo>(offset, data, true) {

        override fun getName() = "hint"
        override fun getAttributesString(): String = "text=\"${data.text}\""
    }

    private class CaretTag(editor: Editor) :
        TagsTestDataUtil.TagInfo<Any>(
            editor.caretModel.currentCaret.offset, "caret",
            /*isStart = */true, /*isClosed = */false, /*isFixed = */true
        )


    private fun collectActualLineExtensionsTags(): List<LineExtensionInfoTag> {
        val tags = ArrayList<LineExtensionInfoTag>()
        val lineCount = myFixture.editor.document.lineCount
        for (i in 0 until lineCount) {
            val lineEndOffset = myFixture.editor.document.getLineEndOffset(i)

            (myFixture.editor as EditorImpl).processLineExtensions(i) { lineExtensionInfo ->
                tags.add(LineExtensionInfoTag(lineEndOffset, lineExtensionInfo))
                true
            }
        }

        return tags
    }

    fun check(text: String) {
        myFixture.configureByText("A.kt", text.trimIndent())

        // Clean test file from the hints tags
        InlayHintsChecker(myFixture).extractInlaysAndCaretInfo(editor.document)

        myFixture.doHighlighting()

        val expectedText = run {
            val tags = if (editor.caretModel.offset > 0) listOf(CaretTag(editor)) else emptyList()
            TagsTestDataUtil.insertTagsInText(tags, editor.document.text)
        }

        Assert.assertTrue(
            "No other inlays should be present in the file",
            editor.inlayModel.getInlineElementsInRange(0, editor.document.textLength).isEmpty()
        )

        val actualText = run {
            val tags = ArrayList<TagsTestDataUtil.TagInfo<*>>()

            tags.addAll(collectActualLineExtensionsTags())

            if (editor.caretModel.offset > 0) {
                tags.add(CaretTag(editor))
            }

            TagsTestDataUtil.insertTagsInText(tags, editor.document.text)
        }

        Assert.assertEquals(expectedText, actualText)
    }

    fun testSimple() {
        check(
            """
            val x = run {
                println("foo")
                1<caret><hint text=" ^run"/>
            }
            """
        )
    }

    fun testQualified() {
        check(
            """
            val x = run {
                var s = "abc"
                s.length<caret><hint text=" ^run"/>
            }
            """
        )
    }

    fun testIf() {
        check(
            """
            val x = run {
                if (true) {
                    1<hint text=" ^run"/>
                } else {
                    0<hint text=" ^run"/>
                }
            }
            """
        )
    }

    fun testOneLineIf() {
        check(
            """
            val x = run {
                println(1)
                if (true) 1 else { 0 }<caret><hint text=" ^run"/>
            }
            """
        )
    }

    fun testWhen() {
        check(
            """
            val x = run {
                when (true) {
                    true -> 1<hint text=" ^run"/>
                    false ->0<hint text=" ^run"/>
                }
            }
            """
        )
    }

    fun testNoHintForSingleExpression() {
        check(
            """
            val x = run {
                1
            }
            """
        )
    }

    fun testLabel() {
        check(
            """
            val x = run foo@{
                println("foo")
                1<hint text=" ^foo"/>
            }
            """
        )
    }

    fun testNested() {
        check(
            """
            val x = run hello@{
                if (true) {
                }

                run { // Two hints here
                    when (true) {
                        true -> 1<hint text=" ^run"/>
                        false -> 0<hint text=" ^run"/>
                    }
                }<hint text=" ^hello"/>
            }
            """
        )
    }

    fun testElvisOperator() {
        check(
            """
            fun foo() {
                run {
                    val length: Int? = null
                    length ?: 0<hint text=" ^run"/>
                }
            }
            """
        )
    }

    fun testPostfixPrefixExpressions() {
        check(
            """
            fun bar() {
                var test = 0
                run {
                    test
                    test++<hint text=" ^run"/>
                }

                run {
                    test
                    ++test<hint text=" ^run"/>
                }
            }
            """
        )
    }

    fun testAnnotatedStatement() {
        check(
            """
            @Target(AnnotationTarget.EXPRESSION)
            annotation class Some

            fun test() {
                run {
                    val files: Any? = null
                    @Some
                    12<hint text=" ^run"/>
                }

                run {
                    val files: Any? = null
                    @Some 12<hint text=" ^run"/>
                }
            }
            """
        )
    }

    fun testLabeledStatement() {
        check(
            """
            fun test() {
                run {
                    val files: Any? = null
                    run@
                    12<hint text=" ^run"/>
                }

                run {
                    val files: Any? = null
                    run@12<hint text=" ^run"/>
                }
            }
            """
        )
    }

    fun testReturnFunctionType() {
        check(
            """
            fun test() = run {
                val a = 1
                { a }<hint text=" ^run"/>
            }
            """
        )
    }
}
