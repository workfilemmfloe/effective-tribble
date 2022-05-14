/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.ast


open class Identifier(
        val name: String,
        val myNullable: Boolean = true,
        val quotingNeeded: Boolean = true
) : Expression() {
    override fun isEmpty() = name.length() == 0

    private open fun ifNeedQuote(): String {
        if (quotingNeeded && (ONLY_KOTLIN_KEYWORDS.contains(name)) || name.contains("$")) {
            return quote(name)
        }

        return name
    }

    override fun toKotlin(): String = ifNeedQuote()
    override fun isNullable(): Boolean = myNullable

    class object {
        val Empty = Identifier("")

        private open fun quote(str: String): String {
            return "`" + str + "`"
        }

        val ONLY_KOTLIN_KEYWORDS: Set<String> = setOf(
                "package", "as", "type", "val", "var", "fun", "is", "in", "object", "when", "trait", "This"
        );
    }
}
