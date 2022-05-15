/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.CompilerPhase
import org.jetbrains.kotlin.backend.common.IrFileEndPhase
import org.jetbrains.kotlin.backend.common.IrFileStartPhase
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.jvm.lower.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.PatchDeclarationParentsVisitor
import org.jetbrains.kotlin.ir.visitors.acceptVoid

val jvmPhases = listOf(
    IrFileStartPhase,

    JvmCoercionToUnitPhase,
    FileClassPhase,
    KCallableNamePropertyPhase,

    makeLateinitPhase(true),

    MoveCompanionObjectFieldsPhase,
    ConstAndJvmFieldPropertiesPhase,
    PropertiesPhase,
    AnnotationPhase,

    makeDefaultArgumentStubPhase(false),

    InterfacePhase,
    InterfaceDelegationPhase,
    SharedVariablesPhase,

    makePatchParentsPhase(1),

    JvmLocalDeclarationsPhase,
    CallableReferencePhase,
    FunctionNVarargInvokePhase,

    InnerClassesPhase,
    InnerClassConstructorCallsPhase,

    makePatchParentsPhase(2),

    EnumClassPhase,
    ObjectClassPhase,
    makeInitializersPhase(JvmLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER, true),
    SingletonReferencesPhase,
    SyntheticAccessorPhase,
    BridgePhase,
    JvmOverloadsAnnotationPhase,
    JvmStaticAnnotationPhase,
    StaticDefaultFunctionPhase,

    TailrecPhase,
    ToArrayPhase,

    makePatchParentsPhase(3),

    IrFileEndPhase
)

fun makePatchParentsPhase(number: Int) = object : CompilerPhase<BackendContext, IrFile> {
    override val name: String = "PatchParents$number"
    override val description: String = "Patch parent references in IrFile, pass $number"
    override val prerequisite: Set<CompilerPhase<BackendContext, *>> = emptySet()

    override fun invoke(context: BackendContext, input: IrFile): IrFile {
        input.acceptVoid(PatchDeclarationParentsVisitor())
        return input
    }
}

class JvmLower(val context: JvmBackendContext) {
    fun lower(irFile: IrFile) {
        var state = irFile
        // TODO run lowering passes as callbacks in bottom-up visitor
       
	    context.rootPhaseManager(irFile).apply {
            for (jvmPhase in jvmPhases) {
                state = phase(jvmPhase, context, state)
            }
        }
    }
}
