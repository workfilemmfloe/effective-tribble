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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallableReferenceImpl
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext

class LocalFunctionGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {
    fun generateLambda(ktLambda: KtLambdaExpression): IrStatement {
        val ktFun = ktLambda.functionLiteral
        val lambdaExpressionType = getInferredTypeWithImplicitCastsOrFail(ktLambda)
        val lambdaDescriptor = getOrFail(BindingContext.FUNCTION, ktFun)
        val irBlock = IrBlockImpl(ktLambda.startOffset, ktLambda.endOffset, lambdaExpressionType, IrStatementOrigin.LAMBDA)

        val irFun = IrFunctionImpl(ktFun.startOffset, ktFun.endOffset, IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA, lambdaDescriptor)
        irFun.body = BodyGenerator(lambdaDescriptor, statementGenerator.context).generateLambdaBody(ktFun)
        irBlock.statements.add(irFun)

        irBlock.statements.add(IrCallableReferenceImpl(ktLambda.startOffset, ktLambda.endOffset, lambdaExpressionType,
                                                       lambdaDescriptor, null, IrStatementOrigin.LAMBDA))

        return irBlock
    }

    fun generateFunction(ktFun: KtNamedFunction): IrStatement =
            if (ktFun.name != null) {
                generateFunctionDeclaration(ktFun)
            }
            else {
                // anonymous function expression
                val funExpressionType = getInferredTypeWithImplicitCastsOrFail(ktFun)
                val irBlock = IrBlockImpl(ktFun.startOffset, ktFun.endOffset, funExpressionType, IrStatementOrigin.ANONYMOUS_FUNCTION)

                val irFun = generateFunctionDeclaration(ktFun)
                irBlock.statements.add(irFun)

                irBlock.statements.add(IrCallableReferenceImpl(ktFun.startOffset, ktFun.endOffset, funExpressionType,
                                                               irFun.descriptor, null, IrStatementOrigin.ANONYMOUS_FUNCTION))

                irBlock
            }

    private fun generateFunctionDeclaration(ktFun: KtNamedFunction): IrFunction {
        val funDescriptor = getOrFail(BindingContext.FUNCTION, ktFun)
        val irFun = IrFunctionImpl(ktFun.startOffset, ktFun.endOffset, IrDeclarationOrigin.DEFINED, funDescriptor)
        irFun.body = BodyGenerator(funDescriptor, statementGenerator.context).generateFunctionBody(ktFun.bodyExpression!!)
        return irFun
    }

}