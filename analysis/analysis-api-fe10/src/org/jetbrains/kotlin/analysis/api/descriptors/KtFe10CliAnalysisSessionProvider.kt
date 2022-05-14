/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.session.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10Symbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.psi.KtElement
import java.lang.UnsupportedOperationException


@OptIn(KtAnalysisApiInternals::class)
class KtFe10AnalysisSessionProvider(project: Project) : KtAnalysisSessionProvider(project) {
    override fun getAnalysisSession(useSiteKtElement: KtElement, factory: KtLifetimeTokenFactory): KtAnalysisSession {
        return KtFe10AnalysisSession(useSiteKtElement, factory.create(project))
    }

    override fun getAnalysisSessionBySymbol(contextSymbol: KtSymbol): KtAnalysisSession {
        if (contextSymbol is KtFe10Symbol) {
            return KtFe10AnalysisSession(contextSymbol.analysisContext, contextSymbol.analysisContext.contextElement.getKtModule())
        } else {
            val contextElement = contextSymbol.psi
            if (contextElement is KtElement) {
                return KtFe10AnalysisSession(contextElement, contextSymbol.token)
            }
        }

        throw UnsupportedOperationException("getAnalysisSessionBySymbol() should not be used on KtFe10AnalysisSession")
    }

    override fun getAnalysisSessionByUseSiteKtModule(useSiteKtModule: KtModule, factory: KtLifetimeTokenFactory): KtAnalysisSession {
        throw UnsupportedOperationException("getAnalysisSessionByModule() should not be used on KtFe10AnalysisSession")
    }

    override fun clearCaches() {}
}