/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.core.refactoring

import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.lexer.JetKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens

public class KotlinNamesValidator : NamesValidator {
    val KEYWORD_SET = JetTokens.KEYWORDS.getTypes().filterIsInstance<JetKeywordToken>().map { it.getValue() }.toHashSet()

    override fun isKeyword(name: String, project: Project?): Boolean = name in KEYWORD_SET
    override fun isIdentifier(name: String, project: Project?): Boolean = JetNameSuggester.isIdentifier(name)
}
