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

package org.jetbrains.kotlin.resolve.calls.results

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.types.KotlinType

class CandidateCallWithArgumentMapping<D : CallableDescriptor, K> private constructor(
        val resolvedCall: MutableResolvedCall<D>,
        private val argumentsToParameters: Map<K, ValueParameterDescriptor>,
        val parametersWithDefaultValuesCount: Int
) {
    override fun toString(): String =
            "${resolvedCall.call}: $parametersWithDefaultValuesCount defaults in ${resolvedCall.candidateDescriptor}"

    val candidateDescriptor: D
        get() = resolvedCall.candidateDescriptor

    val hasVarargs: Boolean
        get() = candidateDescriptor.valueParameters.any { it.varargElementType != null }

    val typeParameters: List<TypeParameterDescriptor>
        get() = candidateDescriptor.original.typeParameters

    val argumentsCount: Int
        get() = argumentsToParameters.size

    val argumentKeys: Collection<K>
        get() = argumentsToParameters.keys

    val isGeneric: Boolean = typeParameters.isNotEmpty()

    val extensionReceiverType: KotlinType?
        get() = candidateDescriptor.extensionReceiverParameter?.type

    /**
     * Returns the type of a value that can be used in place of the corresponding parameter.
     */
    fun getValueParameterType(argumentKey: K): KotlinType? =
            argumentsToParameters[argumentKey]?.let {
                valueParameterDescriptor ->
                valueParameterDescriptor.varargElementType ?: valueParameterDescriptor.type
            }

    companion object {
        fun <D : CallableDescriptor, K> create(
                call: MutableResolvedCall<D>,
                resolvedArgumentToKeys: (ResolvedValueArgument) -> Collection<K>
        ): CandidateCallWithArgumentMapping<D, K> {
            val argumentsToParameters = hashMapOf<K, ValueParameterDescriptor>()
            var parametersWithDefaultValuesCount = 0

            val unsubstitutedValueParameters = call.candidateDescriptor.original.valueParameters
            for ((valueParameterDescriptor, resolvedValueArgument) in call.unsubstitutedValueArguments.entries) {
                if (resolvedValueArgument is DefaultValueArgument) {
                    parametersWithDefaultValuesCount++
                }
                else {
                    val keys = resolvedArgumentToKeys(resolvedValueArgument)
                    for (argumentKey in keys) {
                        // TODO fix 'original' for value parameters of Java generic descriptors.
                        // Should be able to use just 'valueParameterDescriptor' below.
                        // Doesn't work for Java generic descriptors. See also KT-10939.
                        argumentsToParameters[argumentKey] = unsubstitutedValueParameters[valueParameterDescriptor.index]
                    }
                }
            }

            return CandidateCallWithArgumentMapping(call, argumentsToParameters, parametersWithDefaultValuesCount)
        }
    }
}

