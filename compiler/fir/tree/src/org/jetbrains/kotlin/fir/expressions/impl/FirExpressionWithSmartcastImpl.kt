/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

class FirExpressionWithSmartcastImpl(
    override var originalExpression: FirQualifiedAccessExpression,
    override val typeRef: FirTypeRef,
    override val typesFromSmartcast: Collection<ConeKotlinType>
) : FirPureAbstractElement(), FirExpressionWithSmartcast {
    init {
        assert(originalExpression.typeRef is FirResolvedTypeRef)
    }

    override val psi: PsiElement? get() = originalExpression.psi
    override val annotations: List<FirAnnotationCall> get() = originalExpression.annotations
    override val safe: Boolean get() = originalExpression.safe
    override val explicitReceiver: FirExpression? get() = originalExpression.explicitReceiver
    override val dispatchReceiver: FirExpression get() = originalExpression.dispatchReceiver
    override val extensionReceiver: FirExpression get() = originalExpression.extensionReceiver
    override val calleeReference: FirReference get() = originalExpression.calleeReference
    override val originalType: FirTypeRef get() = originalExpression.typeRef

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirExpressionWithSmartcast {
        originalExpression = originalExpression.transformSingle(transformer, data)
        return this
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        originalExpression.acceptChildren(visitor, data)
    }

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirExpressionWithSmartcast {
        throw IllegalStateException()
    }

    override fun <D> transformExplicitReceiver(transformer: FirTransformer<D>, data: D): FirExpressionWithSmartcast {
        throw IllegalStateException()
    }

    override fun <D> transformDispatchReceiver(transformer: FirTransformer<D>, data: D): FirExpressionWithSmartcast {
        throw IllegalStateException()
    }

    override fun <D> transformExtensionReceiver(transformer: FirTransformer<D>, data: D): FirExpressionWithSmartcast {
        throw IllegalStateException()
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {}
}