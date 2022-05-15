/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal.state

internal abstract class BuildState {
    init {
        register(this)
    }

    abstract fun clear()

    companion object {
        private val states = ArrayList<BuildState>()

        @Synchronized
        fun register(state: BuildState) {
            states.add(state)
        }

        @Synchronized
        fun clear() {
            states.forEach { it.clear() }
        }
    }
}