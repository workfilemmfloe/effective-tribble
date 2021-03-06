/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.evaluate

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.checker.EvaluationMode
import org.jetbrains.kotlin.ir.interpreter.checker.IrCompileTimeChecker
import org.jetbrains.kotlin.ir.interpreter.toIrConst
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isPrimitiveArray
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

fun evaluateConstants(irModuleFragment: IrModuleFragment) {
    val interpreter = IrInterpreter(irModuleFragment.irBuiltins)
    irModuleFragment.files.forEach { it.transformChildren(IrConstTransformer(interpreter, it), null) }
}

//TODO create abstract class that will be common for this and lowering
class IrConstTransformer(private val interpreter: IrInterpreter, private val irFile: IrFile) : IrElementTransformerVoid() {

    private fun IrExpression.replaceIfError(original: IrExpression): IrExpression {
        return if (this !is IrErrorExpression) this else original
    }

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.accept(IrCompileTimeChecker(mode = EvaluationMode.ONLY_BUILTINS), null)) {
            return interpreter.interpret(expression, irFile).replaceIfError(expression)
        }
        return super.visitCall(expression)
    }

    override fun visitField(declaration: IrField): IrStatement {
        transformAnnotations(declaration)

        val initializer = declaration.initializer
        val expression = initializer?.expression ?: return declaration
        if (expression is IrConst<*>) return declaration
        val isConst = declaration.correspondingPropertySymbol?.owner?.isConst == true
        if (isConst && expression.accept(IrCompileTimeChecker(declaration, mode = EvaluationMode.ONLY_BUILTINS), null)) {
            initializer.expression = interpreter.interpret(expression, irFile).replaceIfError(expression)
        }

        return declaration
    }

    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
        transformAnnotations(declaration)
        return super.visitDeclaration(declaration)
    }

    private fun transformAnnotations(annotationContainer: IrAnnotationContainer) {
        annotationContainer.annotations.forEach { annotation ->
            transformAnnotation(annotation)
        }
    }

    private fun transformAnnotation(annotation: IrConstructorCall) {
        for (i in 0 until annotation.valueArgumentsCount) {
            val arg = annotation.getValueArgument(i) ?: continue
            when (arg) {
                is IrVararg -> annotation.putValueArgument(i, arg.transformVarArg())
                else -> annotation.putValueArgument(i, arg.transformSingleArg(annotation.symbol.owner.valueParameters[i].type))
            }
        }
    }

    private fun IrVararg.transformVarArg(): IrVararg {
        if (elements.isEmpty()) return this
        val newIrVararg = IrVarargImpl(this.startOffset, this.endOffset, this.type, this.varargElementType)
        for (element in this.elements) {
            when (element) {
                is IrExpression -> newIrVararg.addElement(element.transformSingleArg(this.varargElementType))
                is IrSpreadElement -> {
                    when (val expression = element.expression) {
                        is IrVararg -> expression.transformVarArg().elements.forEach { newIrVararg.addElement(it) }
                        else -> newIrVararg.addElement(expression.transformSingleArg(this.varargElementType))
                    }
                }
            }
        }
        return newIrVararg
    }

    private fun IrExpression.transformSingleArg(expectedType: IrType): IrExpression {
        if (this.accept(IrCompileTimeChecker(mode = EvaluationMode.ONLY_BUILTINS), null)) {
            val const = interpreter.interpret(this, irFile).replaceIfError(this)
            return const.convertToConstIfPossible(expectedType)
        } else if (this is IrConstructorCall) {
            transformAnnotation(this)
        }
        return this
    }

    private fun IrExpression.convertToConstIfPossible(type: IrType): IrExpression {
        return when {
            this !is IrConst<*> || type is IrErrorType -> this
            type.isArray() -> this.convertToConstIfPossible((type as IrSimpleType).arguments.single().typeOrNull!!)
            type.isPrimitiveArray() -> this.convertToConstIfPossible(this.type)
            else -> this.value.toIrConst(type, this.startOffset, this.endOffset)
        }
    }
}
