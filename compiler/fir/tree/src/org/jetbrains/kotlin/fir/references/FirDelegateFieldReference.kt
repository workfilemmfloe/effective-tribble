/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirDelegateFieldReference : FirResolvedCallableReference() {
    abstract override val psi: PsiElement?
    abstract override val name: Name
    abstract override val candidateSymbol: AbstractFirBasedSymbol<*>?
    abstract override val resolvedSymbol: FirDelegateFieldSymbol<*>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitDelegateFieldReference(this, data)
}
