/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirResolvedDeclarationStatus : FirDeclarationStatus {
    override val psi: PsiElement?
    override val visibility: Visibility
    override val modality: Modality?
    override val isExpect: Boolean
    override val isActual: Boolean
    override val isOverride: Boolean
    override val isOperator: Boolean
    override val isInfix: Boolean
    override val isInline: Boolean
    override val isTailRec: Boolean
    override val isExternal: Boolean
    override val isConst: Boolean
    override val isLateInit: Boolean
    override val isInner: Boolean
    override val isCompanion: Boolean
    override val isData: Boolean
    override val isSuspend: Boolean
    override val isStatic: Boolean

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitResolvedDeclarationStatus(this, data)
}
