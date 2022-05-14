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

package org.jetbrains.kotlin.idea.editor.fixers

import com.intellij.lang.SmartEnterProcessorWithFixers
import org.jetbrains.kotlin.idea.editor.KotlinSmartEnterHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.JetWhenExpression

public class KotlinMissingWhenBodyFixer : SmartEnterProcessorWithFixers.Fixer<KotlinSmartEnterHandler>() {
    override fun apply(editor: Editor, processor: KotlinSmartEnterHandler, element: PsiElement) {
        if (element !is JetWhenExpression) return
        val whenExpression = element as JetWhenExpression

        val doc = editor.getDocument()

        val openBrace = whenExpression.getOpenBrace()
        val closeBrace = whenExpression.getCloseBrace()

        if (openBrace == null && closeBrace == null && whenExpression.getEntries().isEmpty()) {
            val openBraceAfter = whenExpression.insertOpenBraceAfter()
            if (openBraceAfter != null) {
                doc.insertString(openBraceAfter.range.end, "{}")
            }
        }
    }

    fun JetWhenExpression.insertOpenBraceAfter(): PsiElement? = when {
        getRightParenthesis() != null -> getRightParenthesis()
        getSubjectExpression() != null -> null
        getLeftParenthesis() != null -> null
        else -> getWhenKeyword()
    }
}
