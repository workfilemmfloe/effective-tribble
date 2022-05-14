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

package org.jetbrains.kotlin

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.types.KotlinType

class ExpressionBlockExtractor(private val irBuiltIns: IrBuiltIns) : IrElementTransformer<Nothing?> {
    var changed = false

    override fun visitFunction(declaration: IrFunction, data: Nothing?): IrStatement {
        //TODO default
        return super.visitFunction(declaration, data)
    }

    override fun visitProperty(declaration: IrProperty, data: Nothing?): IrStatement {
        // TODO initializer
        return super.visitProperty(declaration, data)
    }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: Nothing?): IrStatement {
        // TODO ?
        return super.visitLocalDelegatedProperty(declaration, data)
    }

    override fun visitVariable(declaration: IrVariable, data: Nothing?): IrStatement {
        return super.visitVariable(declaration, data).let {
            /// TODO is this cast ok?
            extractIrBlockIfNeed(it as IrVariable, declaration.initializer, declaration.descriptor.type) { initializer = it }
        }
    }

    override fun visitExpressionBody(body: IrExpressionBody, data: Nothing?): IrBody {
        //TODO ?
        return super.visitExpressionBody(body, data)
    }

    override fun visitVararg(expression: IrVararg, data: Nothing?): IrExpression {
        // TODO ?
        return super.visitVararg(expression, data)
    }

    override fun visitSpreadElement(spread: IrSpreadElement, data: Nothing?): IrSpreadElement {
        // TODO ?
        return super.visitSpreadElement(spread, data)
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: Nothing?): IrExpression {
        //
        return super.visitStringConcatenation(expression, data)
    }

    override fun visitSetVariable(expression: IrSetVariable, data: Nothing?): IrExpression {
        // expression.value
        return super.visitSetVariable(expression, data)
    }

    override fun visitSetField(expression: IrSetField, data: Nothing?): IrExpression {
        // expression.value
        return super.visitSetField(expression, data)
    }

//    override fun visitCall(expression: IrCall, data: Nothing?): org.jetbrains.kotlin.ir.expressions.IrExpression {
//        return super.visitCall(expression, data)
//    }
//
//    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Nothing?): org.jetbrains.kotlin.ir.expressions.IrExpression {
//        return super.visitDelegatingConstructorCall(expression, data)
//    }
//
//    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall, data: Nothing?): org.jetbrains.kotlin.ir.expressions.IrExpression {
//        return super.visitEnumConstructorCall(expression, data)
//    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Nothing?): IrExpression {
        //TODO ?
        return super.visitTypeOperator(expression, data)
    }

    override fun visitWhen(expression: IrWhen, data: Nothing?): IrExpression {
        return super.visitWhen(expression, data)
    }

    override fun visitLoop(loop: IrLoop, data: Nothing?): IrExpression {
        // condition
        return super.visitLoop(loop, data)
    }

//    override fun visitWhileLoop(loop: IrWhileLoop, data: Nothing?): org.jetbrains.kotlin.ir.expressions.IrExpression {
//        return super.visitWhileLoop(loop, data)
//    }
//
//    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Nothing?): org.jetbrains.kotlin.ir.expressions.IrExpression {
//        return super.visitDoWhileLoop(loop, data)
//    }

    override fun visitReturn(expression: IrReturn, data: Nothing?): IrExpression {
        // TODO try to reuse block?
        return super.visitReturn(expression, data).let {
            /// TODO is this cast ok?
            extractIrBlockIfNeed(it as IrReturn, expression.value) { value = it }
        }
    }

    override fun visitThrow(expression: IrThrow, data: Nothing?): IrExpression {
        return super.visitThrow(expression, data).let {
            extractIrBlockIfNeed(expression, expression.value) { value = it}
        }
    }

    private fun <T : IrStatement, R : IrStatement> extractIrBlockIfNeed(expression: T, elementToReplace: IrExpression?, type: KotlinType = irBuiltIns.builtIns.nothingType, replace: T.(IrExpression) -> Unit): R {
        if (elementToReplace is IrBlock) {
            changed = true

            return extractIrBlock(expression, elementToReplace, type, replace)
        }

        @Suppress("UNCHECKED_CAST")
        return expression as R
    }


    private fun <T : IrStatement, R : IrStatement> extractIrBlock(declaration: T, block: IrBlock, type: KotlinType = irBuiltIns.builtIns.nothingType, replace: T.(IrExpression) -> Unit): R {
        val statements = block.statements
        val last = block.statements[statements.lastIndex]
        declaration.replace(last as IrExpression)
        statements[statements.lastIndex] = declaration

        @Suppress("UNCHECKED_CAST")
        return IrCompositeImpl(
                declaration.startOffset,
                declaration.endOffset,
                type,
                null,
                statements) as R
    }
}
