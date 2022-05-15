/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("JsIdePlatformUtil")
package org.jetbrains.kotlin.platform.impl

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms

object JsIdePlatformKind : IdePlatformKind<JsIdePlatformKind>() {

    override fun platformByCompilerArguments(arguments: CommonCompilerArguments): TargetPlatform? {
        return if (arguments is K2JSCompilerArguments)
            JsPlatforms.defaultJsPlatform
        else
            null
    }

    override val platforms get() = listOf(JsPlatforms.defaultJsPlatform)
    override val defaultPlatform get() = JsPlatforms.defaultJsPlatform

    override fun createArguments(): CommonCompilerArguments {
        return K2JSCompilerArguments()
    }

    override val argumentsClass get() = K2JSCompilerArguments::class.java

    override val name get() = "JavaScript"
}

val IdePlatformKind<*>?.isJavaScript
    get() = this is JsIdePlatformKind
