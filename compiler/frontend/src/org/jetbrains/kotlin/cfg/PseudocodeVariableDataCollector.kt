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

package org.jetbrains.kotlin.cfg

import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.LexicalScope
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.VariableDeclarationInstruction
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.Edges
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.collectData
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.traverse
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import java.util.ArrayList
import java.util.HashMap

public class PseudocodeVariableDataCollector(
        private val bindingContext: BindingContext,
        private val pseudocode: Pseudocode
) {
    val lexicalScopeVariableInfo = computeLexicalScopeVariableInfo(pseudocode)

    public fun <I : ControlFlowInfo<*>> collectData(
            traversalOrder: TraversalOrder,
            mergeDataWithLocalDeclarations: Boolean,
            initialInfo: I,
            instructionDataMergeStrategy: (Instruction, Collection<I>) -> Edges<I>
    ): Map<Instruction, Edges<I>> {
        return pseudocode.collectData(
                traversalOrder, mergeDataWithLocalDeclarations,
                instructionDataMergeStrategy,
                { from, to, info -> filterOutVariablesOutOfScope(from, to, info) },
                initialInfo
        )
    }

    private fun <I : ControlFlowInfo<*>> filterOutVariablesOutOfScope(
            from: Instruction,
            to: Instruction,
            info: I
    ): I {
        // If an edge goes from deeper lexical scope to a less deep one, this means that it points outside of the deeper scope.
        val toDepth = to.lexicalScope.depth
        if (toDepth >= from.lexicalScope.depth) return info

        // Variables declared in an inner (deeper) scope can't be accessed from an outer scope.
        // Thus they can be filtered out upon leaving the inner scope.
        @Suppress("UNCHECKED_CAST")
        return info.copy().retainAll { variable ->
            val lexicalScope = lexicalScopeVariableInfo.declaredIn[variable]
            // '-1' for variables declared outside this pseudocode
            val depth = lexicalScope?.depth ?: -1
            depth <= toDepth
        } as I
    }

    fun computeLexicalScopeVariableInfo(pseudocode: Pseudocode): LexicalScopeVariableInfo {
        val lexicalScopeVariableInfo = LexicalScopeVariableInfoImpl()
        pseudocode.traverse(TraversalOrder.FORWARD, { instruction ->
            if (instruction is VariableDeclarationInstruction) {
                val variableDeclarationElement = instruction.variableDeclarationElement
                val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, variableDeclarationElement)
                if (descriptor != null) {
                    // TODO: investigate why tests fail without this eager computation here
                    descriptor.toString()

                    assert(descriptor is VariableDescriptor) {
                        "Variable descriptor should correspond to the instruction for ${instruction.element.getText()}.\n" +
                        "Descriptor: $descriptor"
                    }
                    lexicalScopeVariableInfo.registerVariableDeclaredInScope(
                            descriptor as VariableDescriptor, instruction.lexicalScope
                    )
                }
            }
        })
        return lexicalScopeVariableInfo
    }
}

public interface LexicalScopeVariableInfo {
    val declaredIn : Map<VariableDescriptor, LexicalScope>
    val scopeVariables : Map<LexicalScope, Collection<VariableDescriptor>>
}

public class LexicalScopeVariableInfoImpl : LexicalScopeVariableInfo {
    override val declaredIn = HashMap<VariableDescriptor, LexicalScope>()
    override val scopeVariables = HashMap<LexicalScope, MutableCollection<VariableDescriptor>>()

    fun registerVariableDeclaredInScope(variable: VariableDescriptor, lexicalScope: LexicalScope) {
        declaredIn[variable] = lexicalScope
        val variablesInScope = scopeVariables.getOrPut(lexicalScope, { ArrayList<VariableDescriptor>() })
        variablesInScope.add(variable)
    }
}
