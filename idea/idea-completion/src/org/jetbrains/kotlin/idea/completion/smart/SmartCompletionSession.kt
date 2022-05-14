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

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.idea.completion.*
import org.jetbrains.kotlin.idea.util.CallType
import org.jetbrains.kotlin.load.java.descriptors.SamConstructorDescriptorKindExclude
import org.jetbrains.kotlin.psi.FunctionLiteralArgument
import org.jetbrains.kotlin.psi.JetCodeFragment
import org.jetbrains.kotlin.psi.ValueArgumentName
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.util.DelegatingCall
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

class SmartCompletionSession(configuration: CompletionSessionConfiguration, parameters: CompletionParameters, resultSet: CompletionResultSet)
: CompletionSession(configuration, parameters, resultSet) {

    // we do not include SAM-constructors because they are handled separately and adding them requires iterating of java classes
    override val descriptorKindFilter = DescriptorKindFilter.VALUES exclude SamConstructorDescriptorKindExclude

    private val smartCompletion by lazy(LazyThreadSafetyMode.NONE) {
        expression?.let {
            SmartCompletion(it, resolutionFacade,
                            bindingContext, isVisibleFilter, prefixMatcher, originalSearchScope,
                            toFromOriginalFileMapper, lookupElementFactory)
        }
    }

    override val expectedInfos: Collection<ExpectedInfo>
        get() = smartCompletion?.expectedInfos ?: emptyList()

    override fun doComplete() {
        if (nameExpression != null && NamedArgumentCompletion.isOnlyNamedArgumentExpected(nameExpression)) {
            NamedArgumentCompletion.complete(collector, expectedInfos)
            return
        }

        if (expression != null) {
            addFunctionLiteralArgumentCompletions()

            val (additionalItems, inheritanceSearcher) = smartCompletion!!.additionalItems()
            collector.addElements(additionalItems)

            if (nameExpression != null) {
                val filter = smartCompletion!!.descriptorFilter
                if (filter != null) {
                    referenceVariants.forEach { collector.addElements(filter(it)) }
                    flushToResultSet()

                    processNonImported { collector.addElements(filter(it), notImported = true) }
                    flushToResultSet()

                    if (position.getContainingFile() is JetCodeFragment) {
                        getRuntimeReceiverTypeReferenceVariants().forEach {
                            collector.addElements(filter(it).map { it.withReceiverCast() })
                        }
                        flushToResultSet()
                    }
                }

                // it makes no sense to search inheritors if there is no reference because it means that we have prefix like "this@"
                inheritanceSearcher?.search({ prefixMatcher.prefixMatches(it) }) {
                    collector.addElement(it)
                    flushToResultSet()
                }
            }
        }
    }

    // special completion for outside parenthesis lambda argument
    private fun addFunctionLiteralArgumentCompletions() {
        if (nameExpression != null) {
            val (receiverExpression, callType) = ReferenceVariantsHelper.getExplicitReceiverData(nameExpression) ?: return
            if (callType == CallType.INFIX) {
                val call = receiverExpression.getCall(bindingContext)
                if (call != null && call.getFunctionLiteralArguments().isEmpty()) {
                    val dummyArgument = object : FunctionLiteralArgument {
                        override fun getFunctionLiteral() = throw UnsupportedOperationException()
                        override fun getArgumentExpression() = throw UnsupportedOperationException()
                        override fun getArgumentName(): ValueArgumentName? = null
                        override fun isNamed() = false
                        override fun asElement() = throw UnsupportedOperationException()
                        override fun getSpreadElement(): LeafPsiElement? = null
                        override fun isExternal() = false
                    }
                    val dummyArguments = call.getValueArguments() + listOf(dummyArgument)
                    val dummyCall = object : DelegatingCall(call) {
                        override fun getValueArguments() = dummyArguments
                        override fun getFunctionLiteralArguments() = listOf(dummyArgument)
                        override fun getValueArgumentList() = throw UnsupportedOperationException()
                    }

                    val expectedInfos = ExpectedInfos(bindingContext, resolutionFacade)
                            .calculateForArgument(dummyCall, dummyArgument)
                    collector.addElements(LambdaItems.collect(expectedInfos))
                }
            }
        }
    }

    private fun processNonImported(processor: (DeclarationDescriptor) -> Unit) {
        getTopLevelExtensions().forEach(processor)

        flushToResultSet()

        if (shouldRunTopLevelCompletion()) {
            getTopLevelCallables().forEach(processor)
        }
    }

    override fun createSorter(): CompletionSorter {
        return super.createSorter()
                .weighBefore(KindWeigher.toString(), NameSimilarityWeigher, SmartCompletionPriorityWeigher)
    }
}