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

package org.jetbrains.kotlin.js.translate.reference

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.general.Translation
import org.jetbrains.kotlin.js.translate.utils.*
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.PropertyImportedFromObject
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS
import java.util.*

object CallableReferenceTranslator {

    fun translate(expression: KtCallableReferenceExpression, context: TranslationContext): JsExpression {
        val descriptor = BindingUtils.getDescriptorForReferenceExpression(context.bindingContext(), expression.callableReference)

        val receiver = expression.receiverExpression?.let { r ->
            if (context.bindingContext().get(BindingContext.DOUBLE_COLON_LHS, r) is DoubleColonLHS.Expression) {
                val block = JsBlock()
                val e = Translation.translateAsExpression(r, context, block)
                if (!block.isEmpty) {
                    context.addStatementsToCurrentBlockFrom(block)
                }
                e
            }
            else {
                null
            }
        } ?: (descriptor as? PropertyImportedFromObject)?.let {
            ReferenceTranslator.translateAsValueReference(it.containingObject, context)
        }

        return when (descriptor) {
            is PropertyDescriptor ->
                translateForProperty(descriptor, context, expression, receiver)
            is FunctionDescriptor ->
                translateForFunction(descriptor, context, expression, receiver)
            else ->
                throw IllegalArgumentException("Expected property or function: $descriptor, expression=${expression.text}")
        }
    }

    private fun reportNotSupported(context: TranslationContext, expression: KtCallableReferenceExpression): JsExpression {
        context.bindingTrace().report(ErrorsJs.REFERENCE_TO_BUILTIN_MEMBERS_NOT_SUPPORTED.on(expression, expression))
        return JsLiteral.NULL
    }

    private fun translateForFunction(
            descriptor: FunctionDescriptor,
            context: TranslationContext,
            expression: KtCallableReferenceExpression,
            receiver: JsExpression?
    ): JsExpression {
        return when {
        // TODO Support for callable reference to builtin functions and members
            KotlinBuiltIns.isBuiltIn(descriptor) ->
                reportNotSupported(context, expression)
            isConstructor(descriptor) ->
                translateForConstructor(descriptor, context)
            isExtension(descriptor) ->
                translateForExtensionFunction(descriptor, context, receiver)
            isMember(descriptor) ->
                translateForMemberFunction(descriptor, context, receiver)
            else ->
                ReferenceTranslator.translateAsValueReference(descriptor, context)
        }
    }

    private fun translateForProperty(
            descriptor: PropertyDescriptor,
            context: TranslationContext,
            expression: KtCallableReferenceExpression,
            receiver: JsExpression?
    ): JsExpression {
        return when {
        // TODO Support for callable reference to builtin properties
            KotlinBuiltIns.isBuiltIn(descriptor) ->
                reportNotSupported(context, expression)
            isExtension(descriptor) ->
                translateForExtensionProperty(descriptor, context, receiver)
            isMember(descriptor) ->
                translateForMemberProperty(descriptor, context, receiver)
            else ->
                translateForTopLevelProperty(descriptor, context)
        }
    }

    private fun isConstructor(descriptor: CallableDescriptor) = descriptor is ConstructorDescriptor

    private fun isExtension(descriptor: CallableDescriptor) = DescriptorUtils.isExtension(descriptor)

    private fun isMember(descriptor: CallableDescriptor) = JsDescriptorUtils.getContainingDeclaration(descriptor) is ClassDescriptor

    private fun isVar(descriptor: PropertyDescriptor) = if (descriptor.isVar) JsLiteral.TRUE else JsLiteral.FALSE

    private fun translateForTopLevelProperty(descriptor: PropertyDescriptor, context: TranslationContext): JsExpression {
        val packageDescriptor = JsDescriptorUtils.getContainingDeclaration(descriptor)
        assert(packageDescriptor is PackageFragmentDescriptor) { "Expected PackageFragmentDescriptor: $packageDescriptor" }

        val jsPropertyName = context.getNameForDescriptor(descriptor)
        val jsPropertyNameAsString = context.program().getStringLiteral(jsPropertyName.toString())

        val getter = createTopLevelGetterFunction(descriptor, context)
        val setter = if (descriptor.isVar) createTopLevelSetterFunction(descriptor, context) else JsLiteral.NULL

        return JsInvocation(context.namer().callableRefForTopLevelPropertyReference(), getter, setter, jsPropertyNameAsString)
    }

    private fun createTopLevelGetterFunction(descriptor: PropertyDescriptor, context: TranslationContext): JsExpression {
        val getter = descriptor.getter!!
        if (!JsDescriptorUtils.isSimpleFinalProperty(descriptor) && context.isFromCurrentModule(descriptor)) {
            return context.getInnerReference(getter)
        }

        val expression = if (TranslationUtils.shouldAccessViaFunctions(descriptor)) {
            JsInvocation(ReferenceTranslator.translateAsValueReference(getter, context))
        }
        else {
            ReferenceTranslator.translateAsValueReference(descriptor, context)
        }

        val function = context.createRootScopedFunction(getter)
        function.body.statements += JsReturn(expression)

        return function
    }

    private fun createTopLevelSetterFunction(descriptor: PropertyDescriptor, context: TranslationContext): JsExpression {
        val setter = descriptor.setter!!
        if (!JsDescriptorUtils.isSimpleFinalProperty(descriptor) && context.isFromCurrentModule(descriptor)) {
            return context.getInnerReference(setter)
        }

        val function = context.createRootScopedFunction(setter)
        val valueParam = function.scope.declareTemporaryName("value")
        function.parameters += JsParameter(valueParam)

        val expression = if (TranslationUtils.shouldAccessViaFunctions(descriptor)) {
            JsInvocation(ReferenceTranslator.translateAsValueReference(setter, context), valueParam.makeRef())
        }
        else {
            JsAstUtils.assignment(ReferenceTranslator.translateAsValueReference(descriptor, context), valueParam.makeRef())
        }

        function.body.statements += expression.makeStmt()

        return function
    }

    private fun translateForMemberProperty(
            descriptor: PropertyDescriptor,
            context: TranslationContext,
            receiver: JsExpression?
    ): JsExpression {
        val jsPropertyName = context.getNameForDescriptor(descriptor)
        val jsPropertyNameAsString = context.program().getStringLiteral(jsPropertyName.toString())
        if (receiver == null) {
            return JsInvocation(context.namer().callableRefForMemberPropertyReference(), jsPropertyNameAsString, isVar(descriptor))
        }
        else {
            return JsInvocation(context.namer().boundCallableRefForMemberPropertyReference(), receiver, jsPropertyNameAsString,
                                isVar(descriptor))
        }
    }

    private fun translateForExtensionProperty(
            descriptor: PropertyDescriptor,
            context: TranslationContext,
            receiver: JsExpression?
    ): JsExpression {
        val jsGetterNameRef = ReferenceTranslator.translateAsValueReference(descriptor.getter!!, context)
        val propertyName = descriptor.name
        val jsPropertyNameAsString = context.program().getStringLiteral(propertyName.asString())
        val argumentList = ArrayList<JsExpression>(4)
        if (receiver != null) {
            argumentList.add(receiver)
        }
        argumentList.add(jsPropertyNameAsString)
        argumentList.add(jsGetterNameRef)
        if (descriptor.isVar) {
            val jsSetterNameRef = ReferenceTranslator.translateAsValueReference(descriptor.setter!!, context)
            argumentList.add(jsSetterNameRef)
        }
        return if (AnnotationsUtils.isNativeObject(descriptor)) {
            translateForMemberProperty(descriptor, context, receiver)
        }
        else if (receiver == null) {
            JsInvocation(context.namer().callableRefForExtensionPropertyReference(), argumentList)
        }
        else {
            JsInvocation(context.namer().boundCallableRefForExtensionPropertyReference(), argumentList)
        }
    }

    private fun translateForConstructor(descriptor: FunctionDescriptor, context: TranslationContext): JsExpression {
        val jsFunctionRef = ReferenceTranslator.translateAsValueReference(descriptor, context)
        return JsInvocation(context.namer().callableRefForConstructorReference(), jsFunctionRef)
    }

    private fun translateForExtensionFunction(descriptor: FunctionDescriptor,
                                              context: TranslationContext,
                                              receiver: JsExpression?
    ): JsExpression {
        val jsFunctionRef = ReferenceTranslator.translateAsValueReference(descriptor, context)
        if (AnnotationsUtils.isNativeObject(descriptor)) {
            return translateForMemberFunction(descriptor, context, receiver)
        }
        else {
            if (receiver == null) {
                return JsInvocation(context.namer().callableRefForExtensionFunctionReference(), jsFunctionRef)
            }
            else {
                return JsInvocation(context.namer().boundCallableRefForExtensionFunctionReference(), receiver, jsFunctionRef)
            }
        }
    }

    private fun translateForMemberFunction(
            descriptor: CallableDescriptor,
            context: TranslationContext,
            receiver: JsExpression?
    ): JsExpression {
        val funName = context.getNameForDescriptor(descriptor)
        val funNameAsString = context.program().getStringLiteral(funName.toString())
        if (receiver == null) {
            return JsInvocation(context.namer().callableRefForMemberFunctionReference(), funNameAsString)
        }
        else {
            return JsInvocation(context.namer().boundCallableRefForMemberFunctionReference(), receiver, funNameAsString)
        }
    }
}
