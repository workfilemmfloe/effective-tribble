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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.kotlin.idea.JetIcons
import org.jetbrains.kotlin.idea.completion.handlers.WithTailInsertHandler
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetCallElement
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.types.JetType
import java.util.*

object NamedArgumentCompletion {
    public fun isOnlyNamedArgumentExpected(nameExpression: JetSimpleNameExpression): Boolean {
        val thisArgument = nameExpression.parent as? JetValueArgument ?: return false
        if (thisArgument.isNamed()) return false

        val callElement = thisArgument.getStrictParentOfType<JetCallElement>() ?: return false

        return callElement.valueArguments
                .takeWhile { it != thisArgument }
                .any { it.isNamed() }
    }

    public fun complete(collector: LookupElementsCollector, expectedInfos: Collection<ExpectedInfo>) {
        val nameToParameterType = HashMap<Name, MutableSet<JetType>>()
        for (expectedInfo in expectedInfos) {
            val argumentData = expectedInfo.additionalData as? ArgumentPositionData.Positional ?: continue
            for (parameter in argumentData.namedArgumentCandidates) {
                nameToParameterType.getOrPut(parameter.name) { HashSet() }.add(parameter.type)
            }
        }

        for ((name, types) in nameToParameterType) {
            val typeText = types.singleOrNull()?.let { DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(it) } ?: "..."
            val nameString = name.asString()
            val lookupElement = LookupElementBuilder.create(nameString)
                    .withPresentableText("$nameString =")
                    .withTailText(" $typeText")
                    .withIcon(JetIcons.PARAMETER)
                    .withInsertHandler(NamedArgumentInsertHandler(name))
                    .assignPriority(ItemPriority.NAMED_PARAMETER)
            collector.addElement(lookupElement)
        }
    }

    private class NamedArgumentInsertHandler(private val parameterName: Name) : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            val editor = context.getEditor()
            val text = parameterName.render()
            editor.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), text)
            editor.getCaretModel().moveToOffset(context.getStartOffset() + text.length())

            WithTailInsertHandler.eqTail().postHandleInsert(context, item)
        }
    }
}
