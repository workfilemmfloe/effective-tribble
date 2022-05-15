/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.js

import java.io.File
import java.io.Serializable

interface JsIncrementalCompilationSettings {
    val ignoreLookupsFrom: Set<File>
}

data class JsIncrementalCompilationSettingsImpl(
    override val ignoreLookupsFrom: Set<File>
) : JsIncrementalCompilationSettings, Serializable {
    companion object {
        const val serialVersionUID: Long = 0
    }
}