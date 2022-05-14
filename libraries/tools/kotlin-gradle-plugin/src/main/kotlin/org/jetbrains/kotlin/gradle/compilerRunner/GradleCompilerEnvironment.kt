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

package org.jetbrains.kotlin.gradle.compilerRunner

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.compilerRunner.CompilerEnvironment
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollector
import org.jetbrains.kotlin.gradle.tasks.GradleMessageCollector
import org.jetbrains.kotlin.gradle.tasks.findToolsJar
import org.jetbrains.kotlin.gradle.incremental.ChangedFiles
import org.jetbrains.kotlin.incremental.ICReporter
import org.jetbrains.kotlin.gradle.incremental.multiproject.ArtifactDifferenceRegistryProvider
import java.io.File
import java.net.URL

internal open class GradleCompilerEnvironment(
        val compilerClasspath: List<File>,
        messageCollector: GradleMessageCollector,
        outputItemsCollector: OutputItemsCollector,
        val compilerArgs: CommonCompilerArguments
) : CompilerEnvironment(messageCollector, outputItemsCollector) {
    val toolsJar: File? by lazy { findToolsJar() }

    val compilerFullClasspath: List<File>
            get() = (compilerClasspath + toolsJar).filterNotNull()

    val compilerClasspathURLs: List<URL>
        get() = compilerFullClasspath.map { it.toURI().toURL() }
}

internal class GradleIncrementalCompilerEnvironment(
        compilerClasspath: List<File>,
        val changedFiles: ChangedFiles,
        val reporter: ICReporter,
        val workingDir: File,
        messageCollector: GradleMessageCollector,
        outputItemsCollector: OutputItemsCollector,
        compilerArgs: CommonCompilerArguments,
        val artifactDifferenceRegistryProvider: ArtifactDifferenceRegistryProvider? = null,
        val artifactFile: File? = null,
        val buildHistoryFile: File? = null,
        val friendBuildHistoryFile: File? = null
) : GradleCompilerEnvironment(compilerClasspath, messageCollector, outputItemsCollector, compilerArgs)