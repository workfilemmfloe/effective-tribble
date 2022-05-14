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

import org.gradle.api.Project
import org.jetbrains.kotlin.daemon.common.CompilationResultCategory
import org.jetbrains.kotlin.daemon.common.CompilationResults
import org.jetbrains.kotlin.daemon.common.LoopbackNetworkInterface
import org.jetbrains.kotlin.daemon.common.SOCKET_ANY_FREE_PORT
import org.jetbrains.kotlin.daemon.common.CompileIterationResult
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import org.jetbrains.kotlin.gradle.incremental.pathsAsStringRelativeTo
import java.io.Serializable
import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject

internal class GradleCompilationResults(
        project: Project
): CompilationResults,
   UnicastRemoteObject(SOCKET_ANY_FREE_PORT, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory) {

    private val log = project.logger
    private val projectRootFile = project.rootProject.projectDir

    @Throws(RemoteException::class)
    override fun add(compilationResultCategory: Int, value: Serializable) {
        if (compilationResultCategory == CompilationResultCategory.IC_COMPILE_ITERATION.code) {
            @Suppress("UNCHECKED_CAST")
            val compileIterationResult = value as? CompileIterationResult
            if (compileIterationResult != null) {
                val sourceFiles = compileIterationResult.sourceFiles
                if (sourceFiles.any()) {
                    log.kotlinDebug { "compile iteration: ${sourceFiles.pathsAsStringRelativeTo(projectRootFile)}" }
                }
                val exitCode = compileIterationResult.exitCode
                log.kotlinDebug { "compiler exit code: $exitCode" }
            }
        }
    }
}