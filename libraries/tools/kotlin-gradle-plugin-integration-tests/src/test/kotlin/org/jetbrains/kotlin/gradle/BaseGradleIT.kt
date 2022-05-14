package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.gradle.util.checkBytecodeNotContains
import org.jetbrains.kotlin.gradle.util.createGradleCommand
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Before
import java.io.File
import java.util.regex.Pattern
import kotlin.test.*

val SYSTEM_LINE_SEPARATOR: String = System.getProperty("line.separator")

abstract class BaseGradleIT {

    protected var workingDir = File(".")

    protected open fun defaultBuildOptions(): BuildOptions = BuildOptions(withDaemon = true)

    @Before
    fun setUp() {
        workingDir = FileUtil.createTempDirectory("BaseGradleIT", null)
        acceptAndroidSdkLicenses()
    }

    @After
    fun tearDown() {
        workingDir.deleteRecursively()
    }

    // https://developer.android.com/studio/intro/update.html#download-with-gradle
    fun acceptAndroidSdkLicenses() = defaultBuildOptions().androidHome?.let {
        val sdkLicenses = File(it, "licenses")
        sdkLicenses.mkdirs()

        val sdkLicense = File(sdkLicenses, "android-sdk-license")
        if (!sdkLicense.exists()) {
            sdkLicense.createNewFile()
            sdkLicense.writeText("8933bad161af4178b1185d1a37fbf41ea5269c55")
        }

        val sdkPreviewLicense = File(sdkLicenses, "android-sdk-preview-license")
        if (!sdkPreviewLicense.exists()) {
            sdkPreviewLicense.writeText("84831b9409646a918e30573bab4c9c91346d8abd")
        }
    }

    companion object {

        @JvmStatic
        protected val ranDaemonVersions = hashMapOf<String, Int>()

        val resourcesRootFile = File("src/test/resources")
        val MAX_DAEMON_RUNS = 30

        fun getEnvJDK_18() = System.getenv()["JDK_18"]

        @AfterClass
        @JvmStatic
        @Synchronized
        @Suppress("unused")
        fun tearDownAll() {
            // Latest gradle requires Java > 7
            val environmentVariables = hashMapOf<String, String>()
            getEnvJDK_18()?.let { environmentVariables["JAVA_HOME"] = it }

            ranDaemonVersions.keys.forEach { stopDaemon(it, environmentVariables) }
            ranDaemonVersions.clear()
        }

        fun stopDaemon(ver: String, environmentVariables: Map<String, String> = mapOf()) {
            println("Stopping gradle daemon v$ver")
            val wrapperDir = File(resourcesRootFile, "GradleWrapper-$ver")
            val cmd = createGradleCommand(arrayListOf("-stop"))
            val result = runProcess(cmd, wrapperDir, environmentVariables)
            assert(result.isSuccessful) { "Could not stop daemon: $result" }
        }

        @Synchronized
        fun prepareDaemon(version: String, environmentVariables: Map<String, String> = mapOf()) {
            val useCount = ranDaemonVersions.get(version)
            if (useCount == null || useCount > MAX_DAEMON_RUNS) {
                stopDaemon(version, environmentVariables)
                ranDaemonVersions.put(version, 1)
            }
            else {
                ranDaemonVersions.put(version, useCount + 1)
            }
        }
    }

    // the second parameter is for using with ToolingAPI, that do not like --daemon/--no-daemon  options at all
    data class BuildOptions(
            val withDaemon: Boolean = false,
            val daemonOptionSupported: Boolean = true,
            val incremental: Boolean? = null,
            val androidHome: File? = null,
            val javaHome: File? = null,
            val androidGradlePluginVersion: String? = null,
            val forceOutputToStdout: Boolean = false,
            val debug: Boolean = false,
            val freeCommandLineArgs: List<String> = emptyList(),
            val kotlinVersion: String = KOTLIN_VERSION)

    open inner class Project(
            val projectName: String,
            val wrapperVersion: String,
            directoryPrefix: String? = null,
            val minLogLevel: LogLevel = LogLevel.DEBUG
    ) {
        val resourceDirName = if (directoryPrefix != null) "$directoryPrefix/$projectName" else projectName
        open val resourcesRoot = File(resourcesRootFile, "testProject/$resourceDirName")
        val projectDir = File(workingDir.canonicalFile, projectName)

        open fun setupWorkingDir() {
            copyRecursively(this.resourcesRoot, workingDir)
            copyDirRecursively(File(resourcesRootFile, "GradleWrapper-$wrapperVersion"), projectDir)
        }

        fun relativize(files: Iterable<File>): List<String> =
                files.map { it.relativeTo(projectDir).path }

        fun relativize(vararg files: File): List<String> =
                files.map { it.relativeTo(projectDir).path }

        fun performModifications() {
            for (file in projectDir.walk()) {
                if (!file.isFile) continue

                val fileWithoutExt = File(file.parentFile, file.nameWithoutExtension)

                when (file.extension) {
                    "new" -> {
                        file.copyTo(fileWithoutExt, overwrite = true)
                        file.delete()
                    }
                    "delete" -> {
                        fileWithoutExt.delete()
                        file.delete()
                    }
                }
            }
        }
    }

    class CompiledProject(val project: Project, val output: String, val resultCode: Int) {
        companion object {
            val kotlinSourcesListRegex = Regex("\\[KOTLIN\\] compile iteration: ([^\\r\\n]*)")
            val javaSourcesListRegex = Regex("\\[DEBUG\\] \\[[^\\]]*JavaCompiler\\] Compiler arguments: ([^\\r\\n]*)")
        }

        val compiledKotlinSources: Iterable<File> by lazy { kotlinSourcesListRegex.findAll(output).asIterable().flatMap { it.groups[1]!!.value.split(", ").map { File(project.projectDir, it).canonicalFile } } }
        val compiledJavaSources: Iterable<File> by lazy { javaSourcesListRegex.findAll(output).asIterable().flatMap { it.groups[1]!!.value.split(" ").filter { it.endsWith(".java", ignoreCase = true) }.map { File(it).canonicalFile } } }
    }

    // Basically the same as `Project.build`, tells gradle to wait for debug on 5005 port
    // Faster to type than `project.build("-Dorg.gradle.debug=true")` or `project.build(options = defaultBuildOptions().copy(debug = true))`
    fun Project.debug(vararg params: String, options: BuildOptions = defaultBuildOptions(), check: CompiledProject.() -> Unit) {
        build(*params, options = options.copy(debug = true), check = check)
    }

    fun Project.build(vararg params: String, options: BuildOptions = defaultBuildOptions(), check: CompiledProject.() -> Unit) {
        val cmd = createBuildCommand(params, options)
        val env = createEnvironmentVariablesMap(options)

        if (options.withDaemon) {
            prepareDaemon(wrapperVersion, env)
        }

        println("<=== Test build: ${this.projectName} $cmd ===>")

        val projectDir = File(workingDir, projectName)
        if (!projectDir.exists()) {
            setupWorkingDir()
        }

        val result = runProcess(cmd, projectDir, env, options)
        try {
            CompiledProject(this, result.output, result.exitCode).check()
        }
        catch (t: Throwable) {
            // to prevent duplication of output
            if (!options.forceOutputToStdout) {
                System.out.println(result.output)
            }
            throw t
        }
    }

    fun CompiledProject.assertSuccessful() {
        if (resultCode == 0) return

        val errors = "(?m)^.*\\[ERROR] \\[\\S+] (.*)$".toRegex().findAll(output)
        val errorMessage = buildString {
            appendln("Gradle build failed")
            appendln()
            if (errors.any()) {
                appendln("Possible errors:")
                errors.forEach { match -> appendln(match.groupValues[1]) }
            }
        }
        fail(errorMessage)
    }

    fun CompiledProject.assertFailed(): CompiledProject {
        assertNotEquals(0, resultCode, "Expected that Gradle build failed")
        return this
    }

    fun CompiledProject.assertContains(vararg expected: String): CompiledProject {
        for (str in expected) {
            assertTrue(output.contains(str.normalize()), "Output should contain '$str'")
        }
        return this
    }

    fun CompiledProject.assertContainsRegex(expected: Regex): CompiledProject {
        assertTrue(expected.containsMatchIn(output), "Output should contain pattern '$expected'")
        return this
    }

    fun CompiledProject.assertClassFilesNotContain(dir: File, vararg strings: String) {
        val classFiles = dir.walk().filter { it.isFile && it.extension.toLowerCase() == "class" }

        for (cf in classFiles) {
            checkBytecodeNotContains(cf, strings.toList())
        }
    }

    fun CompiledProject.assertSubstringCount(substring: String, expectedCount: Int) {
        val actualCount = Pattern.quote(substring).toRegex().findAll(output).count()
        assertEquals(expectedCount, actualCount, "Number of occurrences in output for substring '$substring'")
    }

    fun CompiledProject.checkKotlinGradleBuildServices() {
        assertSubstringCount("Initialized KotlinGradleBuildServices", expectedCount = 1)
        assertSubstringCount("Disposed KotlinGradleBuildServices", expectedCount = 1)
    }

    fun CompiledProject.assertNotContains(vararg expected: String): CompiledProject {
        for (str in expected) {
            assertFalse(output.contains(str.normalize()), "Output should not contain '$str'")
        }
        return this
    }

    fun CompiledProject.assertNotContains(regex: Regex) {
        assertNull(regex.find(output), "Output should not contain '$regex'")
    }

    fun CompiledProject.assertNoWarnings() {
        val warnings = "w: .*$".toRegex().findAll(output).map { it.groupValues[0] }

        if (warnings.any()) {
            val message = (listOf("Output should not contain any warnings:") + warnings).joinToString(SYSTEM_LINE_SEPARATOR)
            throw IllegalStateException(message)
        }
    }

    fun CompiledProject.fileInWorkingDir(path: String) = File(File(workingDir, project.projectName), path)

    fun CompiledProject.assertReportExists(pathToReport: String = ""): CompiledProject {
        assertTrue(fileInWorkingDir(pathToReport).exists(), "The report [$pathToReport] does not exist.")
        return this
    }

    fun CompiledProject.assertFileExists(path: String = ""): CompiledProject {
        assertTrue(fileInWorkingDir(path).exists(), "The file [$path] does not exist.")
        return this
    }

    fun CompiledProject.assertNoSuchFile(path: String = ""): CompiledProject {
        assertFalse(fileInWorkingDir(path).exists(), "The file [$path] exists.")
        return this
    }

    fun CompiledProject.assertFileContains(path: String, vararg expected: String): CompiledProject {
        val text = fileInWorkingDir(path).readText()
        expected.forEach {
            assertTrue(text.contains(it), "$path should contain '$it', actual file contents:\n$text")
        }
        return this
    }

    private fun Iterable<File>.projectRelativePaths(project: Project): Iterable<String> {
        return map { it.canonicalFile.toRelativeString(project.projectDir) }
    }

    fun CompiledProject.assertSameFiles(expected: Iterable<String>, actual: Iterable<String>, messagePrefix: String): CompiledProject {
        val expectedSet = expected.map { it.normalizePath() }.toSortedSet().joinToString("\n")
        val actualSet = actual.map { it.normalizePath() }.toSortedSet().joinToString("\n")
        Assert.assertEquals(messagePrefix, expectedSet, actualSet)
        return this
    }

    fun CompiledProject.assertContainFiles(expected: Iterable<String>, actual: Iterable<String>, messagePrefix: String = ""): CompiledProject {
        val expectedNormalized = expected.map(FileUtil::normalize).toSortedSet()
        val actualNormalized = actual.map(FileUtil::normalize).toSortedSet()
        assertTrue(actualNormalized.containsAll(expectedNormalized), messagePrefix + "expected files: ${expectedNormalized.joinToString()}\n  !in actual files: ${actualNormalized.joinToString()}")
        return this
    }

    fun CompiledProject.assertCompiledKotlinSources(sources: Iterable<String>, weakTesting: Boolean = false): CompiledProject =
            if (weakTesting)
                assertContainFiles(sources, compiledKotlinSources.projectRelativePaths(this.project), "Compiled Kotlin files differ:\n  ")
            else
                assertSameFiles(sources, compiledKotlinSources.projectRelativePaths(this.project), "Compiled Kotlin files differ:\n  ")

    fun CompiledProject.assertCompiledJavaSources(sources: Iterable<String>, weakTesting: Boolean = false): CompiledProject =
            if (weakTesting)
                assertContainFiles(sources, compiledJavaSources.projectRelativePaths(this.project), "Compiled Java files differ:\n  ")
            else
                assertSameFiles(sources, compiledJavaSources.projectRelativePaths(this.project), "Compiled Java files differ:\n  ")

    private fun Project.createBuildCommand(params: Array<out String>, options: BuildOptions): List<String> =
            createGradleCommand(createGradleTailParameters(options, params))

    private fun Project.createGradleTailParameters(options: BuildOptions, params: Array<out String> = arrayOf()): List<String> =
            params.toMutableList().apply {
                add("--stacktrace")
                add("--${minLogLevel.name.toLowerCase()}")
                if (options.daemonOptionSupported) {
                    add(if (options.withDaemon) "--daemon" else "--no-daemon")
                }

                add("-Pkotlin_version=" + options.kotlinVersion)
                options.incremental?.let { add("-Pkotlin.incremental=$it") }
                options.androidGradlePluginVersion?.let { add("-Pandroid_tools_version=$it")}
                if (options.debug) {
                    add("-Dorg.gradle.debug=true")
                }
                addAll(options.freeCommandLineArgs)
            }

    private fun createEnvironmentVariablesMap(options: BuildOptions): Map<String, String> =
            hashMapOf<String, String>().apply {
                options.androidHome?.let { sdkDir ->
                    sdkDir.parentFile.mkdirs()
                    put("ANDROID_HOME", sdkDir.canonicalPath)
                }

                options.javaHome?.let {
                    put("JAVA_HOME", it.canonicalPath)
                }
            }

    private fun String.normalize() = this.lineSequence().joinToString(SYSTEM_LINE_SEPARATOR)

    fun copyRecursively(source: File, target: File) {
        assertTrue(target.isDirectory)
        val targetFile = File(target, source.name)
        if (source.isDirectory) {
            targetFile.mkdir()
            source.listFiles()?.forEach { copyRecursively(it, targetFile) }
        } else {
            source.copyTo(targetFile)
        }
    }

    fun copyDirRecursively(source: File, target: File) {
        assertTrue(source.isDirectory)
        assertTrue(target.isDirectory)
        source.listFiles()?.forEach { copyRecursively(it, target) }
    }

    private fun String.normalizePath() = replace("\\", "/")
}