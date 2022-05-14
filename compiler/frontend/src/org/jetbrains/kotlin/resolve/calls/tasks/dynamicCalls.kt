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

package org.jetbrains.kotlin.resolve.calls.tasks

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.calls.tasks.collectors.CallableDescriptorCollector
import org.jetbrains.kotlin.resolve.calls.tasks.collectors.CallableDescriptorCollectors
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.createDynamicType
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.utils.Printer
import java.util.*

class DynamicCallableDescriptors(private val builtIns: KotlinBuiltIns) {

    val dynamicType = createDynamicType(builtIns)

    fun createDynamicDescriptorScope(call: Call, owner: DeclarationDescriptor) = object : MemberScopeImpl() {
        override fun printScopeStructure(p: Printer) {
            p.println(javaClass.getSimpleName(), ": dynamic candidates for " + call)
        }

        override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
            if (isAugmentedAssignmentConvention(name)) return listOf()
            if (call.getCallType() == Call.CallType.INVOKE
                && call.getValueArgumentList() == null && call.getFunctionLiteralArguments().isEmpty()) {
                // this means that we are looking for "imaginary" invokes,
                // e.g. in `+d` we are looking for property "plus" with member "invoke"
                return listOf()
            }
            return listOf(createDynamicFunction(owner, name, call))
        }

        /*
         * Detects the case when name "plusAssign" is requested for "+=" call,
         * since both "plus" and "plusAssign" are resolvable on dynamic receivers,
         * we have to prefer ne of them, and prefer "plusAssign" for generality:
         * it may be called even on a val
         */
        private fun isAugmentedAssignmentConvention(name: Name): Boolean {
            val callee = call.getCalleeExpression()
            if (callee is KtOperationReferenceExpression) {
                val token = callee.getReferencedNameElementType()
                if (token in KtTokens.AUGMENTED_ASSIGNMENTS && OperatorConventions.ASSIGNMENT_OPERATIONS[token] != name) {
                    return true
                }
            }
            return false
        }

        override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
            return if (call.getValueArgumentList() == null && call.getValueArguments().isEmpty()) {
                listOf(createDynamicProperty(owner, name, call))
            }
            else listOf()
        }
    }

    private fun createDynamicProperty(owner: DeclarationDescriptor, name: Name, call: Call): PropertyDescriptorImpl {
        val propertyDescriptor = PropertyDescriptorImpl.create(
                owner,
                Annotations.EMPTY,
                Modality.FINAL,
                Visibilities.PUBLIC,
                true,
                name,
                CallableMemberDescriptor.Kind.DECLARATION,
                SourceElement.NO_SOURCE,
                /* lateInit = */ false,
                /* isConst = */ false
        )
        propertyDescriptor.setType(
                dynamicType,
                createTypeParameters(propertyDescriptor, call),
                createDynamicDispatchReceiverParameter(propertyDescriptor),
                null as KotlinType?
        )

        val getter = DescriptorFactory.createDefaultGetter(propertyDescriptor, Annotations.EMPTY)
        getter.initialize(propertyDescriptor.getType())
        val setter = DescriptorFactory.createDefaultSetter(propertyDescriptor, Annotations.EMPTY)

        propertyDescriptor.initialize(getter, setter)

        return propertyDescriptor
    }

    private fun createDynamicFunction(owner: DeclarationDescriptor, name: Name, call: Call): SimpleFunctionDescriptorImpl {
        val functionDescriptor = SimpleFunctionDescriptorImpl.create(
                owner,
                Annotations.EMPTY,
                name,
                CallableMemberDescriptor.Kind.DECLARATION,
                SourceElement.NO_SOURCE
        )
        functionDescriptor.initialize(
                null,
                createDynamicDispatchReceiverParameter(functionDescriptor),
                createTypeParameters(functionDescriptor, call),
                createValueParameters(functionDescriptor, call),
                dynamicType,
                Modality.FINAL,
                Visibilities.PUBLIC
        )
        return functionDescriptor
    }

    private fun createDynamicDispatchReceiverParameter(owner: CallableDescriptor): ReceiverParameterDescriptorImpl {
        return ReceiverParameterDescriptorImpl(owner, TransientReceiver(dynamicType))
    }

    private fun createTypeParameters(owner: DeclarationDescriptor, call: Call): List<TypeParameterDescriptor> = call.getTypeArguments().indices.map {
        index
        ->
        TypeParameterDescriptorImpl.createWithDefaultBound(
                owner,
                Annotations.EMPTY,
                false,
                Variance.INVARIANT,
                Name.identifier("T$index"),
                index
        )
    }

    private fun createValueParameters(owner: FunctionDescriptor, call: Call): List<ValueParameterDescriptor> {
        val parameters = ArrayList<ValueParameterDescriptor>()

        fun addParameter(arg : ValueArgument, outType: KotlinType, varargElementType: KotlinType?) {
            val index = parameters.size

            parameters.add(ValueParameterDescriptorImpl(
                    owner,
                    null,
                    index,
                    Annotations.EMPTY,
                    arg.getArgumentName()?.asName ?: Name.identifier("p$index"),
                    outType,
                    /* declaresDefaultValue = */ false,
                    /* isCrossinline = */ false,
                    /* isNoinline = */ false,
                    varargElementType,
                    SourceElement.NO_SOURCE
            ))
        }

        fun getFunctionType(funLiteralExpr: KtLambdaExpression): KotlinType {
            val funLiteral = funLiteralExpr.getFunctionLiteral()

            val receiverType = funLiteral.getReceiverTypeReference()?.let { dynamicType }
            val parameterTypes = funLiteral.getValueParameters().map { dynamicType }

            return owner.builtIns.getFunctionType(Annotations.EMPTY, receiverType, parameterTypes, dynamicType)
        }

        for (arg in call.getValueArguments()) {
            val outType: KotlinType
            val varargElementType: KotlinType?
            var hasSpreadOperator = false

            val argExpression = KtPsiUtil.deparenthesize(arg.getArgumentExpression())

            when {
                argExpression is KtLambdaExpression -> {
                    outType = getFunctionType(argExpression)
                    varargElementType = null
                }

                arg.getSpreadElement() != null -> {
                    hasSpreadOperator = true
                    outType = owner.builtIns.getArrayType(Variance.OUT_VARIANCE, dynamicType)
                    varargElementType = dynamicType
                }

                else -> {
                    outType = dynamicType
                    varargElementType = null
                }
            }

            addParameter(arg, outType, varargElementType)

            if (hasSpreadOperator) {
                for (funLiteralArg in call.getFunctionLiteralArguments()) {
                    addParameter(funLiteralArg, getFunctionType(funLiteralArg.getLambdaExpression()), null)
                }

                break
            }
        }

        return parameters
    }
}

public fun DeclarationDescriptor.isDynamic(): Boolean {
    if (this !is CallableDescriptor) return false
    val dispatchReceiverParameter = getDispatchReceiverParameter()
    return dispatchReceiverParameter != null && dispatchReceiverParameter.getType().isDynamic()
}

class CollectorForDynamicReceivers<D: CallableDescriptor>(val delegate: CallableDescriptorCollector<D>) : CallableDescriptorCollector<D> by delegate {
    override fun getExtensionsByName(scope: HierarchicalScope, syntheticScopes: SyntheticScopes, name: Name, receiverTypes: Collection<KotlinType>, location: LookupLocation): Collection<D> {
        return delegate.getExtensionsByName(scope, syntheticScopes, name, receiverTypes, location).filter {
            it.getExtensionReceiverParameter()?.getType()?.isDynamic() ?: false
        }
    }
}

fun <D : CallableDescriptor> CallableDescriptorCollectors<D>.onlyDynamicReceivers(): CallableDescriptorCollectors<D> {
    return CallableDescriptorCollectors(this.map { CollectorForDynamicReceivers(it) })
}
