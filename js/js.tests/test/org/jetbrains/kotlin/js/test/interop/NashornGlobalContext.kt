/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.interop

import jdk.nashorn.internal.runtime.ScriptRuntime


class NashornGlobalContext(private val myState: GlobalRuntimeContext) : InteropGlobalContext {

    override fun updateState(state: Map<String, Any?>) {
        for (key in myState.keys) {
            myState[key] = state[key] ?: ScriptRuntime.UNDEFINED
        }
    }

    override fun toMap(): Map<String, Any?> {
        return myState.toMap()
    }
}