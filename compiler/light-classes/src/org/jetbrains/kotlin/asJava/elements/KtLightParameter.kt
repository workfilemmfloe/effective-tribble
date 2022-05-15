/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration

class KtLightParameter(
        override val clsDelegate: PsiParameter,
        private val index: Int,
        val method: KtLightMethod
) : LightParameter(clsDelegate.name ?: "p$index", clsDelegate.type, method, KotlinLanguage.INSTANCE),
        KtLightDeclaration<KtParameter, PsiParameter> {

    private val modifierList: PsiModifierList
    private var lightIdentifier: KtLightIdentifier? = null

    override val kotlinOrigin: KtParameter?
        get() {
            val declaration = method.kotlinOrigin ?: return null

            val jetIndex = if (declaration.isExtensionDeclaration()) index - 1 else index
            if (jetIndex < 0) return null

            if (declaration is KtFunction) {
                val paramList = declaration.valueParameters
                return if (jetIndex < paramList.size) paramList[jetIndex] else null
            }

            if (jetIndex != 0) return null

            val setter = when (declaration) {
                is KtPropertyAccessor -> if (declaration.isSetter) declaration else null
                is KtProperty -> declaration.setter
                is KtParameter -> return declaration
                else -> return null
            }

            return setter?.parameter
        }

    init {
        if (method.lightMemberOrigin is LightMemberOriginForDeclaration) {
            this.modifierList = KtLightSimpleModifierList(this, emptySet())
        }
        else {
            this.modifierList = super.getModifierList()
        }
    }

    override fun getModifierList(): PsiModifierList = modifierList

    override fun getNavigationElement(): PsiElement = kotlinOrigin ?: super.getNavigationElement()

    override fun isValid(): Boolean = method.isValid

    @Throws(IncorrectOperationException::class)
    override fun setName(@NonNls name: String): PsiElement {
        kotlinOrigin?.setName(name)
        return this
    }

    override fun getContainingFile(): PsiFile = method.containingFile

    override fun getLanguage(): Language = KotlinLanguage.INSTANCE

    override fun getUseScope(): SearchScope {
        return kotlinOrigin?.useScope ?: LocalSearchScope(this)
    }

    override fun getText(): String = kotlinOrigin?.text ?: ""

    override fun getTextRange(): TextRange = kotlinOrigin?.textRange ?: TextRange.EMPTY_RANGE

    override fun getNameIdentifier(): PsiIdentifier? {
        if (lightIdentifier == null) {
            lightIdentifier = KtLightIdentifier(this, kotlinOrigin)
        }
        return lightIdentifier
    }

    override fun getParent(): PsiElement = method.parameterList

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        val result = ApplicationManager.getApplication().runReadAction(Computable {
            val kotlinOrigin = kotlinOrigin
            if (another is KtLightParameter && kotlinOrigin != null) {
                kotlinOrigin == another.kotlinOrigin && clsDelegate == another.clsDelegate
            }
            else {
                null
            }
        })
        result?.let { return it }

        return super.isEquivalentTo(another)
    }

    override fun equals(other: Any?): Boolean {
        return other is PsiElement && isEquivalentTo(other)
    }

    override fun hashCode(): Int = kotlinOrigin?.hashCode() ?: 0
}
