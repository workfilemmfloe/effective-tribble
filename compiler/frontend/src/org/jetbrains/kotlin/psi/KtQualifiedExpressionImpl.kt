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

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.psiUtil.*

object KtQualifiedExpressionImpl {
    fun KtQualifiedExpression.getOperationTokenNode(): ASTNode {
        val operationNode = this.node!!.findChildByType(KtTokens.OPERATIONS)
        return operationNode!!
    }

    fun KtQualifiedExpression.getOperationSign(): KtToken {
        return this.operationTokenNode.elementType as KtToken
    }

    private fun KtQualifiedExpression.getExpression(afterOperation: Boolean): KtExpression? {
        return operationTokenNode.psi?.siblings(afterOperation, false)?.firstOrNull { it is KtExpression } as? KtExpression
    }

    fun KtQualifiedExpression.getReceiverExpression(): KtExpression {
        return getExpression(false) ?: throw AssertionError("No receiver found: ${getElementTextWithContext()}")
    }

    fun KtQualifiedExpression.getSelectorExpression(): KtExpression? {
        return getExpression(true)
    }
}
