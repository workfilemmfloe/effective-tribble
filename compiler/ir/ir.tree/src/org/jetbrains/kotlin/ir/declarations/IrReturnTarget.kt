/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol

interface IrReturnTarget : IrSymbolOwner {
    val descriptor: FunctionDescriptor
    override val symbol: IrReturnTargetSymbol
}