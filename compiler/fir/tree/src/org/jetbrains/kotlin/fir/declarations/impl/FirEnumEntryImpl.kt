/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirPureAbstractElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.SupertypesComputationStatus
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirEnumEntryImpl(
    override val psi: PsiElement?,
    override val session: FirSession,
    override val name: Name,
    override val symbol: FirClassSymbol
) : FirPureAbstractElement(), FirEnumEntry, FirModifiableClass, FirModifiableTypeParametersOwner, FirAbstractAnnotatedElement {
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override val typeParameters: MutableList<FirTypeParameter> = mutableListOf()
    override var status: FirDeclarationStatus = FirDeclarationStatusImpl(Visibilities.UNKNOWN, Modality.FINAL)
    override var supertypesComputationStatus: SupertypesComputationStatus = SupertypesComputationStatus.NOT_COMPUTED
    override val classKind: ClassKind get() = ClassKind.ENUM_ENTRY
    override val declarations: MutableList<FirDeclaration> = mutableListOf()
    override val companionObject: FirRegularClass? get() = null
    override val superTypeRefs: MutableList<FirTypeRef> = mutableListOf()
    override val arguments: MutableList<FirExpression> = mutableListOf()
    override var typeRef: FirTypeRef = session.builtinTypes.enumType

    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
        declarations.forEach { it.accept(visitor, data) }
        superTypeRefs.forEach { it.accept(visitor, data) }
        arguments.forEach { it.accept(visitor, data) }
        typeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirEnumEntryImpl {
        annotations.transformInplace(transformer, data)
        typeParameters.transformInplace(transformer, data)
        status = status.transformSingle(transformer, data)
        declarations.transformInplace(transformer, data)
        superTypeRefs.transformInplace(transformer, data)
        transformArguments(transformer, data)
        typeRef = typeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformArguments(transformer: FirTransformer<D>, data: D): FirEnumEntryImpl {
        arguments.transformInplace(transformer, data)
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }

    override fun replaceSupertypesComputationStatus(newSupertypesComputationStatus: SupertypesComputationStatus) {
        supertypesComputationStatus = newSupertypesComputationStatus
    }

    override fun replaceSuperTypeRefs(newSuperTypeRefs: List<FirTypeRef>) {
        superTypeRefs.clear()
        superTypeRefs.addAll(newSuperTypeRefs)
    }
}
