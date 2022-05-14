/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSetVariable
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.createValueSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns

class IrSetVariableImpl(
        startOffset: Int, endOffset: Int,
        override val symbol: IrValueSymbol,
        override val origin: IrStatementOrigin?
) : IrExpressionBase(startOffset, endOffset, symbol.descriptor.builtIns.unitType), IrSetVariable {
    constructor(
            startOffset: Int, endOffset: Int,
            symbol: IrValueSymbol,
            value: IrExpression,
            origin: IrStatementOrigin?
    ) : this(startOffset, endOffset, symbol, origin) {
        this.value = value
    }

    @Deprecated("Creates unbound symbol")
    constructor(
            startOffset: Int, endOffset: Int,
            descriptor: ValueDescriptor,
            value: IrExpression,
            origin: IrStatementOrigin?
    ) : this(startOffset, endOffset, createValueSymbol(descriptor), value, origin)

    override val descriptor: ValueDescriptor get() = symbol.descriptor

    override lateinit var value: IrExpression

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitSetVariable(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        value.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        value = value.transform(transformer, data)
    }
}