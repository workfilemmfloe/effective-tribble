/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k

import com.intellij.psi.*
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction


class JKSymbolProvider {
    val symbolsByPsi = mutableMapOf<PsiElement, JKSymbol>()
    val symbolsByJK = mutableMapOf<JKDeclaration, JKSymbol>()
    private val elementVisitor = ElementVisitor()

    fun preBuildTree(files: List<PsiJavaFile>) {
        files.forEach { it.accept(elementVisitor) }
    }

    fun provideDirectSymbol(psi: PsiElement): JKSymbol {
        return symbolsByPsi.getOrPut(psi) {
            when (psi) {
                is PsiClass -> JKMultiverseClassSymbol(psi)
                is KtClassOrObject -> JKMultiverseKtClassSymbol(psi)
                is PsiMethod -> JKMultiverseMethodSymbol(psi, this)
                is PsiField -> JKMultiverseFieldSymbol(psi)
                is KtNamedFunction -> JKMultiverseFunctionSymbol(psi)
                else -> TODO(psi::class.toString())
            }
        }
    }

    inline fun <reified T : JKSymbol> provideSymbol(reference: PsiReference): T {
        val target = reference.resolve()
        if (target != null) return provideDirectSymbol(target) as T
        return JKUnresolvedField(reference).let { if (it is T) it else JKUnresolvedMethod(reference) as T }
    }

    fun provideUniverseSymbol(psi: PsiElement, jk: JKDeclaration): JKSymbol = provideUniverseSymbol(psi).also {
        when (it) {
            is JKUniverseClassSymbol -> it.target = jk as JKClass
            is JKUniverseFieldSymbol -> it.target = jk as JKField
            is JKUniverseMethodSymbol -> it.target = jk as JKMethod
        }
        symbolsByJK[jk] = it
    }

    fun provideUniverseSymbol(psi: PsiElement): JKSymbol = symbolsByPsi.getOrPut(psi) {
        when (psi) {
            is PsiField, is PsiParameter, is PsiLocalVariable -> JKUniverseFieldSymbol()
            is PsiMethod -> JKUniverseMethodSymbol(this)
            is PsiClass -> JKUniverseClassSymbol()
            else -> TODO()
        }
    }

    fun transferSymbol(to: JKDeclaration, from: JKDeclaration) = symbolsByJK[from]!!.let {
        it as JKUniverseSymbol<*>
        it.target = to
        symbolsByJK[to] = it
    }

    fun provideUniverseSymbol(jk: JKClass): JKClassSymbol = symbolsByJK.getOrPut(jk) {
        JKUniverseClassSymbol().also { it.target = jk }
    } as JKClassSymbol

    fun provideUniverseSymbol(jk: JKField): JKFieldSymbol = symbolsByJK.getOrPut(jk) {
        JKUniverseFieldSymbol().also { it.target = jk }
    } as JKFieldSymbol

    fun provideUniverseSymbol(jk: JKMethod): JKMethodSymbol = symbolsByJK.getOrPut(jk) {
        JKUniverseMethodSymbol(this).also { it.target = jk }
    } as JKMethodSymbol

    private inner class ElementVisitor : JavaElementVisitor() {
        override fun visitClass(aClass: PsiClass) {
            provideUniverseSymbol(aClass)
            aClass.acceptChildren(this)
        }

        override fun visitField(field: PsiField) {
            provideUniverseSymbol(field)
        }

        override fun visitMethod(method: PsiMethod) {
            provideUniverseSymbol(method)
        }

        override fun visitFile(file: PsiFile) {
            file.acceptChildren(this)
        }
    }
}