/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirEnumEntry : FirRegularClass {
    override val psi: PsiElement?
    override val session: FirSession
    override val resolvePhase: FirResolvePhase
    override val name: Name
    override val annotations: List<FirAnnotationCall>
    override val typeParameters: List<FirTypeParameter>
    override val status: FirDeclarationStatus
    override val supertypesComputationStatus: SupertypesComputationStatus
    override val classKind: ClassKind
    override val declarations: List<FirDeclaration>
    override val symbol: FirClassSymbol
    override val companionObject: FirRegularClass?
    override val superTypeRefs: List<FirTypeRef>
    val arguments: List<FirExpression>
    val typeRef: FirTypeRef

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitEnumEntry(this, data)

    fun <D> transformArguments(transformer: FirTransformer<D>, data: D): FirEnumEntry
}
