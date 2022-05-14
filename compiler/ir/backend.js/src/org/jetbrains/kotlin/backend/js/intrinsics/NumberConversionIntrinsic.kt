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

package org.jetbrains.kotlin.backend.js.intrinsics

import org.jetbrains.kotlin.backend.js.context.FunctionIntrinsic
import org.jetbrains.kotlin.backend.js.context.IrTranslationContext
import org.jetbrains.kotlin.backend.js.util.buildJs
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe

object NumberConversionIntrinsic : FunctionIntrinsic {
    private val conversionFunctions = setOf("toByte", "toShort", "toInt", "toLong", "toFloat", "toDouble", "toChar")

    override fun isApplicable(descriptor: FunctionDescriptor): Boolean {
        val owner = (descriptor.containingDeclaration as? ClassDescriptor) ?: return false
        return owner.fqNameUnsafe == KotlinBuiltIns.FQ_NAMES.number &&
               descriptor.name.asString() in conversionFunctions &&
               descriptor.valueParameters.isEmpty()
    }

    override fun apply(
            context: IrTranslationContext, call: IrCall,
            dispatchReceiver: JsExpression?, extensionReceiver: JsExpression?, arguments: List<JsExpression>
    ): JsExpression? {
        val simpleName = call.descriptor.name.asString()
        val name = if (simpleName == "toFloat") "ToDouble" else simpleName.capitalize()
        return buildJs { "Kotlin".dotPure("number$name").invoke(dispatchReceiver!!).pure() }
    }
}