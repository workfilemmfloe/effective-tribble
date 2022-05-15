/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.addDeclaration
import org.jetbrains.kotlin.fir.declarations.impl.FirClassImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirSimpleFunctionImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.references.impl.FirImplicitThisReference
import org.jetbrains.kotlin.fir.references.impl.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtParameter

fun List<Pair<KtParameter?, FirProperty>>.generateComponentFunctions(
    session: FirSession, firClass: FirClassImpl, packageFqName: FqName, classFqName: FqName,
    firPrimaryConstructor: FirConstructor
) {
    var componentIndex = 1
    for ((ktParameter, firProperty) in this) {
        if (!firProperty.isVal && !firProperty.isVar) continue
        val name = Name.identifier("component$componentIndex")
        componentIndex++
        val symbol = FirNamedFunctionSymbol(CallableId(packageFqName, classFqName, name))
        val status = FirDeclarationStatusImpl(Visibilities.PUBLIC, Modality.FINAL).apply {
            isExpect = false
            isActual = false
            isOverride = false
            isOperator = false
            isInfix = false
            isInline = false
            isTailRec = false
            isExternal = false
            isSuspend = false
        }
        firClass.addDeclaration(
            FirSimpleFunctionImpl(
                ktParameter, session, FirImplicitTypeRefImpl(ktParameter),
                null, name, status, symbol
            ).apply {
                val componentFunction = this
                body = FirSingleExpressionBlock(
                    FirReturnExpressionImpl(
                        ktParameter,
                        FirQualifiedAccessExpressionImpl(ktParameter).apply {
                            val parameterName = firProperty.name
                            dispatchReceiver = FirThisReceiverExpressionImpl(null, FirImplicitThisReference(firClass.symbol)).apply {
                                typeRef = firPrimaryConstructor.returnTypeRef
                            }
                            calleeReference = FirResolvedCallableReferenceImpl(
                                ktParameter,
                                parameterName, firProperty.symbol
                            )
                        }
                    ).apply {
                        target = FirFunctionTarget(null)
                        target.bind(componentFunction)
                    }
                )
            }
        )
    }
}

private val copyName = Name.identifier("copy")

fun List<Pair<KtParameter?, FirProperty>>.generateCopyFunction(
    session: FirSession, classOrObject: KtClassOrObject?, firClass: FirClassImpl, packageFqName: FqName, classFqName: FqName,
    firPrimaryConstructor: FirConstructor
) {
    val symbol = FirNamedFunctionSymbol(CallableId(packageFqName, classFqName, copyName))
    val status = FirDeclarationStatusImpl(Visibilities.PUBLIC, Modality.FINAL).apply {
        isExpect = false
        isActual = false
        isOverride = false
        isOperator = false
        isInfix = false
        isInline = false
        isTailRec = false
        isExternal = false
        isSuspend = false
    }
    firClass.addDeclaration(
        FirSimpleFunctionImpl(
            classOrObject,
            session,
            firPrimaryConstructor.returnTypeRef,
            null,
            copyName,
            status,
            symbol
        ).apply {
            for ((ktParameter, firProperty) in this@generateCopyFunction) {
                val name = firProperty.name
                valueParameters += FirValueParameterImpl(
                    ktParameter, session, firProperty.returnTypeRef,
                    name,
                    FirVariableSymbol(name),
                    FirQualifiedAccessExpressionImpl(ktParameter).apply {
                        dispatchReceiver = FirThisReceiverExpressionImpl(null, FirImplicitThisReference(firClass.symbol)).apply {
                            typeRef = firPrimaryConstructor.returnTypeRef
                        }
                        calleeReference = FirResolvedCallableReferenceImpl(ktParameter, name, firProperty.symbol)
                    },
                    isCrossinline = false, isNoinline = false, isVararg = false
                )
            }

            body = FirEmptyExpressionBlock()
        }
    )
}