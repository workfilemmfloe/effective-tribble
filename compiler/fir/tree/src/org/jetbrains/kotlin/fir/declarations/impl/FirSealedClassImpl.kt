/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirSealedClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.SupertypesComputationStatus
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class FirSealedClassImpl(
    override val psi: PsiElement?,
    override val session: FirSession,
    override val name: Name,
    override var status: FirDeclarationStatus,
    override val classKind: ClassKind,
    override val symbol: FirClassSymbol
) : FirSealedClass(), FirModifiableRegularClass, FirAbstractAnnotatedElement {
    override var resolvePhase: FirResolvePhase = FirResolvePhase.RAW_FIR
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override val typeParameters: MutableList<FirTypeParameter> = mutableListOf()
    override var supertypesComputationStatus: SupertypesComputationStatus = SupertypesComputationStatus.NOT_COMPUTED
    override val declarations: MutableList<FirDeclaration> = mutableListOf()
    override var companionObject: FirRegularClass? = null
    override val superTypeRefs: MutableList<FirTypeRef> = mutableListOf()
    override val inheritors: MutableList<ClassId> = mutableListOf()

    init {
        symbol.bind(this)
    }

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        typeParameters.forEach { it.accept(visitor, data) }
        status.accept(visitor, data)
        (declarations.firstOrNull { it is FirConstructorImpl } as? FirConstructorImpl)?.typeParameters?.forEach { it.accept(visitor, data) }
        declarations.forEach { it.accept(visitor, data) }
        superTypeRefs.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirSealedClassImpl {
        annotations.transformInplace(transformer, data)
        typeParameters.transformInplace(transformer, data)
        status = status.transformSingle(transformer, data)
        (declarations.firstOrNull { it is FirConstructorImpl } as? FirConstructorImpl)?.typeParameters?.transformInplace(transformer, data)
        declarations.transformInplace(transformer, data)
        companionObject = declarations.asSequence().filterIsInstance<FirRegularClass>().firstOrNull { it.status.isCompanion }
        superTypeRefs.transformInplace(transformer, data)
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

    override fun replaceInheritors(newInheritors: List<ClassId>) {
        inheritors.clear()
        inheritors.addAll(newInheritors)
    }
}
