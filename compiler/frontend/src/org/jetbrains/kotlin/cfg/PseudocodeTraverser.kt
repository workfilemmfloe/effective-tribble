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

package org.jetbrains.kotlin.cfg.pseudocodeTraverser

import org.jetbrains.kotlin.cfg.ControlFlowInfo
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.InlinedLocalFunctionDeclarationInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.LocalFunctionDeclarationInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.SubroutineEnterInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.SubroutineSinkInstruction
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder.FORWARD
import java.util.*

enum class LocalFunctionAnalysisStrategy {
    ANALYZE_EVERYTHING {
        override fun shouldVisitLocalFunction(declaration: LocalFunctionDeclarationInstruction) = true
    },

    ONLY_IN_PLACE_LAMBDAS {
        override fun shouldVisitLocalFunction(declaration: LocalFunctionDeclarationInstruction) =
            declaration is InlinedLocalFunctionDeclarationInstruction
    };

    abstract fun shouldVisitLocalFunction(declaration: LocalFunctionDeclarationInstruction): Boolean
}

/**
 * Traverse [analyzeInstruction] function along all instructions of pseudocode in [traversalOrder]
 */
fun Pseudocode.traverse(
    traversalOrder: TraversalOrder,
    analyzeInstruction: (Instruction) -> Unit
) {
    val instructions = getInstructions(traversalOrder)
    for (instruction in instructions) {
        if (instruction is LocalFunctionDeclarationInstruction) {
            instruction.body.traverse(traversalOrder, analyzeInstruction)
        }
        analyzeInstruction(instruction)
    }
}

/**
 * Traverse [analyzeInstruction] function along all instructions of pseudocode
 *   and edges data from [edgesMap] in [traversalOrder].
 *
 * [analyzeInstruction] takes [Instruction] and two [D] values -- data from
 *   incoming and outgoing edges
 */
fun <D> Pseudocode.traverse(
    traversalOrder: TraversalOrder,
    edgesMap: Map<Instruction, Edges<D>>,
    analyzeInstruction: (Instruction, D, D) -> Unit
) {
    val instructions = getInstructions(traversalOrder)
    for (instruction in instructions) {
        if (instruction is LocalFunctionDeclarationInstruction) {
            instruction.body.traverse(traversalOrder, edgesMap, analyzeInstruction)
        }
        val edges = edgesMap[instruction] ?: continue
        analyzeInstruction(instruction, edges.incoming, edges.outgoing)
    }
}

/**
 * Traverse [analyzeInstruction] and [analyzeIncomingEdge] functions along all instructions of pseudocode
 *   and edges data from [edgesMap] in [traversalOrder].
 *
 * [analyzeInstruction] takes [Instruction] and two [D] values -- data from
 *   incoming and outgoing edges.
 *
 * [analyzeIncomingEdge] used for analyzing every single edge. It takes two instructions (previous and current)
 *   and outgoing data from `previous -> current` edge
 *
 */
fun <I : ControlFlowInfo<*, *, *>> Pseudocode.traverse(
    traversalOrder: TraversalOrder,
    edgesMap: Map<Instruction, Edges<I>>,
    analyzeInstruction: (Instruction, I, I) -> Unit,
    analyzeIncomingEdge: (Instruction, Instruction, I) -> Unit,
    localFunctionAnalysisStrategy: LocalFunctionAnalysisStrategy
) {
    traverse(
        traversalOrder,
        edgesMap,
        analyzeInstruction,
        analyzeIncomingEdge,
        localFunctionAnalysisStrategy,
        previousSubGraphInstructions = Collections.emptyList(),
        isLocal = false
    )
}

private fun <I : ControlFlowInfo<*, *, *>> Pseudocode.traverse(
    traversalOrder: TraversalOrder,
    edgesMap: Map<Instruction, Edges<I>>,
    analyzeInstruction: (Instruction, I, I) -> Unit,
    analyzeIncomingEdge: (Instruction, Instruction, I) -> Unit,
    localFunctionAnalysisStrategy: LocalFunctionAnalysisStrategy,
    previousSubGraphInstructions: Collection<Instruction>,
    isLocal: Boolean
) {
    val instructions = getInstructions(traversalOrder)
    val startInstruction = getStartInstruction(traversalOrder)

    for (instruction in instructions) {
        val isStart = instruction.isStartInstruction(traversalOrder)
        if (!isLocal && isStart)
            continue

        val previousInstructions =
            getPreviousIncludingSubGraphInstructions(instruction, traversalOrder, startInstruction, previousSubGraphInstructions)

        if (instruction is LocalFunctionDeclarationInstruction && localFunctionAnalysisStrategy.shouldVisitLocalFunction(instruction)) {
            val subroutinePseudocode = instruction.body
            subroutinePseudocode.traverse(
                traversalOrder, edgesMap, analyzeInstruction, analyzeIncomingEdge, localFunctionAnalysisStrategy, previousInstructions, true
            )
            continue
        }

        previousInstructions.forEach { previousInstruction ->
            val previousData = edgesMap[previousInstruction] ?: return@forEach
            analyzeIncomingEdge(previousInstruction, instruction, previousData.outgoing)
        }

        val data = edgesMap[instruction] ?: continue
        analyzeInstruction(instruction, data.incoming, data.outgoing)
    }
}

/**
 * Collects data from pseudocode using ControlFlowAnalysis
 *
 * [mergeEdges] is callback that takes current instruction and all data from previous edges
 *  it has to merge previous data and return [Edges] info about current instruction
 *
 * [updateEdge] is a callback that takes previous instruction, current instruction and control flow info,
 *   and returns modified ControlFlowInfo. It can be used for cleaning control flow info; for example,
 *   it is possible to use it for clearing information about all local variables when leaving function declaration
 *
 * [localFunctionAnalysisStrategy] describes politic of analyzing [LocalFunctionDeclarationInstruction]
 *  it decides, should CFA come into local function or not
 */
fun <I : ControlFlowInfo<*, *, *>> Pseudocode.collectData(
    traversalOrder: TraversalOrder,
    mergeEdges: (Instruction, Collection<I>) -> Edges<I>,
    updateEdge: (Instruction, Instruction, I) -> I,
    initialInfo: I,
    localFunctionAnalysisStrategy: LocalFunctionAnalysisStrategy
): Map<Instruction, Edges<I>> {
    val edgesMap = LinkedHashMap<Instruction, Edges<I>>()
    val startInstruction = getStartInstruction(traversalOrder)
    edgesMap[startInstruction] = Edges(initialInfo, initialInfo)

    val changed = mutableMapOf<Instruction, Boolean>()
    do {
        collectDataFromSubgraph(
            traversalOrder, edgesMap,
            mergeEdges, updateEdge, Collections.emptyList(), changed, false,
            localFunctionAnalysisStrategy
        )
    } while (changed.any { it.value })

    return edgesMap
}

private fun <I : ControlFlowInfo<*, *, *>> Pseudocode.collectDataFromSubgraph(
    traversalOrder: TraversalOrder,
    edgesMap: MutableMap<Instruction, Edges<I>>,
    mergeEdges: (Instruction, Collection<I>) -> Edges<I>,
    updateEdge: (Instruction, Instruction, I) -> I,
    previousSubGraphInstructions: Collection<Instruction>,
    changed: MutableMap<Instruction, Boolean>,
    isLocal: Boolean,
    localFunctionAnalysisStrategy: LocalFunctionAnalysisStrategy
) {
    val instructions = getInstructions(traversalOrder)
    val startInstruction = getStartInstruction(traversalOrder)

    for (instruction in instructions) {
        val isStart = instruction.isStartInstruction(traversalOrder)
        if (!isLocal && isStart)
            continue

        val previousInstructions =
            getPreviousIncludingSubGraphInstructions(instruction, traversalOrder, startInstruction, previousSubGraphInstructions)

        if (instruction is LocalFunctionDeclarationInstruction && localFunctionAnalysisStrategy.shouldVisitLocalFunction(instruction)) {
            val subroutinePseudocode = instruction.body
            subroutinePseudocode.collectDataFromSubgraph(
                traversalOrder, edgesMap, mergeEdges, updateEdge, previousInstructions, changed, true,
                localFunctionAnalysisStrategy
            )
            // Special case for inlined functions: take flow from EXIT instructions (it contains flow which exits declaration normally)
            val lastInstruction = if (instruction is InlinedLocalFunctionDeclarationInstruction && traversalOrder == FORWARD)
                subroutinePseudocode.exitInstruction
            else
                subroutinePseudocode.getLastInstruction(traversalOrder)
            val previousValue = edgesMap[instruction]
            val newValue = edgesMap[lastInstruction]
            val updatedValue = newValue?.let {
                Edges(updateEdge(lastInstruction, instruction, it.incoming), updateEdge(lastInstruction, instruction, it.outgoing))
            }
            updateEdgeDataForInstruction(instruction, previousValue, updatedValue, edgesMap, changed)
            continue
        }


        val previousDataValue = edgesMap[instruction]
        if (previousDataValue != null && previousInstructions.all { changed[it] == false }) {
            changed[instruction] = false
            continue
        }

        val incomingEdgesData = HashSet<I>()

        for (previousInstruction in previousInstructions) {
            val previousData = edgesMap[previousInstruction] ?: continue
            incomingEdgesData.add(updateEdge(previousInstruction, instruction, previousData.outgoing))
        }

        val mergedData = mergeEdges(instruction, incomingEdgesData)
        updateEdgeDataForInstruction(instruction, previousDataValue, mergedData, edgesMap, changed)
    }
}

private fun getPreviousIncludingSubGraphInstructions(
    instruction: Instruction,
    traversalOrder: TraversalOrder,
    startInstruction: Instruction,
    previousSubGraphInstructions: Collection<Instruction>
): Collection<Instruction> {
    val previous = instruction.getPreviousInstructions(traversalOrder)
    if (instruction != startInstruction || previousSubGraphInstructions.isEmpty()) {
        return previous
    }
    val result = ArrayList(previous)
    result.addAll(previousSubGraphInstructions)
    return result
}

private fun <I : ControlFlowInfo<*, *, *>> updateEdgeDataForInstruction(
    instruction: Instruction,
    previousValue: Edges<I>?,
    newValue: Edges<I>?,
    edgesMap: MutableMap<Instruction, Edges<I>>,
    changed: MutableMap<Instruction, Boolean>
) {
    if (previousValue != newValue && newValue != null) {
        changed[instruction] = true
        edgesMap[instruction] = newValue
    } else {
        changed[instruction] = false
    }
}

data class Edges<out T>(val incoming: T, val outgoing: T)

enum class TraverseInstructionResult {
    CONTINUE,
    SKIP,
    HALT
}

// returns false when interrupted by handler
fun traverseFollowingInstructions(
    rootInstruction: Instruction,
    visited: MutableSet<Instruction> = HashSet(),
    order: TraversalOrder = FORWARD,
    // true to continue traversal
    handler: ((Instruction) -> TraverseInstructionResult)?
): Boolean {
    val stack = ArrayDeque<Instruction>()
    stack.push(rootInstruction)

    while (!stack.isEmpty()) {
        val instruction = stack.pop()
        if (!visited.add(instruction)) continue
        when (handler?.let { it(instruction) } ?: TraverseInstructionResult.CONTINUE) {
            TraverseInstructionResult.CONTINUE -> instruction.getNextInstructions(order).forEach { stack.push(it) }
            TraverseInstructionResult.SKIP -> {
            }
            TraverseInstructionResult.HALT -> return false
        }
    }
    return true
}

enum class TraversalOrder {
    FORWARD,
    BACKWARD
}

fun Pseudocode.getStartInstruction(traversalOrder: TraversalOrder): Instruction =
    if (traversalOrder == FORWARD) enterInstruction else sinkInstruction

fun Pseudocode.getLastInstruction(traversalOrder: TraversalOrder): Instruction =
    if (traversalOrder == FORWARD) sinkInstruction else enterInstruction

fun Pseudocode.getInstructions(traversalOrder: TraversalOrder): List<Instruction> =
    if (traversalOrder == FORWARD) instructions else reversedInstructions

fun Instruction.getNextInstructions(traversalOrder: TraversalOrder): Collection<Instruction> =
    if (traversalOrder == FORWARD) nextInstructions else previousInstructions

fun Instruction.getPreviousInstructions(traversalOrder: TraversalOrder): Collection<Instruction> =
    if (traversalOrder == FORWARD) previousInstructions else nextInstructions

fun Instruction.isStartInstruction(traversalOrder: TraversalOrder): Boolean =
    if (traversalOrder == FORWARD) this is SubroutineEnterInstruction else this is SubroutineSinkInstruction
