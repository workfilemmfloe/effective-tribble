/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.cfg.pseudocode

import org.jetbrains.jet.lang.cfg.pseudocode.instructions.*
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.eval.*
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.jumps.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.bindingContextUtil.*
import org.jetbrains.jet.lang.resolve.calls.model.*
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.resolve.BindingContext
import java.util.*
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.jet.lang.resolve.OverridingUtil
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.descriptors.impl.LocalVariableDescriptor

fun getReceiverTypePredicate(resolvedCall: ResolvedCall<*>, receiverValue: ReceiverValue): TypePredicate? {
    val callableDescriptor = resolvedCall.getResultingDescriptor()
    if (callableDescriptor == null) return null

    when (receiverValue) {
        resolvedCall.getExtensionReceiver() -> {
            val receiverParameter = callableDescriptor.getExtensionReceiverParameter()
            if (receiverParameter != null) return receiverParameter.getType().getSubtypesPredicate()
        }
        resolvedCall.getDispatchReceiver() -> {
            val rootCallableDescriptors = OverridingUtil.getTopmostOverridenDescriptors(callableDescriptor)
            return or(rootCallableDescriptors.map {
                it.getDispatchReceiverParameter()?.getType()?.let { TypeUtils.makeNullableIfNeeded(it, resolvedCall.isSafeCall()) }?.getSubtypesPredicate()
            }.filterNotNull())
        }
    }

    return null
}

public fun getExpectedTypePredicate(value: PseudoValue, bindingContext: BindingContext): TypePredicate {
    val pseudocode = value.createdAt?.owner
    if (pseudocode == null) return AllTypes

    val typePredicates = LinkedHashSet<TypePredicate?>()

    fun addSubtypesOf(jetType: JetType?) = typePredicates.add(jetType?.getSubtypesPredicate())

    fun addTypePredicates(value: PseudoValue) {
        pseudocode.getUsages(value).forEach {
            when (it) {
                is ReturnValueInstruction -> {
                    val returnElement = it.element
                    val functionDescriptor = when(returnElement) {
                        is JetReturnExpression -> returnElement.getTargetFunctionDescriptor(bindingContext)
                        else -> bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, pseudocode.getCorrespondingElement()]
                    }
                    addSubtypesOf((functionDescriptor as? CallableDescriptor)?.getReturnType())
                }

                is ConditionalJumpInstruction ->
                    addSubtypesOf(KotlinBuiltIns.getInstance().getBooleanType())

                is ThrowExceptionInstruction ->
                    addSubtypesOf(KotlinBuiltIns.getInstance().getThrowable().getDefaultType())

                is MergeInstruction ->
                    addTypePredicates(it.outputValue)

                is AccessValueInstruction -> {
                    val accessTarget = it.target
                    val receiverValue = it.receiverValues[value]
                    if (receiverValue != null) {
                        typePredicates.add(getReceiverTypePredicate((accessTarget as AccessTarget.Call).resolvedCall, receiverValue))
                    }
                    else {
                        val expectedType = when (accessTarget) {
                            is AccessTarget.Call ->
                                (accessTarget.resolvedCall.getResultingDescriptor() as? VariableDescriptor)?.getType()
                            is AccessTarget.Declaration ->
                                accessTarget.descriptor.getType()
                            else ->
                                null
                        }
                        addSubtypesOf(expectedType)
                    }
                }

                is CallInstruction -> {
                    val receiverValue = it.receiverValues[value]
                    if (receiverValue != null) {
                        typePredicates.add(getReceiverTypePredicate(it.resolvedCall, receiverValue))
                    }
                    else {
                        it.arguments[value]?.let { parameter ->
                            val expectedType = when (it.resolvedCall.getValueArguments()[parameter]) {
                                is VarargValueArgument ->
                                    parameter.getVarargElementType()
                                else ->
                                    parameter.getType()
                            }
                            addSubtypesOf(expectedType)
                        }
                    }
                }

                is MagicInstruction ->
                    typePredicates.add(it.expectedTypes[value])
            }
        }
    }

    addTypePredicates(value)
    return and(typePredicates.filterNotNull())
}

public fun Instruction.getPrimaryDeclarationDescriptorIfAny(bindingContext: BindingContext): DeclarationDescriptor? {
    return when (this) {
        is CallInstruction -> return resolvedCall.getResultingDescriptor()
        else -> PseudocodeUtil.extractVariableDescriptorIfAny(this, false, bindingContext)
    }
}

public val Instruction.sideEffectFree: Boolean
    get() = owner.isSideEffectFree(this)

private fun Instruction.calcSideEffectFree(): Boolean {
    if (this !is InstructionWithValue) return false
    if (!inputValues.all { it.createdAt?.sideEffectFree ?: false }) return false

    return when (this) {
        is ReadValueInstruction -> target.let {
            when (it) {
                is AccessTarget.Call -> when (it.resolvedCall.getResultingDescriptor()) {
                    is LocalVariableDescriptor, is ValueParameterDescriptor, is ReceiverParameterDescriptor -> true
                    else -> false
                }

                else -> when (element) {
                    is JetConstantExpression, is JetFunctionLiteralExpression, is JetStringTemplateExpression -> true
                    else -> false
                }
            }
        }

        is MagicInstruction -> kind.sideEffectFree

        else -> false
    }
}

fun Pseudocode.getElementValuesRecursively(element: JetElement): List<PseudoValue> {
    val results = ArrayList<PseudoValue>()

    fun Pseudocode.collectValues() {
        getElementValue(element)?.let { results.add(it) }
        for (localFunction in getLocalDeclarations()) {
            localFunction.body.collectValues()
        }
    }

    collectValues()
    return results
}