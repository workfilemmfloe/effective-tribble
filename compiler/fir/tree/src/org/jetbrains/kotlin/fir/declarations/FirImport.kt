/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirImport : FirElement {
    override val psi: PsiElement?
    val importedFqName: FqName?
    val isAllUnder: Boolean
    val aliasName: Name?

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitImport(this, data)
}
