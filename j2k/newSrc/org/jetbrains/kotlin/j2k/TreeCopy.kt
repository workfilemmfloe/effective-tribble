/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k

import org.jetbrains.kotlin.j2k.tree.JKArrayAccessExpression
import org.jetbrains.kotlin.j2k.tree.JKElement
import org.jetbrains.kotlin.j2k.tree.JKKtFieldAccessExpression
import org.jetbrains.kotlin.j2k.tree.impl.JKArrayAccessExpressionImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKKtFieldAccessExpressionImpl

fun <T : JKElement> T.copyTree(): T {
    return when (this) {
        is JKKtFieldAccessExpression -> JKKtFieldAccessExpressionImpl(identifier)
        is JKArrayAccessExpression -> JKArrayAccessExpressionImpl(expression.copyTree(), indexExpression.copyTree())
        else -> TODO("Not supported+$this.toString()")
    } as T
}