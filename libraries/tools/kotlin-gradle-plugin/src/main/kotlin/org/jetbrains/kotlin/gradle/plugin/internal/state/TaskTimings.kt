/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal.state

internal object TaskTimings : BuildState() {
    class Timing(val nanoTime: Long, val task: String)

    private val timings = HashMap<String, MutableList<Timing>>()

    @Synchronized
    fun allTimings(): Map<String, List<Timing>> = timings

    @Synchronized
    fun <T> measureTime(taskPath: String, desc: String, fn: () -> T): T {
        val start = System.nanoTime()
        val result = fn()
        val end = System.nanoTime()
        val time = end - start
        timings.getOrPut(desc) { ArrayList() }.add(Timing(time, taskPath))
        return result
    }

    @Synchronized
    override fun clear() {
        timings.clear()
    }
}