/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import java.io.File

abstract class AbstractIncrementalJsCompilerRunnerEmptyClasspath : AbstractIncrementalJsCompilerRunnerTest() {
    override fun createCompilerArguments(destinationDir: File, testDir: File): K2JSCompilerArguments =
        super.createCompilerArguments(destinationDir, testDir).apply {
            noStdlib = true
            libraries = ""
        }
}