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

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.CALL
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.NO_CALL

sealed class DefaultValues(val defaultValue: String, val possibleValues: List<String>? = null) {
    object BooleanFalseDefault : DefaultValues("false")

    object BooleanTrueDefault : DefaultValues("true")

    object StringNullDefault : DefaultValues("null")

    object EmptyList : DefaultValues("emptyList()")

    object LanguageVersions : DefaultValues(
            "null",
            listOf("1.0", "1.1", "1.2", "1.3").map { "\"$it\"" }
    )

    object JvmTargetVersions : DefaultValues(
            "\"" + "1.6" + "\"",
            listOf("1.6", "1.8").map { "\"$it\"" }

    )

    object JsEcmaVersions : DefaultValues(
            "\"v5\"",
            listOf("\"v5\"")
    )

    object JsModuleKinds : DefaultValues(
            "\"plain\"",
            listOf("\"plain\"", "\"amd\"", "\"commonjs\"", "\"umd\"")
    )

    object JsSourceMapContentModes : DefaultValues(
            "\"${K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_INLINING}\"",
            listOf(
                    K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_NEVER,
                    K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_ALWAYS,
                    K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_INLINING
            ).map { "\"$it\""}
    )

    object JsMain : DefaultValues(
            "\"" + CALL + "\"",
            listOf("\"" + CALL + "\"", "\"" + NO_CALL + "\"")
    )
}
