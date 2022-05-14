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

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.compilerRunner.KotlinLogger
import org.jetbrains.kotlin.daemon.client.DaemonReportMessage
import org.jetbrains.kotlin.daemon.client.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.client.launchProcessWithFallback
import org.jetbrains.kotlin.daemon.common.DaemonReportCategory
import java.io.File
import java.net.URLClassLoader
import kotlin.concurrent.thread

private const val KOTLIN_COMPILER_VERSION_FQ_NAME = "org.jetbrains.kotlin.config.KotlinCompilerVersion"

internal fun loadCompilerVersion(env: GradleCompilerEnvironment): String {
    val result: String? = try {
        val classloader = URLClassLoader(env.compilerClasspathURLs.toTypedArray())
        val compilerVersionClass = Class.forName(KOTLIN_COMPILER_VERSION_FQ_NAME, false, classloader)
        val versionField = compilerVersionClass.fields.find { it.name == "VERSION" }
        versionField?.get(compilerVersionClass).toString()
    }
    catch (e: Throwable) {
        null
    }

    return result ?: "<unknown>"
}

internal fun runToolInSeparateProcess(
        argsArray: Array<String>, compilerClassName: String, classpath: List<File>, logger: KotlinLogger
): ExitCode {
    val javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
    val classpathString = classpath.map {it.absolutePath}.joinToString(separator = File.pathSeparator)
    val builder = ProcessBuilder(javaBin, "-cp", classpathString, compilerClassName, *argsArray)
    val messages = arrayListOf<DaemonReportMessage>()
    val process = launchProcessWithFallback(builder, DaemonReportingTargets(messages = messages))
    messages.reportTo(logger)

    // important to read inputStream, otherwise the process may hang on some systems
    val readErrThread = thread {
        process.errorStream!!.bufferedReader().forEachLine {
            System.err.println(it)
        }
    }
    process.inputStream!!.bufferedReader().forEachLine {
        System.out.println(it)
    }
    readErrThread.join()

    val exitCode = process.waitFor()
    logger.logFinish(OUT_OF_PROCESS_EXECUTION_STRATEGY)
    return exitCodeFromProcessExitCode(logger, exitCode)
}

private fun Iterable<DaemonReportMessage>.reportTo(log: KotlinLogger) {
    for (message in this) {
        @Suppress("UNUSED_VARIABLE")
        val exhaustive: Unit = when (message.category) {
            DaemonReportCategory.DEBUG -> {
                log.debug(message.message)
            }
            DaemonReportCategory.INFO -> {
                log.info(message.message)
            }
            DaemonReportCategory.EXCEPTION -> {
                log.error(message.message)
            }
        }
    }
}

internal fun KotlinLogger.logFinish(strategy: String) {
    debug("Finished executing kotlin compiler using $strategy strategy")
}

internal fun exitCodeFromProcessExitCode(log: KotlinLogger, code: Int): ExitCode {
    val exitCode = ExitCode.values().find { it.code == code }
    if (exitCode != null) return exitCode

    log.debug("Could not find exit code by value: $code")
    return if (code == 0) ExitCode.OK else ExitCode.COMPILATION_ERROR
}