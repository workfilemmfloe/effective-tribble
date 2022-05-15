/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceService
import com.intellij.psi.impl.source.resolve.reference.ProviderBinding
import com.intellij.psi.impl.source.resolve.reference.PsiReferenceRegistrarImpl
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistryImpl
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.ProcessingContext
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KotlinReferenceProvidersService

class KtIdeReferenceProviderService : KotlinReferenceProvidersService() {
    private val kotlinFilteredReferenceProvidersRegistry = object : ReferenceProvidersRegistryImpl() {
        public override fun doGetReferencesFromProviders(context: PsiElement, hints: PsiReferenceService.Hints): Array<PsiReference> {
            if (context.language != KotlinLanguage.INSTANCE) return PsiReference.EMPTY_ARRAY

            return super.doGetReferencesFromProviders(context, hints)
        }

        override fun getRegistrar(language: Language): PsiReferenceRegistrarImpl {
            assert(language == KotlinLanguage.INSTANCE)

            val registrar = super.getRegistrar(KotlinLanguage.INSTANCE)
            return registrar
        }
    }

    override fun getReferences(psiElement: PsiElement): Array<PsiReference> {
        return CachedValuesManager.getCachedValue(psiElement) {
            CachedValueProvider.Result.create(
                kotlinFilteredReferenceProvidersRegistry.doGetReferencesFromProviders(psiElement, PsiReferenceService.Hints.NO_HINTS),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }
    }
}