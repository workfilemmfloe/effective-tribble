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
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.SupertypesComputationStatus
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirModifiableRegularClass : FirRegularClass, FirModifiableClass, FirModifiableTypeParametersOwner, FirAbstractAnnotatedElement {
    override val psi: PsiElement?
    override val session: FirSession
    override var resolvePhase: FirResolvePhase
    override val name: Name
    override val annotations: MutableList<FirAnnotationCall>
    override val typeParameters: MutableList<FirTypeParameter>
    override var status: FirDeclarationStatus
    override var supertypesComputationStatus: SupertypesComputationStatus
    override val classKind: ClassKind
    override val declarations: MutableList<FirDeclaration>
    override val symbol: FirClassSymbol
    override var companionObject: FirRegularClass?
    override val superTypeRefs: MutableList<FirTypeRef>
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirModifiableRegularClass

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase)

    override fun replaceSupertypesComputationStatus(newSupertypesComputationStatus: SupertypesComputationStatus)

    override fun replaceSuperTypeRefs(newSuperTypeRefs: List<FirTypeRef>)
}
