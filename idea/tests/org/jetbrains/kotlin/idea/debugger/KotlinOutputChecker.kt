/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.debugger.impl.OutputChecker
import com.intellij.idea.IdeaLogger
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vfs.CharsetToolkit
import org.junit.Assert
import java.io.File

internal class KotlinOutputChecker(appPath: String, outputPath: String) : OutputChecker(appPath, outputPath) {
    private val CONNECT_PREFIX = "Connected to the target VM"
    private val DISCONNECT_PREFIX = "Disconnected from the target VM"
    private val RUN_JAVA = "Run Java"

    companion object {
        @JvmStatic
        private val LOG = Logger.getInstance(KotlinOutputChecker::class.java)
    }

    // Copied from the base OutputChecker.checkValid(). Need to intercept call to base preprocessBuffer() method
    override fun checkValid(jdk: Sdk, sortClassPath: Boolean) {
        if (IdeaLogger.ourErrorsOccurred != null) {
            throw IdeaLogger.ourErrorsOccurred
        }

        val actual = preprocessBuffer(buildOutputString())

        val outs = File(myAppPath + File.separator + "outs")
        assert(outs.exists() || outs.mkdirs()) { outs }

        var outFile = File(outs, myTestName + ".out")
        if (!outFile.exists()) {
            if (SystemInfo.isWindows) {
                val winOut = File(outs, myTestName + ".win.out")
                if (winOut.exists()) {
                    outFile = winOut
                }
            }
            else if (SystemInfo.isUnix) {
                val unixOut = File(outs, myTestName + ".unx.out")
                if (unixOut.exists()) {
                    outFile = unixOut
                }
            }
        }

        if (!outFile.exists()) {
            FileUtil.writeToFile(outFile, actual)
            LOG.error("Test file created ${outFile.path}\n**************** Don't forget to put it into VCS! *******************")
        }
        else {
            val originalText = FileUtilRt.loadFile(outFile, CharsetToolkit.UTF8)
            val expected = StringUtilRt.convertLineSeparators(originalText)
            if (expected != actual) {
                println("expected:")
                println(originalText)
                println("actual:")
                println(actual)

                val len = Math.min(expected.length, actual.length)
                if (expected.length != actual.length) {
                    println("Text sizes differ: expected " + expected.length + " but actual: " + actual.length)
                }
                if (expected.length > len) {
                    println("Rest from expected text is: \"" + expected.substring(len) + "\"")
                }
                else if (actual.length > len) {
                    println("Rest from actual text is: \"" + actual.substring(len) + "\"")
                }

                Assert.assertEquals(originalText, actual)
            }
        }
    }

    private fun preprocessBuffer(buffer: String): String {
        val lines = buffer.lines().toMutableList()

        val connectedIndex = lines.indexOfFirst { it.startsWith(CONNECT_PREFIX) }
        lines[connectedIndex] = CONNECT_PREFIX

        val runCommandIndex = connectedIndex - 1
        lines[runCommandIndex] = RUN_JAVA

        val disconnectedIndex = lines.indexOfFirst { it.startsWith(DISCONNECT_PREFIX) }
        lines[disconnectedIndex] = DISCONNECT_PREFIX

        return lines.joinToString("\n")
    }

    private fun buildOutputString(): String {
        // Call base method with reflection
        val m = OutputChecker::class.java.getDeclaredMethod("buildOutputString")!!
        val isAccessible = m.isAccessible

        try {
            m.isAccessible = true
            return m.invoke(this) as String
        }
        finally {
            m.isAccessible = isAccessible
        }
    }
}
