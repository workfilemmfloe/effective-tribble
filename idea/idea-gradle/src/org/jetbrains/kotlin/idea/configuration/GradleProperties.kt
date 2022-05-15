/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Joiner
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.util.SystemProperties
import org.jetbrains.annotations.NonNls
import java.io.File
import java.io.IOException
import java.util.*

fun getBaseDirPath(project: Project): File {
    val basePath = project.basePath!!
    return File(ExternalSystemApiUtil.toCanonicalPath(basePath))
}

class GradleProperties @Throws(IOException::class) constructor(val path: File) {
    val properties: Properties

    @Throws(IOException::class)
    constructor(project: Project) : this(File(getBaseDirPath(project), "gradle.properties"))

    init {
        properties = PropertiesFiles.getProperties(this.path)
    }

    @VisibleForTesting
    internal fun getProperty(name: String): String? {
        return properties.getProperty(name)
    }

    @Throws(IOException::class)
    fun save() {
        PropertiesFiles.savePropertiesToFile(properties, path, headerComment)
    }

    fun clear() {
        properties.clear()
    }

    companion object {
        @NonNls
        private val JVM_ARGS_PROPERTY_NAME = "org.gradle.jvmargs"

        private val headerComment: String
            get() {
                val lines = arrayOf(
                    "# For more details on how to configure your build environment visit",
                    "# http://www.gradle.org/docs/current/userguide/build_environment.html",
                    "",
                    "# Specifies the JVM arguments used for the daemon process.",
                    "# The setting is particularly useful for tweaking memory settings.",
                    "# Default value: -Xmx1024m -XX:MaxPermSize=256m",
                    "# org.gradle.jvmargs=-Xmx2048m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8",
                    "",
                    "# When configured, Gradle will run in incubating parallel mode.",
                    "# This option should only be used with decoupled projects. More details, visit",
                    "# http://www.gradle.org/docs/current/userguide/multi_project_builds.html#sec:decoupled_projects",
                    "# org.gradle.parallel=true"
                )
                return Joiner.on(SystemProperties.getLineSeparator()).join(lines)
            }

    }
}