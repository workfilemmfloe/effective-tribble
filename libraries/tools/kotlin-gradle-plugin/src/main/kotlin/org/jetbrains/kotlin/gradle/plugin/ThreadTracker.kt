package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logging
import org.junit.Assert
import java.util.HashSet

public class ThreadTracker {
    val log = Logging.getLogger(this.javaClass)
    private var before: Collection<Thread>? = getThreads()

    private fun getThreads(): Collection<Thread> = Thread.getAllStackTraces().keys

    public fun checkThreadLeak(gradle: Gradle?) {
        try {
            val testThreads = gradle != null &&
                    gradle.rootProject.hasProperty("kotlin.gradle.test") &&
                    !gradle.rootProject.hasProperty("kotlin.gradle.noThreadTest")

            Thread.sleep(if (testThreads) 200L else 50L)

            val after = HashSet(getThreads())
            after.removeAll(before!!)

            for (thread in after) {
                if (thread == Thread.currentThread()) continue

                val name = thread.name

                if (testThreads) {
                    throw RuntimeException("Thread leaked: $thread: $name\n ${thread.stackTrace.joinToString(separator = "\n", prefix = " at ")}")
                }
                else {
                    log.info("Thread leaked: $thread: $name")
                }
            }
        } finally {
            before = null
        }
    }
}