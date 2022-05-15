/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirThisReceiverExpression : FirQualifiedAccessExpression {
    override val psi: PsiElement?
    override val typeRef: FirTypeRef
    override val annotations: List<FirAnnotationCall>
    override val safe: Boolean
    override val explicitReceiver: FirExpression?
    override val dispatchReceiver: FirExpression
    override val extensionReceiver: FirExpression
    override val calleeReference: FirThisReference

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitThisReceiverExpression(this, data)

    override fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirThisReceiverExpression

    override fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirThisReceiverExpression

    override fun <D> transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirThisReceiverExpression

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirThisReceiverExpression
}
