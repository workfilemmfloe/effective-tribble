/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k

import com.google.gson.Gson
import com.intellij.openapi.util.registry.Registry
import com.intellij.rt.execution.junit.FileComparisonFailure
import com.intellij.util.diff.Diff
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import org.jetbrains.kotlin.j2k.NewJ2KTestView.stat


abstract class AbstractNewJavaToKotlinConverterStructureSingleFileTest : AbstractNewJavaToKotlinConverterSingleFileTest() {

    override fun compareResults(expectedFile: File, actual: String) {
        results.totalExpected += expectedFile.readLines().count()
        results.totalActual += actual.lines().count()

        val beforeReg = Registry.`is`("diff.patience.alg")
        Registry.get("diff.patience.alg").setValue(true)
        Diff.buildChanges(expectedFile.readText(), actual)?.toList().orEmpty().forEach { change ->
            results.totalDiffPlus += change.inserted
            results.totalDiffMinus += change.deleted
        }
        Registry.get("diff.patience.alg").setValue(beforeReg)

        KotlinTestUtils.assertEqualsToFile(expectedFile, actual) {
            val file = createKotlinFile(it)
            file.dumpStructureText()
        }
    }

    override fun doTest(javaPath: String) {
        val testName = Thread.currentThread().stackTrace[5].let { "${it.className}.${it.methodName}" }
        try {
            super.doTest(javaPath)
            results.passes += testName
        } catch (e: AssertionError) {
            results.assertionFailures += testName
            throw e
        } catch (e: FileComparisonFailure) {
            results.assertionFailures += testName
            throw e
        } catch (e: Throwable) {
            results.exceptionFailures += testName
            throw e
        }
    }


    companion object {
        fun initializeForTests() {
            Runtime.getRuntime().addShutdownHook(thread(start = false) {
                println("CLOSED")


                val dateString = dateFormat.format(Date())

                File("./test_report/$dateString.txt").apply {
                    parentFile.mkdirs()
                    writeText(results.stat())
                }
                File("./test_report/$dateString.json").apply {
                    writeText(
                        results.serialize()
                    )
                }

            })
        }


        data class TestResults(
            val assertionFailures: MutableSet<String> = mutableSetOf(),
            val exceptionFailures: MutableSet<String> = mutableSetOf(),
            val passes: MutableSet<String> = mutableSetOf(),
            var totalDiffPlus: Int = 0,
            var totalDiffMinus: Int = 0,
            var totalExpected: Int = 0,
            var totalActual: Int = 0
        ) {
            fun serialize(): String {
                return Gson().toJson(this)
            }
        }

        fun loadTestResults(text: String): TestResults {
            return Gson().fromJson(text, TestResults::class.java)
        }


        val results: TestResults by lazy {
            initializeForTests()
            TestResults()
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd__HH-mm-ss")

    }
}