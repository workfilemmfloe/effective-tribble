/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.name.Name

abstract class AbstractFirUseSiteScope(
    session: FirSession,
    protected val superTypesScope: FirSuperTypeScope,
    protected val declaredMemberScope: FirScope
) : AbstractFirOverrideScope(session) {
    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> ProcessorAction): ProcessorAction {
        val overrideCandidates = mutableSetOf<FirFunctionSymbol<*>>()
        if (!declaredMemberScope.processFunctionsByName(name) {
                overrideCandidates += it
                processor(it)
            }
        ) return ProcessorAction.STOP

        return superTypesScope.processFunctionsByName(name) {

            val overriddenBy = it.getOverridden(overrideCandidates)
            if (overriddenBy == null) {
                processor(it)
            } else {
                ProcessorAction.NEXT
            }
        }
    }

    override fun processClassifiersByName(name: Name, position: FirPosition, processor: (FirClassifierSymbol<*>) -> Boolean): Boolean {
        return declaredMemberScope.processClassifiersByName(name, position, processor)
    }
}