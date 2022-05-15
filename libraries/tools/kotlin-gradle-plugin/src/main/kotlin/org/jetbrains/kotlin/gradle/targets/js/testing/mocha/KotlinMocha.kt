/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.mocha

import org.gradle.api.Project
import org.gradle.process.ProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.KotlinGradleNpmPackage
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.jsQuoted
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinTestRunnerCliArgs

class KotlinMocha(override val compilation: KotlinJsCompilation) : KotlinJsTestFramework {
    private val project: Project = compilation.target.project
    private val nodeJs = NodeJsRootPlugin.apply(project.rootProject)
    private val versions = nodeJs.versions

    override val settingsState: String
        get() = "mocha"

    override val requiredNpmDependencies: Collection<RequiredKotlinJsDependency>
        get() = listOf(
            KotlinGradleNpmPackage("test-js-runner"),
            versions.mocha,
            versions.mochaTeamCityReporter
        )

    override fun createTestExecutionSpec(
        task: KotlinJsTest,
        forkOptions: ProcessForkOptions,
        nodeJsArgs: MutableList<String>
    ): TCServiceMessagesTestExecutionSpec {
        val clientSettings = TCServiceMessagesClientSettings(
            task.name,
            testNameSuffix = task.targetName,
            prependSuiteName = true,
            stackTraceParser = ::parseNodeJsStackTraceAsJvm,
            ignoreOutOfRootNodes = true
        )

        val npmProject = compilation.npmProject

        val cliArgs = KotlinTestRunnerCliArgs(
            include = task.includePatterns,
            exclude = task.excludePatterns
        )

        createAdapterJs(task)

        val nodeModules = listOf(
            "mocha/bin/mocha",
            "./$ADAPTER_NODEJS"
        )

        val args = nodeJsArgs +
                nodeModules.map {
                    npmProject.require(it)
                } + cliArgs.toList() +
                listOf("--reporter", "mocha-teamcity-reporter") +
                listOf(
                    "-r", "kotlin-test-js-runner/kotlin-nodejs-source-map-support.js"
                )

        return TCServiceMessagesTestExecutionSpec(
            forkOptions,
            args,
            false,
            clientSettings
        )
    }

    private fun createAdapterJs(task: KotlinJsTest) {
        val npmProject = compilation.npmProject
        val file = task.nodeModulesToLoad
            .map { npmProject.require(it) }
            .single()

        val adapterJs = npmProject.dir.resolve(ADAPTER_NODEJS)
        adapterJs.printWriter().use { writer ->
            val adapter = npmProject.require("kotlin-test-js-runner/kotlin-test-nodejs-runner.js")
            writer.println("require(${adapter.jsQuoted()})")

            writer.println("require(${file.jsQuoted()})")
        }
    }

    companion object {
        const val ADAPTER_NODEJS = "adapter-nodejs.js"
    }
}