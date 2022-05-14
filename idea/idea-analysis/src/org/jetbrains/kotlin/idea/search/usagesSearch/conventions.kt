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

package org.jetbrains.kotlin.idea.search.usagesSearch

import com.google.common.collect.ImmutableSet
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DelegatedPropertyResolver
import org.jetbrains.kotlin.resolve.dataClassUtils.isComponentLike
import org.jetbrains.kotlin.types.expressions.OperatorConventions.*
import org.jetbrains.kotlin.util.OperatorNameConventions

public val ALL_SEARCHABLE_OPERATIONS: ImmutableSet<KtToken> = ImmutableSet
        .builder<KtToken>()
        .addAll(UNARY_OPERATION_NAMES.keySet())
        .addAll(BINARY_OPERATION_NAMES.keySet())
        .addAll(ASSIGNMENT_OPERATIONS.keySet())
        .addAll(COMPARISON_OPERATIONS)
        .addAll(EQUALS_OPERATIONS)
        .addAll(IDENTITY_EQUALS_OPERATIONS)
        .addAll(IN_OPERATIONS)
        .add(KtTokens.LBRACKET)
        .add(KtTokens.BY_KEYWORD)
        .build()

public val INDEXING_OPERATION_NAMES = setOf(OperatorNameConventions.GET, OperatorNameConventions.SET)

public val DELEGATE_ACCESSOR_NAMES = setOf(Name.identifier("getValue"), Name.identifier("setValue"))

public val IN_OPERATIONS_TO_SEARCH = setOf(KtTokens.IN_KEYWORD)

public val COMPARISON_OPERATIONS_TO_SEARCH = setOf(KtTokens.LT, KtTokens.GT)

public fun Name.getOperationSymbolsToSearch(): Set<KtToken> {
    when (this) {
        OperatorNameConventions.COMPARE_TO -> return COMPARISON_OPERATIONS_TO_SEARCH
        OperatorNameConventions.EQUALS -> return EQUALS_OPERATIONS
        OperatorNameConventions.IDENTITY_EQUALS -> return IDENTITY_EQUALS_OPERATIONS
        OperatorNameConventions.CONTAINS -> return IN_OPERATIONS_TO_SEARCH
        OperatorNameConventions.ITERATOR -> return IN_OPERATIONS_TO_SEARCH
        in INDEXING_OPERATION_NAMES -> return setOf(KtTokens.LBRACKET)
        in DELEGATE_ACCESSOR_NAMES -> return setOf(KtTokens.BY_KEYWORD)
        DelegatedPropertyResolver.PROPERTY_DELEGATED_FUNCTION_NAME -> return setOf(KtTokens.BY_KEYWORD)
    }

    if (isComponentLike(this)) return setOf(KtTokens.LPAR)

    val unaryOp = UNARY_OPERATION_NAMES_WITH_DEPRECATED_INVERTED[this]
    if (unaryOp != null) return setOf(unaryOp)

    val binaryOp = BINARY_OPERATION_NAMES.inverse()[this]
    if (binaryOp != null) {
        val assignmentOp = ASSIGNMENT_OPERATION_COUNTERPARTS.inverse()[binaryOp]
        return if (assignmentOp != null) setOf(binaryOp, assignmentOp) else setOf(binaryOp)
    }

    val assignmentOp = ASSIGNMENT_OPERATIONS.inverse()[this]
    if (assignmentOp != null) return setOf(assignmentOp)

    return emptySet()
}
