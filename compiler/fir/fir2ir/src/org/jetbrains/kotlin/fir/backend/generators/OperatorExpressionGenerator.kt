/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.isNullable
import org.jetbrains.kotlin.ir.builders.primitiveOp1
import org.jetbrains.kotlin.ir.builders.primitiveOp2
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.getSimpleFunction

internal class OperatorExpressionGenerator(
    private val components: Fir2IrComponents,
    private val visitor: Fir2IrVisitor,
    private val conversionScope: Fir2IrConversionScope
) : Fir2IrComponents by components {

    fun convertComparisonExpression(comparisonExpression: FirComparisonExpression): IrExpression {
        return comparisonExpression.convertWithOffsets { startOffset, endOffset ->
            generateComparisonCall(startOffset, endOffset, comparisonExpression)
        }
    }

    fun convertEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall): IrExpression {
        return equalityOperatorCall.convertWithOffsets { startOffset, endOffset ->
            generateEqualityOperatorCall(startOffset, endOffset, equalityOperatorCall.operation, equalityOperatorCall.arguments)
        }
    }

    private fun generateComparisonCall(
        startOffset: Int, endOffset: Int,
        comparisonExpression: FirComparisonExpression
    ): IrExpression {
        val operation = comparisonExpression.operation

        fun fallbackToRealCall(): IrExpression {
            val (symbol, origin) = getSymbolAndOriginForComparison(operation, irBuiltIns.intType.classifierOrFail)
            return primitiveOp2(
                startOffset, endOffset,
                symbol!!,
                irBuiltIns.booleanType,
                origin,
                comparisonExpression.compareToCall.accept(visitor, null) as IrExpression,
                IrConstImpl.int(startOffset, endOffset, irBuiltIns.intType, 0)
            )
        }

        if (comparisonExpression.compareToCall.toResolvedCallableSymbol()?.fir?.receiverTypeRef != null) {
            return fallbackToRealCall()
        }

        val comparisonInfo = comparisonExpression.inferPrimitiveNumericComparisonInfo() ?: return fallbackToRealCall()
        val comparisonType = comparisonInfo.comparisonType

        val comparisonIrType = typeConverter.classIdToTypeMap[comparisonType.lookupTag.classId] ?: return fallbackToRealCall()
        val (symbol, origin) = getSymbolAndOriginForComparison(operation, comparisonIrType.classifierOrFail)

        return primitiveOp2(
            startOffset,
            endOffset,
            symbol!!,
            irBuiltIns.booleanType,
            origin,
            comparisonExpression.left.convertToIrExpression(comparisonInfo, isLeftType = true),
            comparisonExpression.right.convertToIrExpression(comparisonInfo, isLeftType = false)
        )
    }

    private fun getSymbolAndOriginForComparison(
        operation: FirOperation,
        classifier: IrClassifierSymbol
    ): Pair<IrSimpleFunctionSymbol?, IrStatementOriginImpl> {
        return when (operation) {
            FirOperation.LT -> irBuiltIns.lessFunByOperandType[classifier] to IrStatementOrigin.LT
            FirOperation.GT -> irBuiltIns.greaterFunByOperandType[classifier] to IrStatementOrigin.GT
            FirOperation.LT_EQ -> irBuiltIns.lessOrEqualFunByOperandType[classifier] to IrStatementOrigin.LTEQ
            FirOperation.GT_EQ -> irBuiltIns.greaterOrEqualFunByOperandType[classifier] to IrStatementOrigin.GTEQ
            else -> error("Unexpected comparison operation: $operation")
        }
    }

    private fun generateEqualityOperatorCall(
        startOffset: Int, endOffset: Int, operation: FirOperation, arguments: List<FirExpression>
    ): IrExpression = when (operation) {
        FirOperation.EQ, FirOperation.NOT_EQ -> transformEqualityOperatorCall(startOffset, endOffset, operation, arguments)
        FirOperation.IDENTITY, FirOperation.NOT_IDENTITY -> transformIdentityOperatorCall(startOffset, endOffset, operation, arguments)
        else -> error("Unexpected operation: $operation")
    }

    private fun transformEqualityOperatorCall(
        startOffset: Int, endOffset: Int, operation: FirOperation, arguments: List<FirExpression>
    ): IrExpression {
        val origin = when (operation) {
            FirOperation.EQ -> IrStatementOrigin.EQEQ
            FirOperation.NOT_EQ -> IrStatementOrigin.EXCLEQ
            else -> error("Not an equality operation: $operation")
        }
        val comparisonInfo = inferPrimitiveNumericComparisonInfo(arguments[0], arguments[1])
        val comparisonType = comparisonInfo?.comparisonType
        val eqeqSymbol = comparisonType?.let { typeConverter.classIdToSymbolMap[it.lookupTag.classId] }
            ?.let { irBuiltIns.ieee754equalsFunByOperandType[it] } ?: irBuiltIns.eqeqSymbol

        val equalsCall = primitiveOp2(
            startOffset,
            endOffset,
            eqeqSymbol,
            irBuiltIns.booleanType,
            origin,
            arguments[0].convertToIrExpression(comparisonInfo, isLeftType = true),
            arguments[1].convertToIrExpression(comparisonInfo, isLeftType = false)
        )
        return if (operation == FirOperation.EQ) {
            equalsCall
        } else {
            equalsCall.negate(origin)
        }
    }

    private fun transformIdentityOperatorCall(
        startOffset: Int, endOffset: Int, operation: FirOperation, arguments: List<FirExpression>
    ): IrExpression {
        val origin = when (operation) {
            FirOperation.IDENTITY -> IrStatementOrigin.EQEQEQ
            FirOperation.NOT_IDENTITY -> IrStatementOrigin.EXCLEQEQ
            else -> error("Not an identity operation: $operation")
        }
        val identityCall = primitiveOp2(
            startOffset, endOffset,
            irBuiltIns.eqeqeqSymbol,
            irBuiltIns.booleanType,
            origin,
            visitor.convertToIrExpression(arguments[0]),
            visitor.convertToIrExpression(arguments[1])
        )
        return if (operation == FirOperation.IDENTITY) {
            identityCall
        } else {
            identityCall.negate(origin)
        }
    }

    private fun IrExpression.negate(origin: IrStatementOrigin) =
        primitiveOp1(startOffset, endOffset, irBuiltIns.booleanNotSymbol, irBuiltIns.booleanType, origin, this)

    private fun FirExpression.convertToIrExpression(
        comparisonInfo: PrimitiveConeNumericComparisonInfo?,
        isLeftType: Boolean
    ): IrExpression {
        val isOriginalNullable = (this as? FirExpressionWithSmartcast)?.originalExpression?.typeRef?.isMarkedNullable ?: false
        val irExpression = visitor.convertToIrExpression(this)
        val operandType = if (isLeftType) comparisonInfo?.leftType else comparisonInfo?.rightType
        val targetType = comparisonInfo?.comparisonType
        val noImplicitCast = comparisonInfo?.leftType == comparisonInfo?.rightType

        fun eraseImplicitCast(): IrExpression {
            if (irExpression is IrTypeOperatorCall) {
                val isDoubleOrFloatWithoutNullability = irExpression.type.isDoubleOrFloatWithoutNullability()
                if (noImplicitCast && !isDoubleOrFloatWithoutNullability && irExpression.operator == IrTypeOperator.IMPLICIT_CAST) {
                    return irExpression.argument
                } else {
                    val expressionType = irExpression.type
                    if (isDoubleOrFloatWithoutNullability &&
                        isOriginalNullable &&
                        expressionType is IrSimpleType &&
                        !expressionType.isNullable()
                    ) {
                        // Make it compatible with IR lowering
                        val nullableDoubleOrFloatType = expressionType.makeNullable()
                        return IrTypeOperatorCallImpl(
                            irExpression.startOffset,
                            irExpression.endOffset,
                            nullableDoubleOrFloatType,
                            irExpression.operator,
                            nullableDoubleOrFloatType,
                            irExpression.argument
                        )
                    }
                }
            }

            return irExpression
        }

        if (targetType == null) {
            return eraseImplicitCast()
        }

        if (operandType == null) error("operandType should be non-null if targetType is non-null")

        val operandClassId = operandType.lookupTag.classId
        val targetClassId = targetType.lookupTag.classId
        if (operandClassId == targetClassId) return eraseImplicitCast()
        val conversionFunction =
            typeConverter.classIdToSymbolMap[operandClassId]?.getSimpleFunction("to${targetType.lookupTag.classId.shortClassName.asString()}")
                ?: error("No conversion function for $operandType ~> $targetType")

        val unsafeIrCall = IrCallImpl(
            irExpression.startOffset, irExpression.endOffset,
            conversionFunction.owner.returnType,
            conversionFunction,
            valueArgumentsCount = 0,
            typeArgumentsCount = 0
        ).also {
            it.dispatchReceiver = irExpression
        }
        return if (operandType.isNullable) {
            val (receiverVariable, receiverVariableSymbol) =
                components.createTemporaryVariableForSafeCallConstruction(irExpression, conversionScope)

            unsafeIrCall.dispatchReceiver = IrGetValueImpl(irExpression.startOffset, irExpression.endOffset, receiverVariableSymbol)

            components.createSafeCallConstruction(receiverVariable, receiverVariableSymbol, unsafeIrCall)
        } else {
            unsafeIrCall
        }
    }
}
