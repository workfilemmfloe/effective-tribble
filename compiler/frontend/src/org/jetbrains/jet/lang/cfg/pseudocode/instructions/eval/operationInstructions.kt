/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.cfg.pseudocode.instructions.eval

import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.lang.cfg.pseudocode.PseudoValue
import org.jetbrains.jet.lang.cfg.pseudocode.PseudoValueFactory
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.LexicalScope
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionWithNext
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionVisitor
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionVisitorWithResult
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor
import org.jetbrains.jet.lang.cfg.pseudocode.TypePredicate

public abstract class OperationInstruction protected(
        element: JetElement,
        lexicalScope: LexicalScope,
        public override val inputValues: List<PseudoValue>
) : InstructionWithNext(element, lexicalScope), InstructionWithValue {
    protected var resultValue: PseudoValue? = null

    override val outputValue: PseudoValue?
        get() = resultValue

    protected fun renderInstruction(name: String, desc: String): String =
            "$name($desc" +
            (if (inputValues.notEmpty) "|${inputValues.joinToString(", ")})" else ")") +
            (if (resultValue != null) " -> $resultValue" else "")

    protected fun setResult(value: PseudoValue?): OperationInstruction {
        this.resultValue = value
        return this
    }

    protected fun setResult(factory: PseudoValueFactory?, valueElement: JetElement? = element): OperationInstruction {
        return setResult(factory?.newValue(valueElement, this))
    }
}

trait StrictlyValuedOperationInstruction: OperationInstruction {
    override val outputValue: PseudoValue
        get() = resultValue!!
}

public class CallInstruction private(
        element: JetElement,
        lexicalScope: LexicalScope,
        val resolvedCall: ResolvedCall<*>,
        public override val receiverValues: Map<PseudoValue, ReceiverValue>,
        public val arguments: Map<PseudoValue, ValueParameterDescriptor>
) : OperationInstruction(element, lexicalScope, receiverValues.keySet() + arguments.keySet()), InstructionWithReceivers {
    override fun accept(visitor: InstructionVisitor) {
        visitor.visitCallInstruction(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitCallInstruction(this)
    }

    override fun createCopy() =
            CallInstruction(element, lexicalScope, resolvedCall, receiverValues, arguments).setResult(resultValue)

    override fun toString() =
            renderInstruction("call", "${render(element)}, ${resolvedCall.getResultingDescriptor()!!.getName()}")

    class object {
        fun create (
                element: JetElement,
                valueElement: JetElement?,
                lexicalScope: LexicalScope,
                resolvedCall: ResolvedCall<*>,
                receiverValues: Map<PseudoValue, ReceiverValue>,
                arguments: Map<PseudoValue, ValueParameterDescriptor>,
                factory: PseudoValueFactory?
        ): CallInstruction =
                CallInstruction(element, lexicalScope, resolvedCall, receiverValues, arguments).setResult(factory, valueElement) as CallInstruction
    }
}

// Introduces black-box operation
// Used to:
//      consume input values (so that they aren't considered unused)
//      denote value transformation which can't be expressed by other instructions (such as call or read)
//      pass more than one value to instruction which formally requires only one (e.g. jump)
// "Synthetic" means that the instruction does not correspond to some operation explicitly expressed by PSI element
//      Examples: providing initial values for parameters, missing right-hand side in assignments
public class MagicInstruction(
        element: JetElement,
        lexicalScope: LexicalScope,
        val synthetic: Boolean,
        inputValues: List<PseudoValue>,
        val expectedTypes: Map<PseudoValue, TypePredicate>
) : OperationInstruction(element, lexicalScope, inputValues), StrictlyValuedOperationInstruction {
    override fun accept(visitor: InstructionVisitor) = visitor.visitMagic(this)

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitMagic(this)

    override fun createCopy() = MagicInstruction(element, lexicalScope, synthetic, inputValues, expectedTypes).setResult(resultValue)

    override fun toString() = renderInstruction("magic", render(element))

    class object {
        fun create(
                element: JetElement,
                valueElement: JetElement?,
                lexicalScope: LexicalScope,
                synthetic: Boolean,
                inputValues: List<PseudoValue>,
                expectedTypes: Map<PseudoValue, TypePredicate>,
                factory: PseudoValueFactory
        ): MagicInstruction = MagicInstruction(
                element, lexicalScope, synthetic, inputValues, expectedTypes
        ).setResult(factory, valueElement) as MagicInstruction
    }
}

// Merges values produced by alternative control-flow paths (such as 'if' branches)
class MergeInstruction private(
        element: JetElement,
        lexicalScope: LexicalScope,
        inputValues: List<PseudoValue>
): OperationInstruction(element, lexicalScope, inputValues), StrictlyValuedOperationInstruction {
    override fun accept(visitor: InstructionVisitor) = visitor.visitMerge(this)

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitMerge(this)

    override fun createCopy() = MergeInstruction(element, lexicalScope, inputValues).setResult(resultValue)

    override fun toString() = renderInstruction("merge", render(element))

    class object {
        fun create(
                element: JetElement,
                lexicalScope: LexicalScope,
                inputValues: List<PseudoValue>,
                factory: PseudoValueFactory
        ): MergeInstruction = MergeInstruction(element, lexicalScope, inputValues).setResult(factory) as MergeInstruction
    }
}