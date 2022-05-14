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

import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension

fun propertyAccessToFieldAccess(call: IrCall): IrExpression {
    val descriptor = call.descriptor

    if (descriptor.isExtension) return call

    return when(descriptor) {
        is PropertyGetterDescriptor ->
            IrGetFieldImpl(call.startOffset, call.endOffset, descriptor.correspondingProperty, call.dispatchReceiver, call.origin, call.superQualifier)
        is PropertySetterDescriptor ->
            IrSetFieldImpl(call.startOffset, call.endOffset, descriptor.correspondingProperty, call.dispatchReceiver, call.getValueArgument(0)!!, call.origin, call.superQualifier)
        else ->
            call
    }

}
