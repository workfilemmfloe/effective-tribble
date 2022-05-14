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

package org.jetbrains.jet.plugin.debugger.evaluate

import com.intellij.debugger.DebuggerInvocationUtil
import com.intellij.debugger.engine.evaluation.expression.EvaluatorBuilderImpl
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.engine.evaluation.CodeFragmentKind
import com.intellij.debugger.engine.ContextUtil
import com.intellij.debugger.engine.SuspendContextImpl
import org.jetbrains.jet.plugin.debugger.*
import org.junit.Assert
import org.jetbrains.jet.plugin.JetFileType
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import org.jetbrains.jet.InTextDirectivesUtils
import org.jetbrains.eval4j.jdi.asValue
import org.jetbrains.eval4j.Value
import org.jetbrains.eval4j.ObjectValue
import com.sun.jdi.ObjectReference
import org.jetbrains.jet.lang.psi.JetCodeFragment
import java.util.Collections
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.execution.process.ProcessOutputTypes
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.spi.LoggingEvent
import org.apache.log4j.Level
import org.apache.log4j.Logger

public abstract class AbstractKotlinEvaluateExpressionTest : KotlinDebuggerTestCase() {
    private val logger = Logger.getLogger(javaClass<KotlinEvaluateExpressionCache>())!!

    private val appender = object : AppenderSkeleton() {
        override fun append(event: LoggingEvent?) {
            println(event?.getRenderedMessage(), ProcessOutputTypes.SYSTEM)
        }
        override fun close() {}
        override fun requiresLayout() = false
    }

    private var oldLogLevel: Level? = null

    override fun setUp() {
        super<KotlinDebuggerTestCase>.setUp()

        oldLogLevel = logger.getLevel()
        logger.setLevel(Level.DEBUG)
        logger.addAppender(appender)
    }

    override fun tearDown() {
        logger.setLevel(oldLogLevel)
        logger.removeAppender(appender)

        super<KotlinDebuggerTestCase>.tearDown()
    }

    fun doSingleBreakpointTest(path: String) {
        val file = File(path)
        val expressions = loadTestDirectivesPairs(file, "// EXPRESSION: ", "// RESULT: ")

        val blocks = findFilesWithBlocks(file).map { FileUtil.loadFile(it, true) }
        val expectedBlockResults = blocks.map { InTextDirectivesUtils.findLinesWithPrefixesRemoved(it, "// RESULT: ").makeString("\n") }

        createDebugProcess(path)

        onBreakpoint {
            val exceptions = linkedMapOf<String, Throwable>()
            for ((expression, expected) in expressions) {
                mayThrow(exceptions, expression) {
                    evaluate(expression, CodeFragmentKind.EXPRESSION, expected)
                }
            }

            for ((i, block) in blocks.withIndices()) {
                mayThrow(exceptions, block) {
                    evaluate(block, CodeFragmentKind.CODE_BLOCK, expectedBlockResults[i])
                }
            }

            checkExceptions(exceptions)
        }
        finish()
    }

    fun doMultipleBreakpointsTest(path: String) {
        val expressions = loadTestDirectivesPairs(File(path), "// EXPRESSION: ", "// RESULT: ")

        createDebugProcess(path)

        val exceptions = linkedMapOf<String, Throwable>()
        for ((expression, expected) in expressions) {
            mayThrow(exceptions, expression) {
                onBreakpoint {
                    evaluate(expression, CodeFragmentKind.EXPRESSION, expected)
                }
            }
        }

        checkExceptions(exceptions)

        finish()
    }



    private fun checkExceptions(exceptions: MutableMap<String, Throwable>) {
        if (!exceptions.empty) {
            for (exc in exceptions.values()) {
                exc.printStackTrace()
            }
            throw AssertionError("Test failed:\n" + exceptions.map { "expression: ${it.key}, exception: ${it.value.getMessage()}" }.makeString("\n"))
        }
    }

    private fun mayThrow(map: MutableMap<String, Throwable>, expression: String, f: () -> Unit) {
        try {
            f()
        }
        catch (e: Throwable) {
            map.put(expression, e)
        }
    }

    private fun loadTestDirectivesPairs(file: File, directivePrefix: String, expectedPrefix: String): List<Pair<String, String>> {
        val fileContent = FileUtil.loadFile(file, true)
        val directives = InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileContent, directivePrefix)
        val expected = InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileContent, expectedPrefix)
        assert(directives.size == expected.size, "Sizes of test directives are different")
        return directives.zip(expected)
    }

    private fun findFilesWithBlocks(mainFile: File): List<File> {
        val mainFileName = mainFile.getName()
        return mainFile.getParentFile()?.listFiles()?.filter { it.name.startsWith(mainFileName) && it.name != mainFileName } ?: Collections.emptyList()
    }

    private fun onBreakpoint(doOnBreakpoint: SuspendContextImpl.() -> Unit) {
        super.onBreakpoint {
            super.printContext(it)
            it.doOnBreakpoint()
        }
    }

    private fun finish() {
        onBreakpoint {
            resume(this)
        }
    }

    private fun SuspendContextImpl.evaluate(text: String, codeFragmentKind: CodeFragmentKind, expectedResult: String) {
        try {
            val sourcePosition = ContextUtil.getSourcePosition(this)
            val contextElement = ContextUtil.getContextElement(sourcePosition)!!
            Assert.assertTrue("KotlinCodeFragmentFactory should be accepted for context element otherwise default evaluator will be called. ContextElement = ${contextElement.getText()}",
                              KotlinCodeFragmentFactory().isContextAccepted(contextElement))

            val evaluator = DebuggerInvocationUtil.commitAndRunReadAction(getProject()) {
                EvaluatorBuilderImpl.build(TextWithImportsImpl(
                        codeFragmentKind,
                        text,
                        JetCodeFragment.getImportsForElement(contextElement),
                        JetFileType.INSTANCE),
                                           contextElement,
                                           sourcePosition)
            }
            if (evaluator == null) throw AssertionError("Cannot create an Evaluator for Evaluate Expression")

            val value = evaluator.evaluate(createEvaluationContext(this))
            val actualResult = value.asValue().asString()
            Assert.assertTrue("Evaluate expression returns wrong result for $text:\nexpected = $expectedResult\nactual   = $actualResult\n", expectedResult == actualResult)
        }
        catch (e: EvaluateException) {
            Assert.assertTrue("Evaluate expression throws wrong exception for $text:\nexpected = $expectedResult\nactual   = ${e.getMessage()}\n", expectedResult == e.getMessage()?.replaceFirst("id=[0-9]*", "id=ID"))
        }
        finally {
            resume(this)
        }
    }

    private fun Value.asString(): String {
        if (this is ObjectValue && this.value is ObjectReference) {
            return this.toString().replaceFirst("id=[0-9]*", "id=ID")
        }
        return this.toString()
    }
}