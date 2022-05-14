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

package org.jetbrains.kotlin.js.coroutine

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.SideEffectKind
import org.jetbrains.kotlin.js.backend.ast.metadata.isSuspend
import org.jetbrains.kotlin.js.backend.ast.metadata.sideEffects
import org.jetbrains.kotlin.js.backend.ast.metadata.synthetic
import org.jetbrains.kotlin.js.inline.util.collectBreakContinueTargets
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.utils.DFS

class CoroutineBodyTransformer(private val program: JsProgram, private val context: CoroutineTransformationContext) : RecursiveJsVisitor() {
    private val entryBlock = context.entryBlock
    private val globalCatchBlock = context.globalCatchBlock
    private var currentBlock = entryBlock
    private val currentStatements: MutableList<JsStatement>
        get() = currentBlock.statements
    private val breakContinueTargetStatements = mutableMapOf<JsContinue, JsStatement>()
    private val breakTargets = mutableMapOf<JsStatement, JumpTarget>()
    private val continueTargets = mutableMapOf<JsStatement, JumpTarget>()
    private val referencedBlocks = mutableSetOf<CoroutineBlock>()
    private lateinit var nodesToSplit: Set<JsNode>
    private var currentCatchBlock = globalCatchBlock
    private val tryStack = mutableListOf(TryBlock(globalCatchBlock, null))

    var hasFinallyBlocks = false
        get
        private set

    private val currentTryDepth: Int
        get() = tryStack.lastIndex

    fun preProcess(node: JsNode) {
        breakContinueTargetStatements += node.collectBreakContinueTargets()
        nodesToSplit = node.collectNodesToSplit(breakContinueTargetStatements)
    }

    fun postProcess(): List<CoroutineBlock> {
        currentBlock.statements += JsReturn()
        val graph = entryBlock.buildGraph(globalCatchBlock)
        val orderedBlocks = DFS.topologicalOrder(listOf(entryBlock)) { graph[it].orEmpty() }
        orderedBlocks.replaceCoroutineFlowStatements(context, program)
        return orderedBlocks
    }

    override fun visitBlock(x: JsBlock) = splitIfNecessary(x) {
        for (statement in x.statements) {
            statement.accept(this)
        }
    }

    override fun visitIf(x: JsIf) = splitIfNecessary(x) {
        val ifBlock = currentBlock

        val thenEntryBlock = CoroutineBlock()
        currentBlock = thenEntryBlock
        x.thenStatement.accept(this)
        val thenExitBlock = currentBlock

        val elseEntryBlock = CoroutineBlock()
        currentBlock = elseEntryBlock
        x.elseStatement?.accept(this)
        val elseExitBlock = currentBlock

        x.thenStatement = JsBlock(thenEntryBlock.statements)
        x.elseStatement = JsBlock(elseEntryBlock.statements)
        ifBlock.statements += x

        val jointBlock = CoroutineBlock()
        thenExitBlock.statements += stateAndJump(jointBlock)
        elseExitBlock.statements += stateAndJump(jointBlock)
        currentBlock = jointBlock
    }

    override fun visitLabel(x: JsLabel) {
        val inner = x.statement
        when (inner) {
            is JsWhile,
            is JsDoWhile,
            is JsFor -> inner.accept(this)

            else -> splitIfNecessary(x) {
                val successor = CoroutineBlock()
                withBreakAndContinue(x.statement, successor, null) {
                    accept(inner)
                }
                if (successor in referencedBlocks) {
                    currentBlock.statements += stateAndJump(successor)
                    currentBlock = successor
                }
            }
        }
    }

    override fun visitWhile(x: JsWhile) = splitIfNecessary(x) {
        val successor = CoroutineBlock()
        val bodyEntryBlock = CoroutineBlock()
        currentStatements += stateAndJump(bodyEntryBlock)

        currentBlock = bodyEntryBlock
        if (x.condition != JsLiteral.TRUE) {
            currentStatements += JsIf(JsAstUtils.notOptimized(x.condition), JsBlock(stateAndJump(successor))).apply { source = x.source }
        }

        withBreakAndContinue(x, successor, bodyEntryBlock) {
            x.body.accept(this)
        }

        currentStatements += stateAndJump(bodyEntryBlock)
        currentBlock = successor
    }

    override fun visitDoWhile(x: JsDoWhile) = splitIfNecessary(x) {
        val successor = CoroutineBlock()
        val bodyEntryBlock = CoroutineBlock()
        currentStatements += stateAndJump(bodyEntryBlock)

        currentBlock = bodyEntryBlock
        withBreakAndContinue(x, successor, bodyEntryBlock) {
            x.body.accept(this)
        }

        if (x.condition != JsLiteral.TRUE) {
            val jsIf = JsIf(JsAstUtils.notOptimized(x.condition), JsBlock(stateAndJump(successor))).apply { source = x.source }
            currentStatements.add(jsIf)
        }
        currentBlock.statements += stateAndJump(bodyEntryBlock)

        currentBlock = successor
    }

    override fun visitFor(x: JsFor) = splitIfNecessary(x) {
        x.initExpression?.let {
            JsExpressionStatement(it).accept(this)
        }
        x.initVars?.let { initVars ->
            if (initVars.vars.isNotEmpty()) {
                initVars.accept(this)
            }
        }

        val increment = CoroutineBlock()
        val successor = CoroutineBlock()
        val bodyEntryBlock = CoroutineBlock()
        currentStatements += stateAndJump(bodyEntryBlock)

        currentBlock = bodyEntryBlock
        if (x.condition != null && x.condition != JsLiteral.TRUE) {
            currentStatements += JsIf(JsAstUtils.notOptimized(x.condition), JsBlock(stateAndJump(successor))).apply { source = x.source }
        }

        withBreakAndContinue(x, successor, increment) {
            x.body.accept(this)
        }

        currentStatements += stateAndJump(increment)
        currentBlock = increment

        x.incrementExpression?.let { JsExpressionStatement(it).accept(this) }
        currentStatements += stateAndJump(bodyEntryBlock)

        currentBlock = successor
    }

    override fun visitBreak(x: JsBreak) {
        val targetStatement = breakContinueTargetStatements[x]!!
        val (targetBlock, targetTryDepth) = breakTargets[targetStatement]!!
        referencedBlocks += targetBlock
        jumpWithFinally(targetTryDepth + 1, targetBlock)
        currentStatements += jump()
    }

    override fun visitContinue(x: JsContinue) {
        val targetStatement = breakContinueTargetStatements[x]!!
        val (targetBlock, targetTryDepth) = continueTargets[targetStatement]!!
        referencedBlocks += targetBlock
        jumpWithFinally(targetTryDepth + 1, targetBlock)
        currentStatements += jump()
    }

    /**
     * When we perform break, continue or return, we can leave try blocks, so we should update $exceptionHandler correspondingly.
     * Also, these try blocks can contain finally clauses, therefore we need to update $finallyPath as well.
     */
    private fun jumpWithFinally(targetTryDepth: Int, successor: CoroutineBlock) {
        if (targetTryDepth < tryStack.size) {
            val tryBlock = tryStack[targetTryDepth]
            currentStatements += exceptionState(tryBlock.catchBlock)
        }

        val relativeFinallyPath = relativeFinallyPath(targetTryDepth)
        val fullPath = relativeFinallyPath + successor
        if (fullPath.size > 1) {
            currentStatements += updateFinallyPath(fullPath.drop(1))
        }
        currentStatements += state(fullPath[0])
    }

    override fun visitTry(x: JsTry) = splitIfNecessary(x) {
        val catchNode = x.catches.firstOrNull()
        val finallyNode = x.finallyBlock
        val successor = CoroutineBlock()

        val catchBlock = CoroutineBlock()
        val finallyBlock = CoroutineBlock()

        tryStack += TryBlock(catchBlock, if (finallyNode != null) finallyBlock else null)

        val oldCatchBlock = currentCatchBlock
        currentCatchBlock = catchBlock
        currentStatements += exceptionState(catchBlock)

        x.tryBlock.statements.forEach { it.accept(this) }

        currentStatements += exceptionState(oldCatchBlock)
        currentCatchBlock = oldCatchBlock

        if (finallyNode != null) {
            currentStatements += updateFinallyPath(listOf(successor))
            currentStatements += stateAndJump(finallyBlock)
        }
        else {
            currentStatements += stateAndJump(successor)
        }

        // Handle catch node
        currentBlock = catchBlock

        if (finallyNode != null) {
            currentStatements += updateFinallyPath(listOf(oldCatchBlock))
            currentStatements += if (catchNode != null) exceptionState(finallyBlock) else stateAndJump(finallyBlock)
        }
        else {
            currentStatements += if (catchNode != null) exceptionState(oldCatchBlock) else stateAndJump(oldCatchBlock)
        }

        if (catchNode != null) {
            currentStatements += JsAstUtils.newVar(catchNode.parameter.name, JsNameRef(
                    context.metadata.exceptionName, JsAstUtils.stateMachineReceiver()))
            catchNode.body.statements.forEach { it.accept(this) }

            if (finallyNode == null) {
                currentStatements += stateAndJump(successor)
            }
            else {
                currentStatements += updateFinallyPath(listOf(successor))
                currentStatements += stateAndJump(finallyBlock)
            }
        }

        // Handle finally node
        if (finallyNode != null) {
            currentBlock = finallyBlock
            finallyNode.statements.forEach { it.accept(this) }
            generateFinallyExit()
            hasFinallyBlocks = true
        }

        tryStack.removeAt(tryStack.lastIndex)

        currentBlock = successor
    }

    // There's no implementation for JsSwitch, since we don't generate it. However, when we implement optimization
    // for simple `when` statement, we will need to support JsSwitch here

    private fun generateFinallyExit() {
        val finallyPathRef = JsNameRef(context.metadata.finallyPathName, JsAstUtils.stateMachineReceiver())
        val stateRef = JsNameRef(context.metadata.stateName, JsAstUtils.stateMachineReceiver())
        val nextState = JsInvocation(JsNameRef("shift", finallyPathRef))
        currentStatements += JsAstUtils.assignment(stateRef, nextState).makeStmt()
        currentStatements += jump()
    }

    override fun visitExpressionStatement(x: JsExpressionStatement) {
        val expression = x.expression
        val splitExpression = handleExpression(expression)
        if (splitExpression == expression) {
            currentStatements += x
        }
        else if (splitExpression != null) {
            currentStatements += JsExpressionStatement(splitExpression).apply { synthetic = true }
        }
    }

    override fun visitVars(x: JsVars) {
        currentStatements += x
    }

    override fun visitReturn(x: JsReturn) {
        val isInFinally = hasEnclosingFinallyBlock()
        if (isInFinally) {
            val returnBlock = CoroutineBlock()
            jumpWithFinally(0, returnBlock)
            val returnExpression = x.expression
            val returnFieldRef = if (returnExpression != null) {
                val ref = JsNameRef(context.returnValueFieldName, JsAstUtils.stateMachineReceiver())
                currentStatements += JsAstUtils.assignment(ref, x.expression).makeStmt()
                ref
            }
            else {
                null
            }
            currentStatements += jump()

            currentBlock = returnBlock
            currentStatements += JsReturn(returnFieldRef?.deepCopy())
        }
        else {
            currentStatements += x
        }
    }

    override fun visitThrow(x: JsThrow) {
        currentStatements += x
    }

    private fun handleExpression(expression: JsExpression): JsExpression? {
        return if (expression is JsInvocation) {
            if (handleInvocation(expression)) null else expression
        }
        else {
            val assignment = JsAstUtils.decomposeAssignment(expression)
            if (assignment != null) {
                (assignment.second as? JsInvocation)?.let { return if (handleInvocation(it)) null else expression }
            }
            expression
        }
    }

    private fun handleInvocation(expression: JsInvocation): Boolean {
        return if (expression.isSuspend) {
            handleSuspend(expression)
            true
        }
        else {
            false
        }
    }

    private fun handleSuspend(invocation: JsInvocation) {
        val nextBlock = CoroutineBlock()
        currentStatements += state(nextBlock)

        val resultRef = JsNameRef(context.metadata.resultName, JsAstUtils.stateMachineReceiver()).apply {
            sideEffects = SideEffectKind.DEPENDS_ON_STATE
        }
        val invocationStatement = JsAstUtils.assignment(resultRef, invocation).makeStmt()
        val suspendCondition = JsAstUtils.equality(resultRef.deepCopy(), context.metadata.suspendObjectRef.deepCopy())
        val suspendIfNeeded = JsIf(suspendCondition, JsReturn(context.metadata.suspendObjectRef.deepCopy()))
        currentStatements += listOf(invocationStatement, suspendIfNeeded, JsBreak())
        currentBlock = nextBlock
    }

    private fun state(target: CoroutineBlock): List<JsStatement> {
        val placeholder = JsDebugger()
        placeholder.targetBlock = target

        return listOf(placeholder)
    }

    private fun jump() = JsContinue()

    private fun stateAndJump(target: CoroutineBlock): List<JsStatement> {
        return state(target) + jump()
    }

    private fun exceptionState(target: CoroutineBlock): List<JsStatement> {
        val placeholder = JsDebugger()
        placeholder.targetExceptionBlock = target

        return listOf(placeholder)
    }

    private fun updateFinallyPath(path: List<CoroutineBlock>): List<JsStatement> {
        val placeholder = JsDebugger()
        placeholder.finallyPath = path
        return listOf(placeholder)
    }

    private inline fun splitIfNecessary(statement: JsStatement, action: () -> Unit) {
        if (statement in nodesToSplit) {
            action()
        }
        else {
            currentStatements += statement
        }
    }

    private fun withBreakAndContinue(
            statement: JsStatement,
            breakBlock: CoroutineBlock,
            continueBlock: CoroutineBlock? = null,
            action: () -> Unit
    ) {
        breakTargets[statement] = JumpTarget(breakBlock, currentTryDepth)
        if (continueBlock != null) {
            continueTargets[statement] = JumpTarget(continueBlock, currentTryDepth)
        }

        action()

        breakTargets.keys -= statement
        continueTargets.keys -= statement
    }

    private fun relativeFinallyPath(targetTryDepth: Int) = tryStack
            .subList(targetTryDepth, tryStack.size)
            .mapNotNull { it.finallyBlock }
            .reversed()

    private fun hasEnclosingFinallyBlock() = tryStack.any { it.finallyBlock != null }

    private data class JumpTarget(val block: CoroutineBlock, val tryDepth: Int)

    private class TryBlock(val catchBlock: CoroutineBlock, val finallyBlock: CoroutineBlock?)
}
