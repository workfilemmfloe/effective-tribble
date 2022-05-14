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

package org.jetbrains.kotlin.effectsystem.resolving.utility

import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.effectsystem.adapters.ValueIdsFactory
import org.jetbrains.kotlin.effectsystem.functors.IsFunctor
import org.jetbrains.kotlin.effectsystem.impls.*
import org.jetbrains.kotlin.effectsystem.structure.ESBooleanExpression
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.IdentifierInfo
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ConditionParser {
    private val constantsParser = ConstantsParser()

    private val CONDITION_JOINING_ANNOTATION = FqName("kotlin.internal.JoinConditions")

    private val EQUALS_CONDITION = FqName("kotlin.internal.Equals")
    private val IS_INSTANCE_CONDITION = FqName("kotlin.internal.IsInstance")
    private val NOT_CONDITION = FqName("kotlin.internal.Not")

    fun parseCondition(resolvedCall: ResolvedCall<*>): ESBooleanExpression? {
        val joiner = getJoiner(resolvedCall)

        val primitiveConditions = mutableListOf<ESBooleanExpression>()
        resolvedCall.resultingDescriptor.valueParameters.flatMapTo(primitiveConditions) { getConditionsOnArgument(it) }

        val conditionOnReceiver = resolvedCall.resultingDescriptor.extensionReceiverParameter?.let { getConditionsOnReceiver(it) } ?: emptyList()
        primitiveConditions.addAll(conditionOnReceiver)

        if (primitiveConditions.isEmpty()) return null

        return joiner.join(primitiveConditions)
    }

    private fun getConditionsOnReceiver(receiverParameter: ReceiverParameterDescriptor): List<ESBooleanExpression> {
        val variable = receiverParameter.extensionReceiverToESVariable()

        val isNegated = receiverParameter.type.annotations.getAllAnnotations().any { it.annotation.annotationClass?.fqNameSafe == NOT_CONDITION }

        return receiverParameter.type.annotations.getAllAnnotations().mapNotNull {
            when (it.annotation.annotationClass?.fqNameSafe) {
                EQUALS_CONDITION -> ESEqual(variable, it.annotation.allValueArguments.values.single().toESConstant() ?: return@mapNotNull null, isNegated)
                IS_INSTANCE_CONDITION -> ESIs(variable, IsFunctor(it.annotation.allValueArguments.values.single().value as KotlinType, isNegated))
                else -> null
            }
        }
    }

    private fun getJoiner(resolvedCall: ResolvedCall<*>): Joiner {
        val joiningStrategyName = resolvedCall.resultingDescriptor.annotations
                .findAnnotation(CONDITION_JOINING_ANNOTATION)?.allValueArguments?.values?.single()?.safeAs<EnumValue>()?.value?.name?.identifier
        return when (joiningStrategyName) {
            "ANY" -> Joiner.ANY
            "NONE" -> Joiner.NONE
            "ALL" -> Joiner.ALL
            else -> Joiner.ALL
        }
    }

    private fun getConditionsOnArgument(parameterDescriptor: ValueParameterDescriptor): List<ESBooleanExpression> {
        val parameterVariable = parameterDescriptor.toESVariable()

        val isNegated = parameterDescriptor.annotations.findAnnotation(NOT_CONDITION) != null

        return parameterDescriptor.annotations.mapNotNull {
            when (it.annotationClass?.fqNameSafe) {
                EQUALS_CONDITION -> ESEqual(parameterVariable, it.allValueArguments.values.single().toESConstant() ?: return@mapNotNull null, isNegated)
                IS_INSTANCE_CONDITION -> ESIs(parameterVariable, IsFunctor(it.allValueArguments.values.single().value as KotlinType, isNegated))
                else -> null
            }
        }
    }

    private enum class Joiner {
        ALL {
            override fun join(conditions: List<ESBooleanExpression>): ESBooleanExpression =
                    conditions.reduce { acc, expr -> acc.and(expr) }
        },

        NONE {
            override fun join(conditions: List<ESBooleanExpression>): ESBooleanExpression =
                    conditions.map { it.not() }.reduce { acc, expr -> acc.and(expr) }
        },

        ANY {
            override fun join(conditions: List<ESBooleanExpression>): ESBooleanExpression =
                    conditions.reduce { acc, expr -> acc.or(expr) }
        };

        abstract fun join(conditions: List<ESBooleanExpression>): ESBooleanExpression
    }

    private fun ConstantValue<*>.toESConstant(): ESConstant? = constantsParser.parseConstantValue(this)
}