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

package org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps

import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.cfg.Label
import org.jetbrains.kotlin.cfg.pseudocode.instructions.LexicalScope
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitor
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitorWithResult

public class ReturnNoValueInstruction(
        element: JetElement,
        lexicalScope: LexicalScope,
        targetLabel: Label
) : AbstractJumpInstruction(element, targetLabel, lexicalScope) {
    override fun accept(visitor: InstructionVisitor) {
        visitor.visitReturnNoValue(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitReturnNoValue(this)
    }

    override fun toString(): String = "ret $targetLabel"

    override fun createCopy(newLabel: Label, lexicalScope: LexicalScope): AbstractJumpInstruction =
            ReturnNoValueInstruction(element, lexicalScope, newLabel)
}
