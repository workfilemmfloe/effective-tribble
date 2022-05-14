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

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder
import org.jetbrains.kotlin.incremental.utils.TestCompilationResult
import org.jetbrains.kotlin.incremental.utils.TestICReporter
import org.jetbrains.kotlin.incremental.utils.TestMessageCollector
import java.io.File

abstract class AbstractIncrementalJsCompilerRunnerTest : AbstractIncrementalCompilerRunnerTestBase<K2JSCompilerArguments>() {
    override fun make(module: Module): TestCompilationResult {
        val reporter = TestICReporter()
        val messageCollector = TestMessageCollector()
        val cacheDir = module.cacheDir
        val sourceRoots = listOf(module.sourceRoot)
        val args = createCompilerArguments(module)
        makeJsIncrementally(cacheDir, sourceRoots, args, reporter = reporter, messageCollector = messageCollector)
        return TestCompilationResult(reporter, messageCollector)
    }

    override val buildLogFinder: BuildLogFinder
        get() = super.buildLogFinder.copy(isJsEnabled = true)

    override fun createCompilerArguments(module: Module): K2JSCompilerArguments {
        val libs = arrayListOf(File(bootstrapKotlincLib, "kotlin-stdlib-js.jar"))
        module.dependencies.mapTo(libs) { it.outDir }

        return K2JSCompilerArguments().apply {
            outputFile = File(module.outDir, "${module.name}.js").path
            libraries = libs.joinToString(File.pathSeparator) { it.canonicalPath }
            metaInfo = true
        }
    }

}