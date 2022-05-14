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

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.cli.common.arguments.K2JSDceArguments
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.gradle.compilerRunner.GradleKotlinLogger
import org.jetbrains.kotlin.gradle.compilerRunner.runToolInSeparateProcess
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDce
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDceOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJsDceOptionsImpl

private const val DCE_CLASS_FQ_NAME = "org.jetbrains.kotlin.cli.js.dce.K2JSDce"

open class KotlinJsDce : AbstractKotlinCompileTool<K2JSDceArguments>(), KotlinJsDce {
    private val dceOptionsImpl = KotlinJsDceOptionsImpl()

    override val dceOptions: KotlinJsDceOptions
        get() = dceOptionsImpl

    override val keep: MutableList<String> = mutableListOf()

    override fun compile() {}

    override fun keep(vararg fqn: String) {
        keep += fqn
    }

    @Suppress("unused")
    @TaskAction
    fun performDce() {
        val inputFiles = getSource().files.map { it.path }

        val outputDirArgs = arrayOf("-output-dir", destinationDir.path)

        val args = K2JSDceArguments()
        dceOptionsImpl.updateArguments(args)
        args.declarationsToKeep = keep.toTypedArray()
        val argsArray = ArgumentUtils.convertArgumentsToStringList(args).toTypedArray()

        val log = GradleKotlinLogger(project.logger)
        val allArgs = argsArray + outputDirArgs + inputFiles
        val exitCode = runToolInSeparateProcess(allArgs, DCE_CLASS_FQ_NAME, computedCompilerClasspath, log)
        throwGradleExceptionIfError(exitCode)
    }
}