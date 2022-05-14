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

package org.jetbrains.kotlin.transformers

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.VariableDescriptorImpl
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.descriptors.IrBuiltinValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltinsPackageFragmentDescriptorImpl
import org.jetbrains.kotlin.ir.descriptors.IrSimpleBuiltinOperatorDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrBinaryPrimitiveImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.types.TypeSubstitutor

typealias IrCallTransformer<T> = (IrCall) -> T

class Intrinsics(val irBuiltIns: IrBuiltIns) {

    private val KOTLIN = FqName("kotlin")
    private val KOTLIN_ANY = FqName("kotlin.Any")
    private val KOTLIN_JS = FqName("kotlin.js")

    // TODO
    val packageFragment = IrBuiltinsPackageFragmentDescriptorImpl(irBuiltIns.builtIns.builtInsModule, FqName("foo.bar"))

    private val KOTLIN_D = object : VariableDescriptorImpl(packageFragment, Annotations.EMPTY, Name.identifier("kotlin"), irBuiltIns.builtIns.anyType, org.jetbrains.kotlin.descriptors.SourceElement.NO_SOURCE) {
        override fun getCompileTimeInitializer(): ConstantValue<*>? {
            TODO("not implemented")
        }

        override fun getVisibility(): Visibility {
            TODO("not implemented")
        }

        override fun substitute(substitutor: TypeSubstitutor): VariableDescriptor? {
            TODO("not implemented")
        }

        override fun isVar() = false

        override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>?, data: D): R {
            TODO("not implemented")
        }
    }
    private val IR_KOTLIN = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, KOTLIN_D)

    private val KOTLIN_TO_STRING_D = IrSimpleBuiltinOperatorDescriptorImpl(KOTLIN_D, Name.identifier("toString"), irBuiltIns.builtIns.stringType).apply {
        addValueParameter(IrBuiltinValueParameterDescriptorImpl(this, Name.identifier("self"), 0, irBuiltIns.builtIns.nullableAnyType))
    }

    private val KOTLIN_HASH_CODE_D = IrSimpleBuiltinOperatorDescriptorImpl(KOTLIN_D, Name.identifier("hashCode"), irBuiltIns.builtIns.stringType).apply {
        addValueParameter(IrBuiltinValueParameterDescriptorImpl(this, Name.identifier("self"), 0, irBuiltIns.builtIns.nullableAnyType))
    }

    private val KOTLIN_EQUALS_D = IrSimpleBuiltinOperatorDescriptorImpl(KOTLIN_D, Name.identifier("equals"), irBuiltIns.builtIns.stringType).apply {
        addValueParameter(IrBuiltinValueParameterDescriptorImpl(this, Name.identifier("self"), 0, irBuiltIns.builtIns.nullableAnyType))
        addValueParameter(IrBuiltinValueParameterDescriptorImpl(this, Name.identifier("other"), 1, irBuiltIns.builtIns.nullableAnyType))
    }

    private val KOTLIN_ARRAY_ITERATOR = IrSimpleBuiltinOperatorDescriptorImpl(KOTLIN_D, Name.identifier("arrayIterator"), irBuiltIns.builtIns.stringType).apply {
        addValueParameter(IrBuiltinValueParameterDescriptorImpl(this, Name.identifier("self"), 0, irBuiltIns.builtIns.nullableAnyType))
    }

    private val intrinsics = IntrinsicsMap<IrCallTransformer<IrExpression>>().apply {
        add(KotlinBuiltIns.FQ_NAMES.any, "toString", 0) {
            val call = IrCallImpl(it.startOffset, it.endOffset, it.type, KOTLIN_TO_STRING_D, null, it.origin)
            call.dispatchReceiver = IR_KOTLIN
            call.putValueArgument(0, it.dispatchReceiver)
            call
        }
        add(KotlinBuiltIns.FQ_NAMES.any, "hashCode", 0) {
            val call = IrCallImpl(it.startOffset, it.endOffset, it.type, KOTLIN_HASH_CODE_D, null, it.origin)
            call.dispatchReceiver = IR_KOTLIN
            call.putValueArgument(0, it.dispatchReceiver)
            call
        }
        add(KotlinBuiltIns.FQ_NAMES.any, "equals", 1) {
            val call = IrCallImpl(it.startOffset, it.endOffset, it.type, KOTLIN_EQUALS_D, null, it.origin)
            call.dispatchReceiver = IR_KOTLIN
            call.putValueArgument(0, it.dispatchReceiver)
            call.putValueArgument(1, it.getValueArgument(0))
            call
        }
        add(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME, "equals", 1, KotlinBuiltIns.FQ_NAMES.any) {
            val call = IrCallImpl(it.startOffset, it.endOffset, it.type, KOTLIN_EQUALS_D, null, it.origin)
            call.dispatchReceiver = IR_KOTLIN
            call.putValueArgument(0, it.extensionReceiver)
            call.putValueArgument(1, it.getValueArgument(0))
            call
        }
        add(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME, "toString", 0, KotlinBuiltIns.FQ_NAMES.any) {
            val call = IrCallImpl(it.startOffset, it.endOffset, it.type, KOTLIN_TO_STRING_D, null, it.origin)
            call.dispatchReceiver = IR_KOTLIN
            call.putValueArgument(0, it.extensionReceiver)
            call
        }

        add(KotlinBuiltIns.FQ_NAMES.string, "plus", 1) {
            IrBinaryPrimitiveImpl(it.startOffset, it.endOffset, it.origin ?: IrStatementOrigin.PLUS, it.descriptor, it.dispatchReceiver!!, it.getValueArgument(0)!!)
        }

        // TODO: hacky
        add(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME, "arrayOf", 1) {
            it.getValueArgument(0)!!
        }


        with(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME + "Array") {
            add(this, "<get-size>", 0) {
                val descriptor = PropertyDescriptorImpl.create(packageFragment, Annotations.EMPTY, Modality.FINAL, Visibilities.DEFAULT_VISIBILITY, false, Name.identifier("length"), CallableMemberDescriptor.Kind.DECLARATION, org.jetbrains.kotlin.descriptors.SourceElement.NO_SOURCE, false, false, false, false, false, false)
                descriptor.setType(it.descriptor.returnType!!, it.descriptor.typeParameters, it.descriptor.dispatchReceiverParameter, it.descriptor.extensionReceiverParameter)
                IrGetFieldImpl(it.startOffset, it.endOffset, descriptor, it.dispatchReceiver ?: it.extensionReceiver)
            }

            add(this, "iterator", 0) {
                // TODO call `new ArrayIterator(arr)` instead
                val call = IrCallImpl(it.startOffset, it.endOffset, it.type, KOTLIN_ARRAY_ITERATOR, null, it.origin)
                call.dispatchReceiver = IR_KOTLIN
                call.putValueArgument(0, it.dispatchReceiver)
                call
            }
        }
    }

    private val irIntrinsics = mapOf<CallableMemberDescriptor, IrCallTransformer<IrExpression>>(
            // TODO: hack
//            irBuiltIns.booleanNot to { irCall -> irCall.getValueArgument(0)!! },

            // TODO specialize for nulls, classes with custom equals, classes with out custom equals
            irBuiltIns.eqeq to { irCall ->
                val call = IrCallImpl(irCall.startOffset, irCall.endOffset, irCall.type, KOTLIN_EQUALS_D, null, irCall.origin)
                call.dispatchReceiver = IR_KOTLIN
                call.putValueArgument(0, irCall.getValueArgument(0))
                call.putValueArgument(1, irCall.getValueArgument(1))
                call
            }
    )

    private val toJsIntrinsics = IntrinsicsMap<IrCallTransformer<JsExpression>>().apply {
        add(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME, "arrayOf", 1) {
            it
            JsNullLiteral()
        }
    }

    private operator fun FqName.plus(part: String): FqName = child(Name.identifier(part))

    private fun <T> IntrinsicsMap<IrCallTransformer<T>>.add(
            owner: FqName,
            name: String,
            valueParameterCount: Int,
            receiverParameter: FqNameUnsafe? = null,
            transform: IrCallTransformer<T>
    ) {
        registerIntrinsic(owner, receiverParameter, name, valueParameterCount, transform)
    }

    private fun <T> IntrinsicsMap<IrCallTransformer<T>>.add(
            owner: FqNameUnsafe,
            name: String,
            valueParameterCount: Int,
            receiverParameter: FqNameUnsafe? = null,
            transform: IrCallTransformer<T>
    ) {
        add(owner.toSafe(), name, valueParameterCount, receiverParameter, transform)
    }

    fun get(descriptor: CallableMemberDescriptor) = irIntrinsics[descriptor] ?: intrinsics.getIntrinsic(descriptor)
    fun getJS(descriptor: CallableMemberDescriptor) = toJsIntrinsics.getIntrinsic(descriptor)
}

// intrinsics
// descriptor -> IrExpression
// IrCall -> JsExpression


/*
 Any:
 * .equals
 * .toString
 * .hashCode
 * ext fun Any?.equals ???
 * ext fun Any?.toString ???

 Array:
 * factory methods [in js?]
 * get [gen js?]
 * set [gen js?]
 * <get-size>
 * iterator -> ?
 * ctor for Arrays of primitives with size
 * ctor for Arrays of primitives with size and lambda

 Other:
 * String|Boolean|Char|Number.equals -> Kotlin.equals ???
 * kotlin.js.jsClass -> JS_CLASS_FUN_INTRINSIC
 * kotlin.arrayOfNulls -> KotlinFunctionIntrinsic("nullArray")
 * ext fun Iterator.iterator in "kotlin" -> RETURN_RECEIVER_INTRINSIC
 * java.util.HashMap.<init> ???
 * java.util.HashSet.<init> ???

 <primitive numbers> and char
 * .equals -> ===
 * .unaryPlus -> +e
 * .unaryMinus -> -e
 * .plus -> a + b
 * .minus -> a - b
 * .inc -> a++
 * .dec -> a--
 * Char.plus|minus(Int) -> a + b
 * Char.rangeTo(Char) -> ???
 * .compareTo
 * .rangeTo
 * Int|Short|Byte.div(Int|Short|Byte) -> INTEGER_DIVISION_INTRINSIC: (a/b)|0

 WAT???
 * kotlin.collections.Map.get -> NATIVE_MAP_GET
 * ext fun MutableMap.set in package "kotlin.js" -> NATIVE_MAP_SET

 String
 * kotlin.CharSequence.get -> .charAt / charCodeAt
 * kotlin.CharSequence.<get-length> -> .length
 * kotlin.CharSequence.subSequence -> BuiltInFunctionIntrinsic("substring") [Kotlin.subSequence]

 NumberAndCharConversionFIF
 Progression
 ProgressionCompanion
 StandardClasses.java
 Long

 */
