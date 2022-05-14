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
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.client.reportFromDaemon
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import java.io.File
import java.io.Serializable
import java.rmi.Remote
import java.rmi.server.UnicastRemoteObject

internal open class GradleCompilerServicesFacadeImpl(
        project: Project,
        val compilerMessageCollector: MessageCollector,
        port: Int = SOCKET_ANY_FREE_PORT
) : UnicastRemoteObject(port, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory),
    CompilerServicesFacadeBase,
    Remote {

    protected val log: Logger = project.logger

    override fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        when (ReportCategory.fromCode(category)) {
            ReportCategory.IC_MESSAGE -> {
                log.kotlinDebug { "[IC] $message" }
            }
            ReportCategory.DAEMON_MESSAGE -> {
                log.kotlinDebug { "[DAEMON] $message" }
            }
            else -> {
                compilerMessageCollector.reportFromDaemon(
                        outputsCollector = null,
                        category = category,
                        severity = severity,
                        message = message,
                        attachment = attachment)
            }
        }
    }
}

internal class GradleIncrementalCompilerServicesFacadeImpl(
        project: Project,
        private val environment: GradleIncrementalCompilerEnvironment,
        port: Int = SOCKET_ANY_FREE_PORT
) : GradleCompilerServicesFacadeImpl(project, environment.messageCollector, port),
    IncrementalCompilerServicesFacade {

    override fun hasAnnotationsFileUpdater(): Boolean = false
    override fun updateAnnotations(outdatedClassesJvmNames: Iterable<String>) {
    }
    override fun revert() {
    }

    override fun getChanges(artifact: File, sinceTS: Long): Iterable<SimpleDirtyData>? =
            null

    override fun registerChanges(timestamp: Long, dirtyData: SimpleDirtyData) {
    }

    override fun unknownChanges(timestamp: Long) {
    }
}
