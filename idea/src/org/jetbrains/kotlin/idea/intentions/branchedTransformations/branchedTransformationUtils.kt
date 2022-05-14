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

package org.jetbrains.kotlin.idea.intentions.branchedTransformations

import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.psi.patternMatching.matches
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

fun KtWhenCondition.toExpression(subject: KtExpression?): KtExpression {
    val factory = KtPsiFactory(this)
    when (this) {
        is KtWhenConditionIsPattern -> {
            val op = if (isNegated()) "!is" else "is"
            return factory.createExpressionByPattern("$0 $op $1", subject ?: "_", getTypeReference() ?: "")
        }

        is KtWhenConditionInRange -> {
            val op = getOperationReference().getText()
            return factory.createExpressionByPattern("$0 $op $1", subject ?: "_", getRangeExpression() ?: "")
        }

        is KtWhenConditionWithExpression -> {
            return if (subject != null) {
                factory.createExpressionByPattern("$0 == $1", subject, getExpression() ?: "")
            }
            else {
                getExpression()!!
            }
        }

        else -> throw IllegalArgumentException("Unknown JetWhenCondition type: $this")
    }
}

public fun KtWhenExpression.getSubjectToIntroduce(): KtExpression?  {
    if (getSubjectExpression() != null) return null

    var lastCandidate: KtExpression? = null
    for (entry in getEntries()) {
        val conditions = entry.getConditions()
        if (!entry.isElse() && conditions.isEmpty()) return null

        for (condition in conditions) {
            if (condition !is KtWhenConditionWithExpression) return null

            val candidate = condition.getExpression()?.getWhenConditionSubjectCandidate() as? KtNameReferenceExpression ?: return null

            if (lastCandidate == null) {
                lastCandidate = candidate
            }
            else if (!lastCandidate.matches(candidate)) {
                return null
            }

        }
    }

    return lastCandidate
}

private fun KtExpression?.getWhenConditionSubjectCandidate(): KtExpression? {
    return when(this) {
        is KtIsExpression -> getLeftHandSide()

        is KtBinaryExpression -> {
            val lhs = getLeft()
            val op = getOperationToken()
            when (op) {
                KtTokens.IN_KEYWORD, KtTokens.NOT_IN -> lhs
                KtTokens.EQEQ -> lhs as? KtNameReferenceExpression ?: getRight()
                else -> null
            }

        }

        else -> null
    }
}

public fun KtWhenExpression.introduceSubject(): KtWhenExpression {
    val subject = getSubjectToIntroduce()!!

    val whenExpression = KtPsiFactory(this).buildExpression {
        appendFixedText("when(").appendExpression(subject).appendFixedText("){\n")

        for (entry in getEntries()) {
            val branchExpression = entry.getExpression()

            if (entry.isElse()) {
                appendFixedText("else")
            }
            else {
                for ((i, condition) in entry.getConditions().withIndex()) {
                    if (i > 0) appendFixedText(",")

                    val conditionExpression = (condition as KtWhenConditionWithExpression).getExpression()
                    when (conditionExpression)  {
                        is KtIsExpression -> {
                            if (conditionExpression.isNegated()) {
                                appendFixedText("!")
                            }
                            appendFixedText("is ")
                            appendNonFormattedText(conditionExpression.getTypeReference()?.getText() ?: "")
                        }

                        is KtBinaryExpression -> {
                            val lhs = conditionExpression.getLeft()
                            val rhs = conditionExpression.getRight()
                            val op = conditionExpression.getOperationToken()
                            when (op) {
                                KtTokens.IN_KEYWORD -> appendFixedText("in ").appendExpression(rhs)
                                KtTokens.NOT_IN -> appendFixedText("!in ").appendExpression(rhs)
                                KtTokens.EQEQ -> appendExpression(if (subject.matches(lhs)) rhs else lhs)
                                else -> throw IllegalStateException()
                            }
                        }

                        else -> throw IllegalStateException()
                    }
                }
            }
            appendFixedText("->")

            appendExpression(branchExpression)
            appendFixedText("\n")
        }

        appendFixedText("}")
    } as KtWhenExpression

    return replaced(whenExpression)
}

fun KtPsiFactory.combineWhenConditions(conditions: Array<KtWhenCondition>, subject: KtExpression?): KtExpression? {
    when (conditions.size) {
        0 -> return null
        1 -> return conditions[0].toExpression(subject)
        else -> {
            return buildExpression {
                appendExpressions(conditions.map { it.toExpression(subject) }, separator = "||")
            }
        }
    }
}