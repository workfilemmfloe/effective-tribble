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

package org.jetbrains.kotlin.idea.completion.handlers

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.completion.KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY
import org.jetbrains.kotlin.idea.completion.smart.SmartCompletion

class WithTailInsertHandler(val tailText: String,
                            val spaceBefore: Boolean,
                            val spaceAfter: Boolean,
                            val overwriteText: Boolean = true) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        item.handleInsert(context)
        postHandleInsert(context, item)
    }

    fun postHandleInsert(context: InsertionContext, item: LookupElement) {
        val completionChar = context.getCompletionChar()
        if (completionChar == tailText.singleOrNull() || (spaceAfter && completionChar == ' ')) {
            context.setAddCompletionChar(false)
        }
        //TODO: what if completion char is different?

        val document = context.getDocument()
        PsiDocumentManager.getInstance(context.getProject()).doPostponedOperationsAndUnblockDocument(document)

        var tailOffset = context.getTailOffset()
        if (completionChar == Lookup.REPLACE_SELECT_CHAR && item.getUserData(KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY) != null) {
            val offset = context.getOffsetMap().getOffset(SmartCompletion.OLD_ARGUMENTS_REPLACEMENT_OFFSET)
            if (offset != -1) tailOffset = offset
        }

        val moveCaret = context.getEditor().getCaretModel().getOffset() == tailOffset

        //TODO: analyze parenthesis balance to decide whether to replace or not
        var insert = true
        if (overwriteText) {
            var offset = document.charsSequence.skipSpacesAndLineBreaks(tailOffset)
            if (document.isTextAt(offset, tailText)) {
                insert = false
                offset += tailText.length()
                tailOffset = offset

                if (spaceAfter && document.charsSequence.isCharAt(offset, ' ')) {
                    document.deleteString(offset, offset + 1)
                }
            }
        }

        var textToInsert = ""
        if (insert) {
            textToInsert = tailText
            if (spaceBefore) textToInsert = " " + textToInsert
        }
        if (spaceAfter) textToInsert += " "

        document.insertString(tailOffset, textToInsert)

        if (moveCaret) {
            context.getEditor().getCaretModel().moveToOffset(tailOffset + textToInsert.length())

            if (tailText == ",") {
                AutoPopupController.getInstance(context.getProject())?.autoPopupParameterInfo(context.getEditor(), null)
            }
        }
    }

    companion object {
        fun commaTail() = WithTailInsertHandler(",", spaceBefore = false, spaceAfter = true /*TODO: use code style option*/)
        fun rparenthTail() = WithTailInsertHandler(")", spaceBefore = false, spaceAfter = false)
        fun rbraceTail() = WithTailInsertHandler("}", spaceBefore = true, spaceAfter = false)
        fun elseTail() = WithTailInsertHandler("else", spaceBefore = true, spaceAfter = true)
        fun eqTail() = WithTailInsertHandler("=", spaceBefore = true, spaceAfter = true) /*TODO: use code style options*/
        fun spaceTail() = WithTailInsertHandler(" ", spaceBefore = false, spaceAfter = false, overwriteText = false)
        fun colonTail() = WithTailInsertHandler(":", spaceBefore = false, spaceAfter = true)
    }
}
