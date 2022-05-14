/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.j2k

import com.intellij.psi.*
import org.jetbrains.kotlin.j2k.ast.*
import java.util.ArrayList

trait StatementConverter {
    fun convertStatement(statement: PsiStatement, codeConverter: CodeConverter): Statement
}

trait SpecialStatementConverter {
    fun convertStatement(statement: PsiStatement, codeConverter: CodeConverter): Statement?
}

fun StatementConverter.withSpecialConverter(specialConverter: SpecialStatementConverter): StatementConverter {
    return object: StatementConverter {
        override fun convertStatement(statement: PsiStatement, codeConverter: CodeConverter): Statement
                = specialConverter.convertStatement(statement, codeConverter) ?: this@withSpecialConverter.convertStatement(statement, codeConverter)
    }
}

class DefaultStatementConverter : JavaElementVisitor(), StatementConverter {
    private var _codeConverter: CodeConverter? = null
    private var result: Statement = Statement.Empty

    private val codeConverter: CodeConverter get() = _codeConverter!!
    private val converter: Converter get() = codeConverter.converter

    override fun convertStatement(statement: PsiStatement, codeConverter: CodeConverter): Statement {
        this._codeConverter = codeConverter
        result = Statement.Empty

        statement.accept(this)
        return result
    }

    override fun visitAssertStatement(statement: PsiAssertStatement) {
        val descriptionExpr = statement.getAssertDescription()
        val condition = codeConverter.convertExpression(statement.getAssertCondition())
        if (descriptionExpr == null) {
            result = MethodCallExpression.buildNotNull(null, "assert", listOf(condition))
        }
        else {
            val description = codeConverter.convertExpression(descriptionExpr)
            if (descriptionExpr is PsiLiteralExpression) {
                result = MethodCallExpression.buildNotNull(null, "assert", listOf(condition, description))
            }
            else {
                val block = Block(listOf(description), LBrace().assignNoPrototype(), RBrace().assignNoPrototype())
                val lambda = LambdaExpression(null, block.assignNoPrototype())
                result = MethodCallExpression.build(null, "assert", listOf(condition), listOf(), false, lambda)
            }
        }
    }

    override fun visitBlockStatement(statement: PsiBlockStatement) {
        val block = codeConverter.convertBlock(statement.getCodeBlock())
        result = MethodCallExpression.build(null, "run", listOf(), listOf(), false, LambdaExpression(null, block).assignNoPrototype())
    }

    override fun visitBreakStatement(statement: PsiBreakStatement) {
        if (statement.getLabelIdentifier() == null) {
            result = BreakStatement(Identifier.Empty)
        }
        else {
            result = BreakStatement(converter.convertIdentifier(statement.getLabelIdentifier()))
        }
    }

    override fun visitContinueStatement(statement: PsiContinueStatement) {
        if (statement.getLabelIdentifier() == null) {
            result = ContinueStatement(Identifier.Empty)
        }
        else {
            result = ContinueStatement(converter.convertIdentifier(statement.getLabelIdentifier()))
        }
    }

    override fun visitDeclarationStatement(statement: PsiDeclarationStatement) {
        result = DeclarationStatement(statement.getDeclaredElements().map {
            when (it) {
                is PsiLocalVariable -> codeConverter.convertLocalVariable(it)
                is PsiClass -> converter.convertClass(it)
                else -> Element.Empty //what else can be here?
            }
        })
    }

    override fun visitDoWhileStatement(statement: PsiDoWhileStatement) {
        val condition = statement.getCondition()
        val expression = if (condition != null && condition.getType() != null)
            codeConverter.convertExpression(condition, condition.getType())
        else
            codeConverter.convertExpression(condition)
        result = DoWhileStatement(expression, codeConverter.convertStatementOrBlock(statement.getBody()), statement.isInSingleLine())
    }

    override fun visitExpressionStatement(statement: PsiExpressionStatement) {
        result = codeConverter.convertExpression(statement.getExpression())
    }

    override fun visitExpressionListStatement(statement: PsiExpressionListStatement) {
        result = ExpressionListStatement(codeConverter.convertExpressions(statement.getExpressionList().getExpressions()))
    }

    override fun visitForStatement(statement: PsiForStatement) {
        result = ForConverter(statement, codeConverter).execute()
    }

    override fun visitForeachStatement(statement: PsiForeachStatement) {
        val iteratorExpr = codeConverter.convertExpression(statement.getIteratedValue())
        val iterator = BangBangExpression.surroundIfNullable(iteratorExpr)
        val iterationParameter = statement.getIterationParameter()
        result = ForeachStatement(iterationParameter.declarationIdentifier(),
                                  if (codeConverter.settings.specifyLocalVariableTypeByDefault) codeConverter.typeConverter.convertVariableType(iterationParameter) else null,
                                  iterator,
                                  codeConverter.convertStatementOrBlock(statement.getBody()),
                                  statement.isInSingleLine())
    }

    override fun visitIfStatement(statement: PsiIfStatement) {
        val condition = statement.getCondition()
        val expression = codeConverter.convertExpression(condition, PsiType.BOOLEAN)
        result = IfStatement(expression,
                             codeConverter.convertStatementOrBlock(statement.getThenBranch()),
                             codeConverter.convertStatementOrBlock(statement.getElseBranch()),
                             statement.isInSingleLine())
    }

    override fun visitLabeledStatement(statement: PsiLabeledStatement) {
        val statementConverted = codeConverter.convertStatement(statement.getStatement())
        val identifier = converter.convertIdentifier(statement.getLabelIdentifier())
        if (statementConverted is ForConverter.WhileWithInitializationPseudoStatement) { // special case - if our loop gets converted to while with initialization we should move the label to the loop
            val labeledLoop = LabeledStatement(identifier, statementConverted.loop).assignPrototype(statement)
            result = ForConverter.WhileWithInitializationPseudoStatement(statementConverted.initialization, labeledLoop, statementConverted.kind)
        }
        else {
            result = LabeledStatement(identifier, statementConverted)
        }
    }

    override fun visitSwitchLabelStatement(statement: PsiSwitchLabelStatement) {
        result = if (statement.isDefaultCase())
            ElseWhenEntrySelector()
        else
            ValueWhenEntrySelector(codeConverter.convertExpression(statement.getCaseValue()))
    }

    override fun visitSwitchStatement(statement: PsiSwitchStatement) {
        result = SwitchConverter(codeConverter).convert(statement)
    }

    override fun visitSynchronizedStatement(statement: PsiSynchronizedStatement) {
        result = SynchronizedStatement(codeConverter.convertExpression(statement.getLockExpression()),
                                         codeConverter.convertBlock(statement.getBody()))
    }

    override fun visitThrowStatement(statement: PsiThrowStatement) {
        result = ThrowStatement(codeConverter.convertExpression(statement.getException()))
    }

    override fun visitTryStatement(tryStatement: PsiTryStatement) {
        val tryBlock = tryStatement.getTryBlock()
        val catchesConverted = convertCatches(tryStatement)
        val finallyConverted = codeConverter.convertBlock(tryStatement.getFinallyBlock())

        val resourceList = tryStatement.getResourceList()
        if (resourceList != null) {
            val variables = resourceList.getResourceVariables()
            if (variables.isNotEmpty()) {
                result = convertTryWithResources(tryBlock, variables, catchesConverted, finallyConverted)
                return
            }
        }

        result = TryStatement(codeConverter.convertBlock(tryBlock), catchesConverted, finallyConverted)
    }

    private fun convertCatches(tryStatement: PsiTryStatement): List<CatchStatement> {
        val catches = ArrayList<CatchStatement>()
        for ((block, parameter) in tryStatement.getCatchBlocks().zip(tryStatement.getCatchBlockParameters())) {
            val blockConverted = codeConverter.convertBlock(block)
            val annotations = converter.convertAnnotations(parameter)
            val parameterType = parameter.getType()
            val types = if (parameterType is PsiDisjunctionType)
                parameterType.getDisjunctions()
            else
                listOf(parameterType)
            for (t in types) {
                var convertedType = codeConverter.typeConverter.convertType(t, Nullability.NotNull)
                val convertedParameter = Parameter(parameter.declarationIdentifier(),
                                                   convertedType,
                                                   Parameter.VarValModifier.None,
                                                   annotations,
                                                   Modifiers.Empty).assignPrototype(parameter)
                catches.add(CatchStatement(convertedParameter, blockConverted).assignNoPrototype())
            }
        }
        return catches
    }

    private fun convertTryWithResources(tryBlock: PsiCodeBlock?, resourceVariables: List<PsiResourceVariable>, catchesConverted: List<CatchStatement>, finallyConverted: Block): Statement {
        var wrapResultStatement: (Expression) -> Statement = { it }
        var converterForBody = codeConverter

        var block = converterForBody.convertBlock(tryBlock)
        var expression: Expression = Expression.Empty
        for (variable in resourceVariables.reverse()) {
            val lambda = LambdaExpression(Identifier.toKotlin(variable.getName()!!), block)
            expression = MethodCallExpression.build(codeConverter.convertExpression(variable.getInitializer()), "use", listOf(), listOf(), false, lambda)
            expression.assignNoPrototype()
            block = Block(listOf(expression), LBrace().assignNoPrototype(), RBrace().assignNoPrototype()).assignNoPrototype()
        }

        if (catchesConverted.isEmpty() && finallyConverted.isEmpty) {
            return wrapResultStatement(expression)
        }

        block = Block(listOf(wrapResultStatement(expression)), LBrace().assignPrototype(tryBlock?.getLBrace()), RBrace().assignPrototype(tryBlock?.getRBrace()), true)
        return TryStatement(block.assignPrototype(tryBlock), catchesConverted, finallyConverted)
    }

    override fun visitWhileStatement(statement: PsiWhileStatement) {
        val condition = statement.getCondition()
        val expression = if (condition?.getType() != null)
            codeConverter.convertExpression(condition, condition!!.getType())
        else
            codeConverter.convertExpression(condition)
        result = WhileStatement(expression, codeConverter.convertStatementOrBlock(statement.getBody()), statement.isInSingleLine())
    }

    override fun visitReturnStatement(statement: PsiReturnStatement) {
        val returnValue = statement.getReturnValue()
        val methodReturnType = codeConverter.methodReturnType
        val expression = if (returnValue != null && methodReturnType != null)
            codeConverter.convertExpression(returnValue, methodReturnType)
        else
            codeConverter.convertExpression(returnValue)
        result = ReturnStatement(expression)
    }

    override fun visitEmptyStatement(statement: PsiEmptyStatement) {
        result = Statement.Empty
    }
}

fun CodeConverter.convertStatementOrBlock(statement: PsiStatement?): Statement {
    return if (statement is PsiBlockStatement)
        convertBlock(statement.getCodeBlock())
    else
        convertStatement(statement)
}

