/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.*
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.util.CachedValue
import org.jetbrains.kotlin.asJava.builder.LightClassDataHolder
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

class KtUltraLightClassForFacade(
    manager: PsiManager,
    facadeClassFqName: FqName,
    lightClassDataCache: CachedValue<LightClassDataHolder.ForFacade>,
    files: Collection<KtFile>,
    private val filesWithSupports: Collection<Pair<KtFile, KtUltraLightSupport>>
) : KtLightClassForFacade(manager, facadeClassFqName, lightClassDataCache, files) {

    override fun getDelegate(): PsiClass = invalidAccess()

    override val lightClassDataCache: CachedValue<LightClassDataHolder.ForFacade> get() = invalidAccess()

    override val clsDelegate: PsiClass get() = invalidAccess()

    override fun getScope(): PsiElement? = parent

    private val filesWithSupportsWithCreators by lazyPub {
        filesWithSupports.map { (file, support) ->
            Triple(
                file,
                support,
                UltraLightMembersCreator(
                    containingClass = this,
                    containingClassIsNamedObject = false,
                    containingClassIsSealed = true,
                    mangleInternalFunctions = false,
                    support = support
                )
            )
        }
    }

    private fun loadMethodsFromFile(
        file: KtFile,
        support: KtUltraLightSupport,
        creator: UltraLightMembersCreator,
        result: MutableList<KtLightMethod>
    ) {
        for (declaration in file.declarations.filterNot { it.isHiddenByDeprecation(support) }) {
            when (declaration) {
                is KtNamedFunction -> result.addAll(creator.createMethods(declaration, true))
                is KtProperty -> {
                    result.addAll(
                        creator.propertyAccessors(
                            declaration, declaration.isVar,
                            forceStatic = true,
                            onlyJvmStatic = false
                        )
                    )

                }
            }
        }
    }

    private val _ownMethods: List<KtLightMethod> by lazyPub {
        mutableListOf<KtLightMethod>().also { result ->
            for ((file, support, creator) in filesWithSupportsWithCreators) {
                loadMethodsFromFile(file, support, creator, result)
            }
        }
    }

    private val _ownFields: List<KtLightField> by lazyPub {
        hashSetOf<String>().let { nameCache ->
            filesWithSupportsWithCreators.flatMap { (file, _, creator) ->
                file.declarations.filterIsInstance<KtProperty>().mapNotNull {
                    creator.createPropertyField(it, nameCache, forceStatic = true)
                }
            }
        }
    }

    override fun getOwnFields() = _ownFields

    override fun getOwnMethods() = _ownMethods

    override fun getVisibleSignatures(): MutableCollection<HierarchicalMethodSignature> = PsiSuperMethodImplUtil.getVisibleSignatures(this)

    override fun copy(): KtLightClassForFacade =
        KtUltraLightClassForFacade(manager, facadeClassFqName, lightClassDataCache, files, filesWithSupports)
}