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

package org.jetbrains.kotlin.types.expressions

import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtMultiDeclaration
import org.jetbrains.kotlin.psi.KtMultiDeclarationEntry
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorResolver
import org.jetbrains.kotlin.resolve.TypeResolver
import org.jetbrains.kotlin.resolve.dataClassUtils.createComponentName
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.utils.getLocalVariable
import org.jetbrains.kotlin.resolve.validation.SymbolUsageValidator
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

public class MultiDeclarationResolver(
        private val fakeCallResolver: FakeCallResolver,
        private val descriptorResolver: DescriptorResolver,
        private val typeResolver: TypeResolver,
        private val symbolUsageValidator: SymbolUsageValidator
) {
    public fun defineLocalVariablesFromMultiDeclaration(
            writableScope: LexicalWritableScope,
            multiDeclaration: KtMultiDeclaration,
            receiver: ReceiverValue,
            reportErrorsOn: KtExpression,
            context: ExpressionTypingContext
    ) {
        for ((componentIndex, entry) in multiDeclaration.getEntries().withIndex()) {
            val componentName = createComponentName(componentIndex + 1)

            val expectedType = getExpectedTypeForComponent(context, entry)
            val results = fakeCallResolver.resolveFakeCall(context.replaceExpectedType(expectedType), receiver, componentName, entry)

            var componentType: KotlinType? = null
            if (results.isSuccess()) {
                context.trace.record(BindingContext.COMPONENT_RESOLVED_CALL, entry, results.getResultingCall())

                val functionDescriptor = results.getResultingDescriptor()
                symbolUsageValidator.validateCall(null, functionDescriptor, context.trace, entry)

                componentType = functionDescriptor.getReturnType()
                if (componentType != null && !TypeUtils.noExpectedType(expectedType) && !KotlinTypeChecker.DEFAULT.isSubtypeOf(componentType, expectedType)) {
                    context.trace.report(Errors.COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH.on(reportErrorsOn, componentName, componentType, expectedType))
                }
            }
            else if (results.isAmbiguity()) {
                context.trace.report(Errors.COMPONENT_FUNCTION_AMBIGUITY.on(reportErrorsOn, componentName, results.getResultingCalls()))
            }
            else {
                context.trace.report(Errors.COMPONENT_FUNCTION_MISSING.on(reportErrorsOn, componentName, receiver.getType()))
            }
            if (componentType == null) {
                componentType = ErrorUtils.createErrorType("$componentName() return type")
            }
            val variableDescriptor = descriptorResolver.resolveLocalVariableDescriptorWithType(writableScope, entry, componentType, context.trace)

            val olderVariable = writableScope.getLocalVariable(variableDescriptor.getName())
            ExpressionTypingUtils.checkVariableShadowing(context, variableDescriptor, olderVariable)

            writableScope.addVariableDescriptor(variableDescriptor)
        }
    }

    private fun getExpectedTypeForComponent(context: ExpressionTypingContext, entry: KtMultiDeclarationEntry): KotlinType {
        val entryTypeRef = entry.getTypeReference() ?: return TypeUtils.NO_EXPECTED_TYPE
        return typeResolver.resolveType(context.scope, entryTypeRef, context.trace, true)
    }
}