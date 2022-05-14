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

import com.intellij.util.containers.HashMap
import org.jetbrains.kotlin.TestWithWorkingDir
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.incremental.testingUtils.*
import org.jetbrains.kotlin.incremental.utils.TestCompilationResult
import org.junit.Assert
import java.io.File
import java.util.*

abstract class AbstractIncrementalCompilerRunnerTestBase<Args : CommonCompilerArguments> : TestWithWorkingDir() {
    protected open val buildLogFinder: BuildLogFinder
        get() = BuildLogFinder(isGradleEnabled = true)

    protected fun make(sortedModules: List<Module>): TestCompilationResult {
        var exitCode = ExitCode.OK
        val compiledSources = HashSet<File>()
        val compileErrors = ArrayList<String>()

        for (module in sortedModules) {
            val (moduleExitCode, sources, errors) = make(module)
            exitCode = moduleExitCode
            compiledSources.addAll(sources)
            compileErrors.addAll(errors)

            if (exitCode != ExitCode.OK) break
        }

        return TestCompilationResult(exitCode, compiledSources, compileErrors)
    }
    protected abstract fun make(module: Module): TestCompilationResult

    protected abstract fun createCompilerArguments(module: Module): Args

    protected class Module(val name: String, workingDir: File) {
        val sourceRoot by lazy { File(workingDir, "src/$name").apply { mkdirs() } }
        val cacheDir by lazy { File(workingDir, "incremental-data/$name").apply { mkdirs() } }
        val outDir by lazy { File(workingDir, "out/$name").apply { mkdirs() } }
        val dependencies = hashSetOf<Module>()

        override fun equals(other: Any?): Boolean =
                name == (other as? Module)?.name

        override fun hashCode(): Int =
                name.hashCode()

        override fun toString(): String =
                "Module(name='$name')"
    }

    private fun createModule(name: String) =
            Module(name, File(workingDir, name))

    fun doTest(path: String) {
        val testDir = File(path)
        val sortedModules = sortModules(readModules(testDir))

        fun Iterable<File>.relativePaths() =
                map { it.relativeTo(workingDir).path.replace('\\', '/') }

        val mapWorkingToOriginalFile = HashMap<File, File>()
        for (module in sortedModules) {
            mapWorkingToOriginalFile.putAll(copyTestSources(testDir, module.sourceRoot, filePrefix = module.name))
        }
        // initial build
        val (_, _, errors) = make(sortedModules)
        if (errors.isNotEmpty()) {
            throw IllegalStateException("Initial build failed: \n${errors.joinToString("\n")}")
        }

        // modifications
        val buildLogFile = buildLogFinder.findBuildLog(testDir) ?: throw IllegalStateException("build log file not found in $workingDir")
        val buildLogSteps = parseTestBuildLog(buildLogFile)
        val modifications = getModificationsToPerform(testDir,
                                                      moduleNames = null,
                                                      allowNoFilesWithSuffixInTestData = false,
                                                      touchPolicy = TouchPolicy.CHECKSUM)

        assert(modifications.size == buildLogSteps.size) {
            "Modifications count (${modifications.size}) != expected build log steps count (${buildLogSteps.size})"
        }

        // Sometimes error messages differ.
        // This needs to be fixed, but it does not really matter much (e.g extra lines),
        // The workaround is to compare logs without errors, then logs with errors.
        // (if logs without errors differ then either compiled files differ or exit codes differ)
        val expectedSB = StringBuilder()
        val actualSB = StringBuilder()
        val expectedSBWithoutErrors = StringBuilder()
        val actualSBWithoutErrors = StringBuilder()
        var step = 1
        for ((modificationStep, buildLogStep) in modifications.zip(buildLogSteps)) {
            modificationStep.forEach { it.perform(workingDir, mapWorkingToOriginalFile) }
            val (_, compiledSources, compileErrors) = make(sortedModules)

            expectedSB.appendLine(stepLogAsString(step, buildLogStep.compiledKotlinFiles, buildLogStep.compileErrors))
            expectedSBWithoutErrors.appendLine(stepLogAsString(step, buildLogStep.compiledKotlinFiles, buildLogStep.compileErrors, includeErrors = false))
            actualSB.appendLine(stepLogAsString(step, compiledSources.relativePaths(), compileErrors))
            actualSBWithoutErrors.appendLine(stepLogAsString(step, compiledSources.relativePaths(), compileErrors, includeErrors = false))
            step++
        }

        if (expectedSBWithoutErrors.toString() != actualSBWithoutErrors.toString()) {
            Assert.assertEquals(expectedSB.toString(), actualSB.toString())
        }

        // todo: also compare caches
        run rebuildAndCompareOutput@ {
            val outAfterIC = HashMap<Module, File>()
            val outAfterIncrementalBuildRoot = File(workingDir, "out-after-ic")
            for (module in sortedModules) {
                val targetDir = File(outAfterIncrementalBuildRoot, module.name).apply { mkdirs() }
                module.outDir.copyRecursively(targetDir)
                outAfterIC[module] = targetDir

                with (module.outDir) {
                    deleteRecursively()
                    mkdirs()
                }

                with (module.cacheDir) {
                    deleteRecursively()
                    mkdirs()
                }
            }

            val rebuildResult = make(sortedModules)
            val rebuildExpectedToSucceed = buildLogSteps.last().compileSucceeded
            val rebuildSucceeded = rebuildResult.exitCode == ExitCode.OK
            Assert.assertEquals("Rebuild exit code differs from incremental exit code", rebuildExpectedToSucceed, rebuildSucceeded)

            if (rebuildSucceeded) {
                for (module in sortedModules) {
                    assertEqualDirectories(module.outDir, outAfterIC[module]!!, forgiveExtraFiles = rebuildSucceeded)
                }
            }
        }
    }

    private fun readModules(testDir: File): Set<Module> {
        val dependenciesTxt = File(testDir, "dependencies.txt")
        if (!dependenciesTxt.exists()) {
            return setOf(Module(name = "", workingDir = workingDir))
        }

        val modules = HashMap<String, Module>()
        for (line in dependenciesTxt.readLines()) {
            val (moduleName, dependencyName) = line.split("->")
            val module = modules.getOrPut(moduleName) { createModule(moduleName) }
            if (dependencyName.isNotBlank()) {
                val dependencyModule = modules.getOrPut(dependencyName) { createModule(dependencyName) }
                module.dependencies.add(dependencyModule)
            }
        }

        return modules.values.toSet()
    }

    private fun sortModules(modules: Set<Module>): List<Module> {
        val topSortedModules = ArrayList<Module>(modules.size)

        val visited = HashSet<Module>(modules.size)
        val visiting = HashSet<Module>(modules.size)

        fun visit(module: Module) {
            if (module in visited) return

            if (module in visiting) error("Cycles in module graphs are not supported")

            visiting.add(module)
            module.dependencies.forEach { visit(it) }
            visiting.remove(module)
            visited.add(module)

            topSortedModules.add(module)
        }

        for (module in modules) {
            visit(module)
        }

        return topSortedModules
    }

    private fun stepLogAsString(step: Int, ktSources: Iterable<String>, errors: Collection<String>, includeErrors: Boolean = true): String {
        val sb = StringBuilder()

        sb.appendLine("<======= STEP $step =======>")
        sb.appendLine()
        sb.appendLine("Compiled kotlin sources:")
        ktSources.toSet().toTypedArray().sortedArray().forEach { sb.appendLine(it) }
        sb.appendLine()

        if (errors.isEmpty()) {
            sb.appendLine("SUCCESS")
        }
        else {
            sb.appendLine("FAILURE")
            if (includeErrors) {
                errors.filter(String::isNotEmpty).forEach { sb.appendLine(it) }
            }
        }

        return sb.toString()
    }

    private fun StringBuilder.appendLine(line: String = "") {
        append(line)
        append('\n')
    }

    companion object {
        @JvmStatic
        protected val bootstrapKotlincLib: File = File("dependencies/bootstrap-compiler/Kotlin/kotlinc/lib")
    }
}