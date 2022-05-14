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

package org.jetbrains.kotlin.idea.completion.smart

import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.kotlin.idea.completion.ExpectedInfo
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.kotlin.psi.*
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.completion.InsertionContext
import org.jetbrains.kotlin.idea.completion.handlers.WithTailInsertHandler
import org.jetbrains.kotlin.types.TypeSubstitutor

object KeywordValues {
    public fun addToCollection(collection: MutableCollection<LookupElement>, expectedInfos: Collection<ExpectedInfo>, expressionWithType: JetExpression) {
        var skipTrueFalse = false

        val whenCondition = expressionWithType.getParent() as? JetWhenConditionWithExpression
        if (whenCondition != null) {
            val entry = whenCondition.getParent() as JetWhenEntry
            val whenExpression = entry.getParent() as JetWhenExpression
            if (whenExpression.getElseExpression() == null && entry == whenExpression.getEntries().last) {
                val lookupElement = LookupElementBuilder.create("else").bold().withTailText(" ->")
                collection.add(object: LookupElementDecorator<LookupElement>(lookupElement) {
                    override fun handleInsert(context: InsertionContext) {
                        WithTailInsertHandler("->", spaceBefore = true, spaceAfter = true).handleInsert(context, getDelegate())
                    }
                })
            }
            if (whenExpression.getSubjectExpression() == null) { // no sense in true or false entries for when with no subject
                skipTrueFalse = true
            }
        }

        if (!skipTrueFalse) {
            val booleanInfoClassifier = { (info: ExpectedInfo) ->
                if (info.type == KotlinBuiltIns.getInstance().getBooleanType()) ExpectedInfoClassification.matches(TypeSubstitutor.EMPTY) else ExpectedInfoClassification.notMatches
            }
            collection.addLookupElements(null, expectedInfos, booleanInfoClassifier, { LookupElementBuilder.create("true").bold().assignSmartCompletionPriority(SmartCompletionItemPriority.TRUE) })
            collection.addLookupElements(null, expectedInfos, booleanInfoClassifier, { LookupElementBuilder.create("false").bold().assignSmartCompletionPriority(SmartCompletionItemPriority.FALSE) })
        }

        collection.addLookupElements(null,
                                     expectedInfos,
                                     { info -> if (info.type.isMarkedNullable()) ExpectedInfoClassification.matches(TypeSubstitutor.EMPTY) else ExpectedInfoClassification.notMatches },
                                     { LookupElementBuilder.create("null").bold().assignSmartCompletionPriority(SmartCompletionItemPriority.NULL) })
    }
}
