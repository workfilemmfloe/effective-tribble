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

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.js.*
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.js.JsProtoBuf
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import org.jetbrains.kotlin.utils.LibraryUtils
import java.io.File

fun makeJsIncrementally(
        cachesDir: File,
        sourceRoots: Iterable<File>,
        args: K2JSCompilerArguments,
        messageCollector: MessageCollector = MessageCollector.NONE,
        reporter: ICReporter = EmptyICReporter
) {
    val versions = commonCacheVersions(cachesDir) + standaloneCacheVersion(cachesDir)
    val allKotlinFiles = sourceRoots.asSequence().flatMap { it.walk() }
            .filter { it.isFile && it.extension.equals("kt", ignoreCase = true) }.toList()

    withJsIC {
        val compiler = IncrementalJsCompilerRunner(cachesDir, versions, reporter)
        compiler.compile(allKotlinFiles, args, messageCollector) {
            it.inputsCache.sourceSnapshotMap.compareAndUpdate(allKotlinFiles)
        }
    }
}

inline fun <R> withJsIC(fn: ()->R): R {
    val isJsEnabledBackup = IncrementalCompilation.isEnabledForJs()
    IncrementalCompilation.setIsEnabledForJs(true)

    try {
        return withIC { fn() }
    }
    finally {
        IncrementalCompilation.setIsEnabledForJs(isJsEnabledBackup)
    }
}

class IncrementalJsCompilerRunner(
        workingDir: File,
        cacheVersions: List<CacheVersion>,
        reporter: ICReporter
) : IncrementalCompilerRunner<K2JSCompilerArguments, IncrementalJsCachesManager>(
        workingDir,
        "caches-js",
        cacheVersions,
        reporter,
        artifactChangesProvider = null,
        changesRegistry = null
) {
    // value is null when key is not a js library
    private val librariesCache = hashMapOf<File, JsLibraryProtoMapValue?>()
    private fun readJsLibrary(file: File): JsLibraryProtoMapValue? =
            librariesCache.getOrPut(file) {
                if (!file.exists() || LibraryUtils.isKotlinJavascriptStdLibrary(file)) {
                    null
                }
                else {
                    val modulesList = KotlinJavascriptMetadataUtils.loadMetadata(file)
                    val modules = modulesList.associate { it.moduleName to it.body }
                    JsLibraryProtoMapValue(modules)
                }
            }
    private fun getLibrariesMap(args: K2JSCompilerArguments): Map<File, JsLibraryProtoMapValue> {
        val paths = args.libraries?.split(File.pathSeparator) ?: return emptyMap()
        val libraries = HashMap<File, JsLibraryProtoMapValue>()
        for (path in paths) {
            val file = File(path)
            libraries[file] = readJsLibrary(file) ?: continue
        }
        return libraries
    }

    override fun preBuildHook(args: K2JSCompilerArguments, compilationMode: CompilationMode, caches: IncrementalJsCachesManager) {
        val libraries = getLibrariesMap(args)
        caches.platformCache.updateLibaries(libraries)
    }

    override fun isICEnabled(): Boolean =
        IncrementalCompilation.isEnabled() && IncrementalCompilation.isEnabledForJs()

    override fun createCacheManager(args: K2JSCompilerArguments): IncrementalJsCachesManager =
        IncrementalJsCachesManager(cacheDirectory, reporter)

    override fun destinationDir(args: K2JSCompilerArguments): File =
        File(args.outputFile).parentFile

    override fun calculateSourcesToCompile(caches: IncrementalJsCachesManager, changedFiles: ChangedFiles.Known, args: K2JSCompilerArguments): CompilationMode {
        if (BuildInfo.read(lastBuildInfoFile) == null) return CompilationMode.Rebuild { "No information on previous build" }

        val dirtyFiles = HashSet<File>()
        val libraries = getLibrariesMap(args)
        val librariesChanges = caches.platformCache.compareLibraries(libraries)
        @Suppress("UNUSED_VARIABLE") // To make this 'when' exhaustive
        val unused: Any = when (librariesChanges) {
            is LibrariesChangesEither.Known -> {
                val (dirtyLookupSymbols, dirtyClassFqNames) = librariesChanges.changesCollector.getDirtyData(listOf(caches.platformCache), reporter)
                with (dirtyFiles) {
                    addAll(mapLookupSymbolsToFiles(caches.lookupCache, dirtyLookupSymbols, reporter))
                    addAll(mapClassesFqNamesToFiles(kotlin.collections.listOf(caches.platformCache), dirtyClassFqNames, reporter))
                }
            }
            is LibrariesChangesEither.Unknown -> {
                CompilationMode.Rebuild { librariesChanges.reason }
            }
        }

        dirtyFiles.addAll(getDirtyFiles(changedFiles))
        return CompilationMode.Incremental(dirtyFiles)
    }

    override fun makeServices(
            args: K2JSCompilerArguments,
            lookupTracker: LookupTracker,
            caches: IncrementalJsCachesManager,
            compilationMode: CompilationMode
    ): Services.Builder =
        super.makeServices(args, lookupTracker, caches, compilationMode).apply {
            register(IncrementalResultsConsumer::class.java, IncrementalResultsConsumerImpl())

            if (compilationMode is CompilationMode.Incremental) {
                register(IncrementalDataProvider::class.java, IncrementalDataProviderFromCache(caches.platformCache))
            }
        }

    override fun updateCaches(
            services: Services,
            caches: IncrementalJsCachesManager,
            generatedFiles: List<GeneratedFile>,
            changesCollector: ChangesCollector
    ) {
        val incrementalResults = services.get(IncrementalResultsConsumer::class.java) as IncrementalResultsConsumerImpl

        val jsCache = caches.platformCache
        jsCache.header = incrementalResults.headerMetadata

        return jsCache.compareAndUpdate(incrementalResults, changesCollector)
    }

    override fun runCompiler(
            sourcesToCompile: Set<File>,
            args: K2JSCompilerArguments,
            caches: IncrementalJsCachesManager,
            services: Services,
            messageCollector: MessageCollector
    ): ExitCode {
        val freeArgsBackup = args.freeArgs.toMutableList()

        try {
            sourcesToCompile.mapTo(args.freeArgs) { it.absolutePath }
            val exitCode = K2JSCompiler().exec(messageCollector, services, args)
            reporter.reportCompileIteration(sourcesToCompile, exitCode)
            return exitCode
        }
        finally {
            args.freeArgs = freeArgsBackup
        }
    }
}