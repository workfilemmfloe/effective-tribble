/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.org.objectweb.asm.Type

enum class ValueKind {
    GENERAL,
    GENERAL_VARARG,
    DEFAULT_PARAMETER,
    DEFAULT_INLINE_PARAMETER,
    DEFAULT_MASK,
    METHOD_HANDLE_IN_DEFAULT,
    READ_OF_INLINE_LAMBDA_FOR_INLINE_SUSPEND_PARAMETER,
    READ_OF_OBJECT_FOR_INLINE_SUSPEND_PARAMETER
}

interface CallGenerator {

    class DefaultCallGenerator(private val codegen: ExpressionCodegen) : CallGenerator {

        override fun genCallInner(
            callableMethod: Callable,
            resolvedCall: ResolvedCall<*>?,
            callDefault: Boolean,
            codegen: ExpressionCodegen
        ) {
            if (!callDefault) {
                callableMethod.genInvokeInstruction(codegen.v)
            } else {
                (callableMethod as CallableMethod).genInvokeDefaultInstruction(codegen.v)
            }
        }

        override fun processHiddenParameters() {}

        override fun putHiddenParamsIntoLocals() {}

        override fun genValueAndPut(
            valueParameterDescriptor: ValueParameterDescriptor?,
            argumentExpression: KtExpression,
            parameterType: JvmKotlinType,
            parameterIndex: Int
        ) {
            val container = valueParameterDescriptor?.containingDeclaration
            val isVarargInvoke = container != null && JvmCodegenUtil.isDeclarationOfBigArityFunctionInvoke(container)

            val v = codegen.v
            if (isVarargInvoke) {
                if (parameterIndex == 0) {
                    v.iconst(container!!.valueParameters.size)
                    v.newarray(OBJECT_TYPE)
                }
                v.dup()
                v.iconst(parameterIndex)
            }

            val value = codegen.gen(argumentExpression)
            value.put(parameterType.type, parameterType.kotlinType, v)

            if (isVarargInvoke) {
                v.astore(OBJECT_TYPE)
            }
        }

        override fun putCapturedValueOnStack(stackValue: StackValue, valueType: Type, paramIndex: Int) {
            stackValue.put(stackValue.type, stackValue.kotlinType, codegen.v)
        }

        override fun putValueIfNeeded(parameterType: JvmKotlinType, value: StackValue, kind: ValueKind, parameterIndex: Int) {
            value.put(value.type, value.kotlinType, codegen.v)
        }

        override fun reorderArgumentsIfNeeded(actualArgsWithDeclIndex: List<ArgumentAndDeclIndex>, valueParameterTypes: List<Type>) {
            val mark = codegen.myFrameMap.mark()
            val reordered = actualArgsWithDeclIndex.withIndex().dropWhile {
                it.value.declIndex == it.index
            }

            reordered.reversed().map {
                val argumentAndDeclIndex = it.value
                val type = valueParameterTypes.get(argumentAndDeclIndex.declIndex)
                val stackValue = StackValue.local(codegen.frameMap.enterTemp(type), type)
                stackValue.store(StackValue.onStack(type), codegen.v)
                Pair(argumentAndDeclIndex.declIndex, stackValue)
            }.sortedBy {
                it.first
            }.forEach {
                it.second.put(valueParameterTypes.get(it.first), codegen.v)
            }
            mark.dropTo()
        }
    }

    fun genCall(callableMethod: Callable, resolvedCall: ResolvedCall<*>?, callDefault: Boolean, codegen: ExpressionCodegen) {
        if (resolvedCall != null) {
            val calleeExpression = resolvedCall.call.calleeExpression
            if (calleeExpression != null) {
                codegen.markStartLineNumber(calleeExpression)
            }
        }

        genCallInner(callableMethod, resolvedCall, callDefault, codegen)
    }

    fun genCallInner(callableMethod: Callable, resolvedCall: ResolvedCall<*>?, callDefault: Boolean, codegen: ExpressionCodegen)

    fun genValueAndPut(
        valueParameterDescriptor: ValueParameterDescriptor?,
        argumentExpression: KtExpression,
        parameterType: JvmKotlinType,
        parameterIndex: Int
    )

    fun putValueIfNeeded(parameterType: JvmKotlinType, value: StackValue) {
        putValueIfNeeded(parameterType, value, ValueKind.GENERAL)
    }

    fun putValueIfNeeded(
        parameterType: JvmKotlinType,
        value: StackValue,
        kind: ValueKind = ValueKind.GENERAL,
        parameterIndex: Int = -1
    )

    fun putCapturedValueOnStack(
        stackValue: StackValue,
        valueType: Type,
        paramIndex: Int
    )

    fun processHiddenParameters()

    fun putHiddenParamsIntoLocals()

    fun reorderArgumentsIfNeeded(actualArgsWithDeclIndex: List<ArgumentAndDeclIndex>, valueParameterTypes: List<Type>)
}
