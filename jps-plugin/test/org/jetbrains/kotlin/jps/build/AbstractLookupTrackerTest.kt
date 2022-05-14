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

package org.jetbrains.kotlin.jps.build

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.incremental.components.LocationInfo
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.join
import java.io.File
import java.util.*

private val DECLARATION_KEYWORDS = listOf("interface", "class", "enum class", "object", "fun", "val", "var")
private val DECLARATION_STARTS_WITH = DECLARATION_KEYWORDS.map { it + " " }

abstract class AbstractLookupTrackerTest : AbstractIncrementalJpsTest(
        allowNoFilesWithSuffixInTestData = true,
        allowNoBuildLogFileInTestData = true
) {
    // ignore KDoc like comments which starts with `/**`, example: /** text */
    val COMMENT_WITH_LOOKUP_INFO = "/\\*[^*]+\\*/".toRegex()

    override fun checkLookups(lookupTracker: LookupTracker, compiledFiles: Set<File>) {
        if (lookupTracker !is TestLookupTracker) throw AssertionError("Expected TestLookupTracker, but: ${lookupTracker.javaClass}")

        val fileToLookups = lookupTracker.lookups.groupBy { it.lookupContainingFile }

        fun checkLookupsInFile(expectedFile: File, actualFile: File) {

            val independentFilePath = FileUtil.toSystemIndependentName(actualFile.path)
            val lookupsFromFile = fileToLookups[independentFilePath] ?: return

            val text = actualFile.readText()

            val matchResult = COMMENT_WITH_LOOKUP_INFO.find(text)
            if (matchResult != null) {
                fail("File $actualFile unexpectedly contains multiline comments. In range ${matchResult.range} found: ${matchResult.value} in $text")
            }

            val lines = text.lines().toArrayList()

            for ((line, lookupsFromLine) in lookupsFromFile.groupBy { it.lookupLine }) {
                val columnToLookups = lookupsFromLine.groupBy { it.lookupColumn }.toList().sortedBy { it.first }

                val lineContent = lines[line - 1]
                val parts = ArrayList<CharSequence>(columnToLookups.size * 2)

                var start = 0

                for ((column, lookupsFromColumn) in columnToLookups) {
                    val end = column - 1
                    parts.add(lineContent.subSequence(start, end))

                    val lookups = lookupsFromColumn.distinct().joinToString(separator = " ", prefix = "/*", postfix = "*/") {
                        val rest = lineContent.substring(end)

                        val name =
                                when {
                                    rest.startsWith(it.name) || // same name
                                    rest.startsWith("$" + it.name) || // backing field
                                    DECLARATION_STARTS_WITH.any { rest.startsWith(it) } // it's declaration
                                         -> ""
                                    else -> "(" + it.name + ")"
                                }

                        it.scopeKind.toString()[0].toLowerCase().toString() + ":" + it.scopeFqName.let { if (it.isNotEmpty()) it else "<root>" } + name
                    }

                    parts.add(lookups)

                    start = end
                }

                lines[line - 1] = parts.joinToString("") + lineContent.subSequence(start, lineContent.length)
            }

            val actual = lines.joinToString("\n")

            KotlinTestUtils.assertEqualsToFile(expectedFile, actual)
        }

        for (actualFile in compiledFiles) {
            val expectedFile = mapWorkingToOriginalFile[actualFile]!!

            checkLookupsInFile(expectedFile, actualFile)
        }
    }

    override fun preProcessSources(srcDir: File) = dropBlockComments(srcDir)

    private fun dropBlockComments(workSrcDir: File) {
        for (file in workSrcDir.walkTopDown()) {
            if (!file.isFile) continue

            val original = file.readText()
            val modified = original.replace(COMMENT_WITH_LOOKUP_INFO, "")

            if (original != modified) {
                file.writeText(modified)
            }
        }
    }
}

class TestLookupTracker : LookupTracker {
    val lookups = arrayListOf<LookupInfo>()

    override fun record(locationInfo: LocationInfo, scopeFqName: String, scopeKind: ScopeKind, name: String) {
        val (line, column) = locationInfo.position
        lookups.add(LookupInfo(locationInfo.filePath, line, column, scopeFqName, scopeKind, name))
    }

    data class LookupInfo(
            val lookupContainingFile: String,
            val lookupLine: Int,
            val lookupColumn: Int,
            val scopeFqName: String,
            val scopeKind: ScopeKind,
            val name: String
    )
}


