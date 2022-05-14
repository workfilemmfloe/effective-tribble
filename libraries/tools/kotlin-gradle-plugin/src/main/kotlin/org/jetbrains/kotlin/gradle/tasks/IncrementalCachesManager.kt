package org.jetbrains.kotlin.gradle.tasks

import org.jetbrains.kotlin.incremental.LookupStorage
import org.jetbrains.kotlin.modules.TargetId
import java.io.File

internal class IncrementalCachesManager (
        private val targetId: TargetId,
        private val cacheDirectory: File,
        private val outputDir: File
) {
    private val incrementalCacheDir = File(cacheDirectory, "increCache.${targetId.name}")
    private val lookupCacheDir = File(cacheDirectory, "lookups")
    private var incrementalCacheOpen = false
    private var lookupCacheOpen = false

    val incrementalCache: GradleIncrementalCacheImpl by lazy {
        incrementalCacheOpen = true
        GradleIncrementalCacheImpl(targetDataRoot = incrementalCacheDir.apply { mkdirs() }, targetOutputDir = outputDir, target = targetId)
    }

    val lookupCache: LookupStorage by lazy {
        lookupCacheOpen = true
        LookupStorage(lookupCacheDir.apply { mkdirs() })
    }

    fun clean() {
        close(flush = false)
        cacheDirectory.deleteRecursively()
    }

    fun close(flush: Boolean = false) {
        if (incrementalCacheOpen) {
            if (flush) {
                incrementalCache.flush(false)
            }
            incrementalCache.close()
            incrementalCacheOpen = false
        }

        if (lookupCacheOpen) {
            if (flush) {
                lookupCache.flush(false)
            }
            lookupCache.close()
            lookupCacheOpen = false
        }
    }
}