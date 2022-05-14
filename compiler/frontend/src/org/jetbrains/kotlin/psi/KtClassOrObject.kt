/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.CheckUtil
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

abstract class KtClassOrObject :
        KtTypeParameterListOwnerStub<KotlinClassOrObjectStub<out KtClassOrObject>>, KtDeclarationContainer, KtNamedDeclaration {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinClassOrObjectStub<out KtClassOrObject>, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    fun getSuperTypeList(): KtSuperTypeList? = getStubOrPsiChild(KtStubElementTypes.SUPER_TYPE_LIST)
    open fun getSuperTypeListEntries(): List<KtSuperTypeListEntry> = getSuperTypeList()?.getEntries().orEmpty()

    fun addSuperTypeListEntry(superTypeListEntry: KtSuperTypeListEntry): KtSuperTypeListEntry {
        getSuperTypeList()?.let {
            return EditCommaSeparatedListHelper.addItem(it, getSuperTypeListEntries(), superTypeListEntry)
        }

        val psiFactory = KtPsiFactory(this)
        val specifierListToAdd = psiFactory.createSuperTypeCallEntry("A()").replace(superTypeListEntry).getParent()
        val colon = addBefore(psiFactory.createColon(), getBody())
        return (addAfter(specifierListToAdd, colon) as KtSuperTypeList).getEntries().first()
    }

    fun removeSuperTypeListEntry(superTypeListEntry: KtSuperTypeListEntry) {
        val specifierList = getSuperTypeList() ?: return
        assert(superTypeListEntry.getParent() === specifierList)

        if (specifierList.getEntries().size > 1) {
            EditCommaSeparatedListHelper.removeItem<KtElement>(superTypeListEntry)
        }
        else {
            deleteChildRange(findChildByType<PsiElement>(KtTokens.COLON) ?: specifierList, specifierList)
        }
    }

    fun getAnonymousInitializers(): List<KtAnonymousInitializer> = getBody()?.anonymousInitializers.orEmpty()

    fun getBody(): KtClassBody? = getStubOrPsiChild(KtStubElementTypes.CLASS_BODY)

    fun addDeclaration(declaration: KtDeclaration): KtDeclaration {
        val body = getOrCreateBody()
        val anchor = PsiTreeUtil.skipSiblingsBackward(body.rBrace ?: body.getLastChild()!!, PsiWhiteSpace::class.java)
        return body.addAfter(declaration, anchor) as KtDeclaration
    }

    fun addDeclarationAfter(declaration: KtDeclaration, anchor: PsiElement?): KtDeclaration {
        val anchorBefore = anchor ?: getDeclarations().lastOrNull() ?: return addDeclaration(declaration)
        return getOrCreateBody().addAfter(declaration, anchorBefore) as KtDeclaration
    }

    fun addDeclarationBefore(declaration: KtDeclaration, anchor: PsiElement?): KtDeclaration {
        val anchorAfter = anchor ?: getDeclarations().firstOrNull() ?: return addDeclaration(declaration)
        return getOrCreateBody().addBefore(declaration, anchorAfter) as KtDeclaration
    }

    fun isTopLevel(): Boolean = getStub()?.isTopLevel() ?: (getParent() is KtFile)

    fun isLocal(): Boolean = getStub()?.isLocal() ?: KtPsiUtil.isLocal(this)
    
    override fun getDeclarations() = getBody()?.getDeclarations().orEmpty()

    override fun getPresentation(): ItemPresentation? = ItemPresentationProviders.getItemPresentation(this)

    fun getPrimaryConstructor(): KtPrimaryConstructor? = getStubOrPsiChild(KtStubElementTypes.PRIMARY_CONSTRUCTOR)

    fun getPrimaryConstructorModifierList(): KtModifierList? = getPrimaryConstructor()?.getModifierList()
    fun getPrimaryConstructorParameterList(): KtParameterList? = getPrimaryConstructor()?.getValueParameterList()
    fun getPrimaryConstructorParameters(): List<KtParameter> = getPrimaryConstructorParameterList()?.getParameters().orEmpty()

    fun hasExplicitPrimaryConstructor(): Boolean = getPrimaryConstructor() != null

    fun hasPrimaryConstructor(): Boolean = hasExplicitPrimaryConstructor() || !hasSecondaryConstructors()
    private fun hasSecondaryConstructors(): Boolean = !getSecondaryConstructors().isEmpty()

    fun getSecondaryConstructors(): List<KtSecondaryConstructor> = getBody()?.secondaryConstructors.orEmpty()

    fun isAnnotation(): Boolean = hasModifier(KtTokens.ANNOTATION_KEYWORD)

    override fun delete() {
        CheckUtil.checkWritable(this);

        val file = getContainingKtFile();
        if (!isTopLevel() || file.getDeclarations().size > 1) {
            super.delete()
        }
        else {
            file.delete();
        }
    }
}

fun KtClassOrObject.getOrCreateBody(): KtClassBody {
    getBody()?.let { return it }

    val newBody = KtPsiFactory(this).createEmptyClassBody()
    if (this is KtEnumEntry) return addAfter(newBody, initializerList ?: nameIdentifier) as KtClassBody
    return add(newBody) as KtClassBody
}
