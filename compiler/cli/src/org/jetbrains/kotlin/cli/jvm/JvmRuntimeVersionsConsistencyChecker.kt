/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.jvm

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.MavenComparableVersion
import java.io.IOException
import java.util.*
import java.util.jar.Attributes
import java.util.jar.Manifest

object JvmRuntimeVersionsConsistencyChecker {
    private val LOG = Logger.getInstance(JvmRuntimeVersionsConsistencyChecker::class.java)

    private fun fatal(message: String): Nothing {
        LOG.error(message)
        throw AssertionError(message)
    }

    private fun <T> T?.assertNotNull(message: () -> String): T =
            if (this == null) fatal(message()) else this

    private val VERSION_ISSUE_SEVERITY = CompilerMessageSeverity.ERROR

    private const val META_INF = "META-INF"
    private const val MANIFEST_MF = "$META_INF/MANIFEST.MF"

    private const val MANIFEST_KOTLIN_VERSION_ATTRIBUTE = "manifest.impl.attribute.kotlin.version"
    private const val MANIFEST_KOTLIN_VERSION_VALUE = "manifest.impl.value.kotlin.version"
    private const val MANIFEST_KOTLIN_RUNTIME_COMPONENT = "manifest.impl.attribute.kotlin.runtime.component"
    private const val MANIFEST_KOTLIN_RUNTIME_COMPONENT_CORE = "manifest.impl.value.kotlin.runtime.component.core"
    private const val MANIFEST_KOTLIN_RUNTIME_COMPONENT_MAIN = "manifest.impl.value.kotlin.runtime.component.main"

    private const val KOTLIN_STDLIB_MODULE = "$META_INF/kotlin-stdlib.kotlin_module"
    private const val KOTLIN_REFLECT_MODULE = "$META_INF/kotlin-reflection.kotlin_module"

    private val RUNTIME_IMPLEMENTATION_TITLES = setOf(
            "kotlin-runtime", "kotlin-stdlib", "kotlin-reflect", "Kotlin Runtime", "Kotlin Standard Library", "Kotlin Reflect"
    )

    private val KOTLIN_VERSION_ATTRIBUTE: String
    private val CURRENT_COMPILER_VERSION: MavenComparableVersion

    private val KOTLIN_RUNTIME_COMPONENT_ATTRIBUTE: String
    private val KOTLIN_RUNTIME_COMPONENT_CORE: String
    private val KOTLIN_RUNTIME_COMPONENT_MAIN: String

    init {
        val manifestProperties: Properties = try {
            JvmRuntimeVersionsConsistencyChecker::class.java
                    .getResourceAsStream("/kotlinManifest.properties")
                    .let { input -> Properties().apply { load(input) } }
        }
        catch (e: Exception) {
            LOG.error(e)
            throw e
        }

        KOTLIN_VERSION_ATTRIBUTE = manifestProperties.getProperty(MANIFEST_KOTLIN_VERSION_ATTRIBUTE)
                .assertNotNull { "$MANIFEST_KOTLIN_VERSION_ATTRIBUTE not found in kotlinManifest.properties" }

        CURRENT_COMPILER_VERSION = run {
            val kotlinVersionString = manifestProperties.getProperty(MANIFEST_KOTLIN_VERSION_VALUE)
                    .assertNotNull { "$MANIFEST_KOTLIN_VERSION_VALUE not found in kotlinManifest.properties" }

            MavenComparableVersion(kotlinVersionString)
        }

        if (CURRENT_COMPILER_VERSION != ApiVersion.LATEST.version) {
            fatal("Kotlin compiler version $CURRENT_COMPILER_VERSION in kotlinManifest.properties doesn't match ${ApiVersion.LATEST}")
        }

        KOTLIN_RUNTIME_COMPONENT_ATTRIBUTE = manifestProperties.getProperty(MANIFEST_KOTLIN_RUNTIME_COMPONENT)
                .assertNotNull { "$MANIFEST_KOTLIN_RUNTIME_COMPONENT not found in kotlinManifest.properties" }
        KOTLIN_RUNTIME_COMPONENT_CORE = manifestProperties.getProperty(MANIFEST_KOTLIN_RUNTIME_COMPONENT_CORE)
                .assertNotNull { "$MANIFEST_KOTLIN_RUNTIME_COMPONENT_CORE not found in kotlinManifest.properties" }
        KOTLIN_RUNTIME_COMPONENT_MAIN = manifestProperties.getProperty(MANIFEST_KOTLIN_RUNTIME_COMPONENT_MAIN)
                .assertNotNull { "$MANIFEST_KOTLIN_RUNTIME_COMPONENT_MAIN not found in kotlinManifest.properties" }
    }

    private class KotlinLibraryFile(val file: VirtualFile, val version: MavenComparableVersion) {
        override fun toString(): String =
                "${file.name}:$version"
    }

    private class RuntimeJarsInfo(
            // Runtime jars with components "Main" and "Core"
            val jars: List<KotlinLibraryFile>,
            // Runtime jars with components "Core" only (a subset of [jars])
            val coreJars: List<KotlinLibraryFile>,
            // Library jars which have some Kotlin Runtime library bundled into them
            val otherLibrariesWithBundledRuntime: List<VirtualFile>
    )

    fun checkCompilerClasspathConsistency(
            messageCollector: MessageCollector,
            languageVersionSettings: LanguageVersionSettings?,
            classpathJarRoots: List<VirtualFile>
    ) {
        val runtimeJarsInfo = collectRuntimeJarsInfo(classpathJarRoots)
        if (runtimeJarsInfo.jars.isEmpty()) return

        val apiVersion = languageVersionSettings?.apiVersion?.version ?: CURRENT_COMPILER_VERSION

        val consistency = checkCompilerClasspathConsistency(messageCollector, apiVersion, runtimeJarsInfo)
        if (consistency != ClasspathConsistency.Consistent) {
            val message = when (consistency) {
                is ClasspathConsistency.InconsistentWithApiVersion ->
                    "Runtime JAR files in the classpath have the version ${consistency.actualRuntimeVersion}, " +
                    "which is older than the API version $apiVersion. " +
                    "Remove them from the classpath or pass '-api-version ${consistency.actualRuntimeVersion}' explicitly. " +
                    "You can also pass '-language-version ${consistency.actualRuntimeVersion}' instead, which will restrict " +
                    "not only the APIs to the specified version, but also the language features. " +
                    "Alternatively, you can use '-Xskip-runtime-version-check' to suppress this error"
                else ->
                    "Some runtime JAR files in the classpath have an incompatible version. " +
                    "Remove them from the classpath or use '-Xskip-runtime-version-check' to suppress errors"
            }
            messageCollector.issue(null, message)
        }

        val librariesWithBundled = runtimeJarsInfo.otherLibrariesWithBundledRuntime
        if (librariesWithBundled.isNotEmpty()) {
            messageCollector.issue(
                    null,
                    "Some JAR files in the classpath have the Kotlin Runtime library bundled into them. " +
                    "This may cause difficult to debug problems if there's a different version of the Kotlin Runtime library in the classpath. " +
                    "Consider removing these libraries from the classpath or use '-Xskip-runtime-version-check' to suppress this warning",
                    CompilerMessageSeverity.STRONG_WARNING
            )

            for (library in librariesWithBundled) {
                messageCollector.issue(
                        library,
                        "Library has Kotlin runtime bundled into it",
                        CompilerMessageSeverity.STRONG_WARNING
                )
            }
        }
    }

    private sealed class ClasspathConsistency {
        object Consistent : ClasspathConsistency()
        class InconsistentWithApiVersion(val actualRuntimeVersion: MavenComparableVersion) : ClasspathConsistency()
        object InconsistentWithCompilerVersion : ClasspathConsistency()
        object InconsistentBecauseOfRuntimesWithDifferentVersions : ClasspathConsistency()
    }

    private fun checkCompilerClasspathConsistency(
            messageCollector: MessageCollector,
            apiVersion: MavenComparableVersion,
            runtimeJarsInfo: RuntimeJarsInfo
    ): ClasspathConsistency {
        // The "Core" jar files should not be newer than the compiler. This behavior is reserved for the future if we realise that we're
        // going to break language/library compatibility in such a way that it's easier to make the old compiler just report an error
        // in the case the new runtime library is specified in the classpath, rather than employing any other compatibility breakage tools
        // we have at our disposal (Deprecated, SinceKotlin, SinceKotlinInfo in metadata, etc.)
        if (runtimeJarsInfo.coreJars.map {
            checkNotNewerThanCompiler(messageCollector, it)
        }.any { it }) return ClasspathConsistency.InconsistentWithCompilerVersion

        val jars = runtimeJarsInfo.jars
        if (jars.isEmpty()) return ClasspathConsistency.Consistent

        val runtimeVersion = checkMatchingVersionsAndGetRuntimeVersion(messageCollector, jars)
                             ?: return ClasspathConsistency.InconsistentBecauseOfRuntimesWithDifferentVersions

        if (jars.map {
            checkCompatibleWithApiVersion(messageCollector, it, apiVersion)
        }.any { it }) return ClasspathConsistency.InconsistentWithApiVersion(runtimeVersion)

        return ClasspathConsistency.Consistent
    }

    private fun checkNotNewerThanCompiler(messageCollector: MessageCollector, jar: KotlinLibraryFile): Boolean {
        if (jar.version > CURRENT_COMPILER_VERSION) {
            messageCollector.issue(jar.file, "Runtime JAR file has version ${jar.version} which is newer than compiler version $CURRENT_COMPILER_VERSION")
            return true
        }
        return false
    }

    private fun checkCompatibleWithApiVersion(
            messageCollector: MessageCollector, jar: KotlinLibraryFile, apiVersion: MavenComparableVersion
    ): Boolean {
        if (jar.version < apiVersion) {
            messageCollector.issue(jar.file, "Runtime JAR file has version ${jar.version} which is older than required for API version $apiVersion")
            return true
        }
        return false
    }

    // Returns the version if it's the same across all jars, or null if versions of some jars differ.
    private fun checkMatchingVersionsAndGetRuntimeVersion(
            messageCollector: MessageCollector,
            jars: List<KotlinLibraryFile>
    ): MavenComparableVersion? {
        assert(jars.isNotEmpty()) { "'jars' must not be empty" }
        val oldestVersion = jars.minBy { it.version }!!.version
        val newestVersion = jars.maxBy { it.version }!!.version

        // If the oldest version is the same as the newest version, then all jars have the same version
        if (oldestVersion == newestVersion) return oldestVersion

        messageCollector.issue(null, buildString {
            appendln("Runtime JAR files in the classpath must have the same version. These files were found in the classpath:")
            for (jar in jars) {
                appendln("    ${jar.file.path} (version ${jar.version})")
            }
        }.trimEnd())

        return null
    }

    private fun MessageCollector.issue(file: VirtualFile?, message: String, severity: CompilerMessageSeverity = VERSION_ISSUE_SEVERITY) {
        report(severity, message, CompilerMessageLocation.create(file?.let(VfsUtilCore::virtualToIoFile)?.path))
    }

    private fun collectRuntimeJarsInfo(classpathJarRoots: List<VirtualFile>): RuntimeJarsInfo {
        val jars = ArrayList<KotlinLibraryFile>(2)
        val coreJars = ArrayList<KotlinLibraryFile>(2)
        val otherLibrariesWithBundledRuntime = ArrayList<VirtualFile>(0)

        for (jarRoot in classpathJarRoots) {
            val fileKind = determineFileKind(jarRoot)
            if (fileKind is FileKind.Irrelevant) continue

            val jarFile = VfsUtilCore.getVirtualFileForJar(jarRoot) ?: continue
            when (fileKind) {
                is FileKind.Runtime -> {
                    val file = KotlinLibraryFile(jarFile, fileKind.version)
                    jars.add(file)
                    if (fileKind.isCoreComponent) {
                        coreJars.add(file)
                    }
                }
                FileKind.OldRuntime -> jars.add(KotlinLibraryFile(jarFile, ApiVersion.KOTLIN_1_0.version))
                FileKind.LibraryWithBundledRuntime -> otherLibrariesWithBundledRuntime.add(jarFile)
            }
        }

        return RuntimeJarsInfo(jars, coreJars, otherLibrariesWithBundledRuntime)
    }

    private sealed class FileKind {
        class Runtime(val version: MavenComparableVersion, val isCoreComponent: Boolean) : FileKind()

        // Runtime library of Kotlin 1.0
        object OldRuntime : FileKind()

        object LibraryWithBundledRuntime : FileKind()

        object Irrelevant : FileKind()
    }

    private fun determineFileKind(jarRoot: VirtualFile): FileKind {
        val manifestFile = jarRoot.findFileByRelativePath(MANIFEST_MF)
        val manifest = try {
            manifestFile?.let { Manifest(it.inputStream) }
        }
        catch (e: IOException) {
            return FileKind.Irrelevant
        }

        val runtimeComponent = manifest?.mainAttributes?.getValue(KOTLIN_RUNTIME_COMPONENT_ATTRIBUTE)
        return when (runtimeComponent) {
            KOTLIN_RUNTIME_COMPONENT_MAIN ->
                FileKind.Runtime(manifest.getKotlinLanguageVersion(), isCoreComponent = false)
            KOTLIN_RUNTIME_COMPONENT_CORE ->
                FileKind.Runtime(manifest.getKotlinLanguageVersion(), isCoreComponent = true)
            null -> when {
                jarRoot.findFileByRelativePath(KOTLIN_STDLIB_MODULE) == null &&
                jarRoot.findFileByRelativePath(KOTLIN_REFLECT_MODULE) == null -> FileKind.Irrelevant
                isGenuineKotlinRuntime(manifest) -> FileKind.OldRuntime
                else -> FileKind.LibraryWithBundledRuntime
            }
            else -> FileKind.Irrelevant
        }
    }

    // Returns true if the manifest is from the original Kotlin Runtime jar, false if it's from a library with a bundled runtime
    private fun isGenuineKotlinRuntime(manifest: Manifest?): Boolean {
        return manifest != null &&
               manifest.mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE) in RUNTIME_IMPLEMENTATION_TITLES
    }

    private fun Manifest.getKotlinLanguageVersion(): MavenComparableVersion =
            (mainAttributes.getValue(KOTLIN_VERSION_ATTRIBUTE)?.let((ApiVersion)::parse) ?: ApiVersion.KOTLIN_1_0).version
}
