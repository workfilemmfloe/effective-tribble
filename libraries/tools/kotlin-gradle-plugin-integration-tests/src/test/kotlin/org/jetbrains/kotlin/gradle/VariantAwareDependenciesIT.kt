/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.gradle.util.testResolveAllConfigurations
import org.junit.Test

class VariantAwareDependenciesIT : BaseGradleIT() {
    private val gradleVersion = GradleVersionRequired.AtLeast("4.8")

    @Test
    fun testJvmKtAppResolvesMppLib() {
        val outerProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        val innerProject = Project("simpleProject")

        with(outerProject) {
            embedProject(innerProject)
            gradleBuildScript(innerProject.projectName).appendText("\ndependencies { compile rootProject }")

            testResolveAllConfigurations(innerProject.projectName) {
                assertContains(">> :${innerProject.projectName}:runtime --> sample-lib-jvm6-1.0.jar")
            }
        }
    }

    @Test
    fun testJsKtAppResolvesMppLib() {
        val outerProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        val innerProject = Project("kotlin2JsInternalTest")

        with(outerProject) {
            embedProject(innerProject)
            gradleBuildScript(innerProject.projectName).appendText("\nrepositories { jcenter() }; dependencies { compile rootProject }")

            testResolveAllConfigurations(innerProject.projectName) {
                assertContains(">> :${innerProject.projectName}:runtime --> sample-lib-nodejs-1.0.jar")
            }
        }
    }

    @Test
    fun testMppLibResolvesJvmKtApp() {
        val outerProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        val innerProject = Project("simpleProject")

        with(outerProject) {
            embedProject(innerProject)
            gradleBuildScript().appendText("\ndependencies { jvm6MainImplementation project(':${innerProject.projectName}') }")

            testResolveAllConfigurations(innerProject.projectName)
        }
    }

    @Test
    fun testMppLibResolvesJsKtApp() {
        val outerProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        val innerProject = Project("kotlin2JsInternalTest")

        with(outerProject) {
            embedProject(innerProject)
            gradleBuildScript().appendText("\ndependencies { nodeJsMainImplementation project(':${innerProject.projectName}') }")

            testResolveAllConfigurations(innerProject.projectName)
        }
    }

    @Test
    fun testNonKotlinJvmAppResolvesMppLib() {
        val outerProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        val innerProject = Project("simpleProject").apply {
            setupWorkingDir()
            gradleBuildScript().modify { it.replace("apply plugin: \"kotlin\"", "") }
        }

        with(outerProject) {
            embedProject(innerProject)
            gradleBuildScript(innerProject.projectName).appendText("\ndependencies { compile rootProject }")

            testResolveAllConfigurations(innerProject.projectName)
        }
    }

    @Test
    fun testJvmKtAppResolvesJvmKtApp() {
        val outerProject = Project("simpleProject", gradleVersion)
        val innerProject = Project("jvmTarget") // cannot use simpleApp again, should be another project

        with(outerProject) {
            embedProject(innerProject)
            gradleBuildScript(innerProject.projectName).appendText("\ndependencies { compile rootProject }")

            testResolveAllConfigurations(innerProject.projectName)
        }
    }

    @Test
    fun testJsKtAppResolvesJsKtApp() {
        val outerProject = Project("kotlin2JsInternalTest", gradleVersion)
        val innerProject = Project("kotlin2JsNoOutputFileProject")

        with(outerProject) {
            embedProject(innerProject)
            gradleBuildScript(innerProject.projectName).appendText("\ndependencies { compile rootProject }")

            testResolveAllConfigurations(innerProject.projectName)
        }
    }

    @Test
    fun testMppResolvesJvmAndJsKtLibs() {
        val outerProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        val innerJvmProject = Project("simpleProject")
        val innerJsProject = Project("kotlin2JsInternalTest")

        with(outerProject) {
            embedProject(innerJvmProject)
            embedProject(innerJsProject)

            gradleBuildScript().appendText("\n" + """
                dependencies {
                    jvm6Implementation project(':${innerJvmProject.projectName}')
                    jvm6TestRuntime project(':${innerJvmProject.projectName}')
                    nodeJsImplementation project(':${innerJsProject.projectName}')
                    nodeJsTestRuntime project(':${innerJsProject.projectName}')
                }
            """.trimIndent())

            testResolveAllConfigurations(innerJvmProject.projectName)
        }
    }

    @Test
    fun testJvmKtAppDependsOnMppTestRuntime() {
        val outerProject = Project("sample-lib", gradleVersion, "new-mpp-lib-and-app")
        val innerProject = Project("simpleProject")

        with(outerProject) {
            embedProject(innerProject)

            gradleBuildScript(innerProject.projectName).appendText(
                "\ndependencies { testCompile project(path: ':', configuration: 'jvm6TestRuntime') }"
            )

            testResolveAllConfigurations(innerProject.projectName) {
                assertContains(">> :${innerProject.projectName}:testCompile --> sample-lib-jvm6-1.0.jar")
                assertContains(">> :${innerProject.projectName}:testRuntime --> sample-lib-jvm6-1.0.jar")
            }
        }
    }

    @Test
    fun testKtAppResolvesOldMpp() {
        val outerProject = Project("multiplatformProject")
        val innerJvmProject = Project("simpleProject")
        val innerJsProject = Project("kotlin2JsInternalTest")

        with(outerProject) {
            embedProject(innerJvmProject)
            embedProject(innerJsProject)

            listOf(innerJvmProject to ":libJvm", innerJsProject to ":libJs").forEach { (project, dependency) ->
                gradleBuildScript(project.projectName).appendText(
                    "\n" + """
                        configurations.create('foo')
                        dependencies {
                            foo project('$dependency')
                            compile project('$dependency')
                            foo project(':lib')
                            compile project(':lib')
                        }
                    """.trimIndent()
                )

                testResolveAllConfigurations(project.projectName)
            }
        }
    }

    private fun Project.embedProject(other: Project) {
        setupWorkingDir()
        other.setupWorkingDir()
        other.projectDir.copyRecursively(projectDir.resolve(other.projectName))
        projectDir.resolve("settings.gradle").appendText("\ninclude '${other.projectName}'")
    }
}