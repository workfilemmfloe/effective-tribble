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

package org.jetbrains.jet.jps.build

import org.jetbrains.ether.IncrementalTestCase
import org.jetbrains.jps.builders.JpsBuildTestCase
import kotlin.properties.Delegates
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import org.jetbrains.jps.builders.CompileScopeTestBuilder
import org.jetbrains.jps.builders.BuildResult
import org.jetbrains.jps.builders.impl.logging.ProjectBuilderLoggerBase
import org.jetbrains.jps.builders.logging.BuildLoggingManager
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.util.JpsPathUtil
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.jet.config.IncrementalCompilation

public class IncrementalJpsTest : JpsBuildTestCase() {
    private val testDataDir: File
        get() = File(AbstractKotlinJpsBuildTestCase.TEST_DATA_PATH + "incremental/" + getTestName(true))

    var workDir: File by Delegates.notNull()

    override fun setUp() {
        super.setUp()
        System.setProperty("kotlin.jps.tests", "true")

        workDir = FileUtil.createTempDirectory("jps-build", null)

        FileUtil.copyDir(testDataDir, File(workDir, "src"), { it.getName().endsWith(".kt") || it.getName().endsWith(".java") })

        JpsJavaExtensionService.getInstance().getOrCreateProjectExtension(myProject!!)
                .setOutputUrl(JpsPathUtil.pathToUrl(getAbsolutePath("out")))
    }

    override fun tearDown() {
        System.clearProperty("kotlin.jps.tests")
        super.tearDown()
    }

    fun buildGetLog(scope: CompileScopeTestBuilder = CompileScopeTestBuilder.make().all()): String {
        val logger = MyLogger(FileUtil.toSystemIndependentName(workDir.getAbsolutePath()))
        val descriptor = createProjectDescriptor(BuildLoggingManager(logger))
        try {
            doBuild(descriptor, scope)!!.assertSuccessful()
            return logger.log
        } finally {
            descriptor.release()
        }
    }

    private fun doTest() {
        if (!IncrementalCompilation.ENABLED) {
            return
        }

        addModule("module", array<String>(getAbsolutePath("src")), null, null, addJdk("my jdk"))
        AbstractKotlinJpsBuildTestCase.addKotlinRuntimeDependency(myProject!!)

        buildGetLog()

        FileUtil.processFilesRecursively(testDataDir, {
            if (it!!.getName().endsWith(".new")) {
                it.copyTo(File(workDir, "src/" + it.getName().trimTrailing(".new")))
            }

            true
        })

        val log = buildGetLog()
        UsefulTestCase.assertSameLinesWithFile(File(testDataDir, "build.log").getAbsolutePath(), log)

        val outDir = File(getAbsolutePath("out"))
        val outAfterMake = File(getAbsolutePath("out-after-make"))
        FileUtil.copyDir(outDir, outAfterMake)

        buildGetLog(CompileScopeTestBuilder.rebuild().allModules())

        assertEqualDirectories(outDir, outAfterMake, { it.name == "script.xml" })
    }

    override fun doGetProjectDir(): File? = workDir

    fun testIndependentClasses() {
        doTest()
    }

    fun testSimpleClassDependency() {
        doTest()
    }

    fun testTopLevelMembersInTwoPackages() {
        doTest()
    }

    private class MyLogger(val rootPath: String) : ProjectBuilderLoggerBase() {
        private val logBuf = StringBuilder()
        public val log: String
            get() = logBuf.toString()

        override fun isEnabled(): Boolean = true

        override fun logLine(message: String?) {

            fun String.replaceHashWithStar(): String {
                val lastHyphen = this.lastIndexOf('-')
                if (lastHyphen != -1 && substring(lastHyphen + 1).matches("[0-9a-f]{8}\\.class")) {
                    return substring(0, lastHyphen) + "-*.class"
                }
                return this
            }

            logBuf.append(message!!.trimLeading(rootPath + "/").replaceHashWithStar()).append('\n')
        }
    }
}
