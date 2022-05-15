/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.types.ConeTypeContext
import org.jetbrains.kotlin.fir.types.FirCorrespondingSupertypesCache

private class SessionBasedTypeContext(
    override val session: FirSession,
    override val correspondingSupertypesCache: FirCorrespondingSupertypesCache?
) : ConeTypeContext

val FirSession.typeContext: ConeTypeContext get() = SessionBasedTypeContext(this, null)