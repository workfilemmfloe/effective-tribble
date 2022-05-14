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
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

abstract public class KtClassOrObject :
        KtTypeParameterListOwnerStub<KotlinClassOrObjectStub<out KtClassOrObject>>, KtDeclarationContainer, KtNamedDeclaration {
    public constructor(node: ASTNode) : super(node)
    public constructor(stub: KotlinClassOrObjectStub<out KtClassOrObject>, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    public fun getSuperTypeList(): KtSuperTypeList? = getStubOrPsiChild(KtStubElementTypes.SUPER_TYPE_LIST)
    open public fun getSuperTypeListEntries(): List<KtSuperTypeListEntry> = getSuperTypeList()?.getEntries().orEmpty()

    public fun addSuperTypeListEntry(superTypeListEntry: KtSuperTypeListEntry): KtSuperTypeListEntry {
        getSuperTypeList()?.let {
            return EditCommaSeparatedListHelper.addItem(it, getSuperTypeListEntries(), superTypeListEntry)
        }

        val psiFactory = KtPsiFactory(this)
        val specifierListToAdd = psiFactory.createSuperTypeCallEntry("A()").replace(superTypeListEntry).getParent()
        val colon = addBefore(psiFactory.createColon(), getBody())
        return (addAfter(specifierListToAdd, colon) as KtSuperTypeList).getEntries().first()
    }

    public fun removeSuperTypeListEntry(superTypeListEntry: KtSuperTypeListEntry) {
        val specifierList = getSuperTypeList() ?: return
        assert(superTypeListEntry.getParent() === specifierList)

        if (specifierList.getEntries().size() > 1) {
            EditCommaSeparatedListHelper.removeItem<KtElement>(superTypeListEntry)
        }
        else {
            deleteChildRange(findChildByType<PsiElement>(KtTokens.COLON) ?: specifierList, specifierList)
        }
    }

    public fun getAnonymousInitializers(): List<KtAnonymousInitializer> = getBody()?.anonymousInitializers.orEmpty()

    public fun getBody(): KtClassBody? = getStubOrPsiChild(KtStubElementTypes.CLASS_BODY)

    public fun getOrCreateBody(): KtClassBody = getBody() ?: add(KtPsiFactory(this).createEmptyClassBody()) as KtClassBody

    public fun addDeclaration(declaration: KtDeclaration): KtDeclaration {
        val body = getOrCreateBody()
        val anchor = PsiTreeUtil.skipSiblingsBackward(body.rBrace ?: body.getLastChild()!!, javaClass<PsiWhiteSpace>())
        return body.addAfter(declaration, anchor) as KtDeclaration
    }

    public fun addDeclarationAfter(declaration: KtDeclaration, anchor: PsiElement?): KtDeclaration {
        val anchorBefore = anchor ?: getDeclarations().lastOrNull() ?: return addDeclaration(declaration)
        return getOrCreateBody().addAfter(declaration, anchorBefore) as KtDeclaration
    }

    public fun addDeclarationBefore(declaration: KtDeclaration, anchor: PsiElement?): KtDeclaration {
        val anchorAfter = anchor ?: getDeclarations().firstOrNull() ?: return addDeclaration(declaration)
        return getOrCreateBody().addBefore(declaration, anchorAfter) as KtDeclaration
    }

    public fun isTopLevel(): Boolean = getStub()?.isTopLevel() ?: (getParent() is KtFile)

    public fun isLocal(): Boolean = getStub()?.isLocal() ?: KtPsiUtil.isLocal(this)
    
    override fun getDeclarations() = getBody()?.getDeclarations().orEmpty()

    override fun getPresentation(): ItemPresentation? = ItemPresentationProviders.getItemPresentation(this)

    public fun getPrimaryConstructor(): KtPrimaryConstructor? = getStubOrPsiChild(KtStubElementTypes.PRIMARY_CONSTRUCTOR)

    public fun getPrimaryConstructorModifierList(): KtModifierList? = getPrimaryConstructor()?.getModifierList()
    public fun getPrimaryConstructorParameterList(): KtParameterList? = getPrimaryConstructor()?.getValueParameterList()
    public fun getPrimaryConstructorParameters(): List<KtParameter> = getPrimaryConstructorParameterList()?.getParameters().orEmpty()

    public fun hasExplicitPrimaryConstructor(): Boolean = getPrimaryConstructor() != null

    public fun hasPrimaryConstructor(): Boolean = hasExplicitPrimaryConstructor() || !hasSecondaryConstructors()
    private fun hasSecondaryConstructors(): Boolean = !getSecondaryConstructors().isEmpty()

    public fun getSecondaryConstructors(): List<KtSecondaryConstructor> = getBody()?.secondaryConstructors.orEmpty()

    public fun isAnnotation(): Boolean = hasModifier(KtTokens.ANNOTATION_KEYWORD)

    public override fun delete() {
        CheckUtil.checkWritable(this);

        val file = getContainingKtFile();
        if (!isTopLevel() || file.getDeclarations().size() > 1) {
            super.delete()
        }
        else {
            file.delete();
        }
    }
}
