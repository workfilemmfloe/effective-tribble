/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirProperty : FirPureAbstractElement(), FirVariable<FirProperty>, FirControlFlowGraphOwner, FirTypeParametersOwner, FirCallableMemberDeclaration<FirProperty> {
    abstract override val psi: PsiElement?
    abstract override val session: FirSession
    abstract override val resolvePhase: FirResolvePhase
    abstract override val returnTypeRef: FirTypeRef
    abstract override val receiverTypeRef: FirTypeRef?
    abstract override val name: Name
    abstract override val initializer: FirExpression?
    abstract override val delegate: FirExpression?
    abstract override val delegateFieldSymbol: FirDelegateFieldSymbol<FirProperty>?
    abstract override val isVar: Boolean
    abstract override val isVal: Boolean
    abstract override val getter: FirPropertyAccessor?
    abstract override val setter: FirPropertyAccessor?
    abstract override val annotations: List<FirAnnotationCall>
    abstract override val controlFlowGraphReference: FirControlFlowGraphReference
    abstract override val containerSource: DeserializedContainerSource?
    abstract override val symbol: FirPropertySymbol
    abstract val backingFieldSymbol: FirBackingFieldSymbol
    abstract val isLocal: Boolean
    abstract override val typeParameters: List<FirTypeParameter>
    abstract override val status: FirDeclarationStatus

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitProperty(this, data)

    abstract override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirProperty

    abstract override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirProperty

    abstract override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirProperty

    abstract override fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirProperty

    abstract override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirProperty
}
