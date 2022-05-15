/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirVariableAssignment : FirPureAbstractElement(), FirQualifiedAccess {
    abstract override val psi: PsiElement?
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val safe: Boolean
    abstract override val explicitReceiver: FirExpression?
    abstract override val dispatchReceiver: FirExpression
    abstract override val extensionReceiver: FirExpression
    abstract override val calleeReference: FirReference
    abstract val lValue: FirReference
    abstract val rValue: FirExpression
    abstract val operation: FirOperation

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitVariableAssignment(this, data)

    abstract override fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirVariableAssignment

    abstract override fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirVariableAssignment

    abstract override fun <D> transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirVariableAssignment

    abstract override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirVariableAssignment

    abstract fun <D> transformRValue(transformer: FirTransformer<D>, data: D): FirVariableAssignment
}
