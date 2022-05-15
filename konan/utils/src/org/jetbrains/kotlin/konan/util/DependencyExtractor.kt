/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.util

import org.jetbrains.kotlin.konan.file.unzipTo
import java.io.File
import java.util.concurrent.TimeUnit

internal class DependencyExtractor {
    private val useZip = System.getProperty("os.name").startsWith("Windows")

    internal val archiveExtension = if (useZip) {
        "zip"
    } else {
        "tar.gz"
    }

    private fun extractTarGz(tarGz: File, targetDirectory: File) {
        val tarProcess = ProcessBuilder().apply {
            command("tar", "-xzf", tarGz.canonicalPath)
            directory(targetDirectory)
            inheritIO()
        }.start()
        val finished = tarProcess.waitFor(extractionTimeout, extractionTimeoutUntis)
        when {
            finished && tarProcess.exitValue() != 0 ->
                throw RuntimeException(
                    "Cannot extract archive with dependency: ${tarGz.canonicalPath}.\n" +
                    "Tar exit code: ${tarProcess.exitValue()}."
                )
            !finished -> {
                tarProcess.destroy()
                throw RuntimeException(
                    "Cannot extract archive with dependency: ${tarGz.canonicalPath}.\n" +
                    "Tar process hasn't finished in ${extractionTimeoutUntis.toSeconds(extractionTimeout)} sec."
                )
            }
        }
    }

    fun extract(archive: File, targetDirectory: File) {
        if (useZip) {
            archive.toPath().unzipTo(targetDirectory.toPath())
        } else {
            extractTarGz(archive, targetDirectory)
        }
    }

    companion object {
        val extractionTimeout = 3600L
        val extractionTimeoutUntis = TimeUnit.SECONDS
    }
}