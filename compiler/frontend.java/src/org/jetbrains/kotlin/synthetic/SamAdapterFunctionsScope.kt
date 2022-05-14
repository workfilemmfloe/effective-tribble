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

package org.jetbrains.kotlin.synthetic

import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.load.java.sam.SingleAbstractMethodUtils
import org.jetbrains.kotlin.load.java.typeEnhacement.enhanceSignature
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.*
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedHashSet
import kotlin.properties.Delegates

interface SamAdapterExtensionFunctionDescriptor : FunctionDescriptor {
    val sourceFunction: FunctionDescriptor
}

class SamAdapterFunctionsScope(storageManager: StorageManager) : JetScope by JetScope.Empty {
    private val extensionForFunction = storageManager.createMemoizedFunctionWithNullableValues<FunctionDescriptor, FunctionDescriptor> { function ->
        extensionForFunctionNotCached(function)
    }

    private fun extensionForFunctionNotCached(function: FunctionDescriptor): FunctionDescriptor? {
        if (!function.visibility.isVisibleOutside()) return null
        if (!function.hasJavaOriginInHierarchy()) return null //TODO: should we go into base at all?
        if (!SingleAbstractMethodUtils.isSamAdapterNecessary(function)) return null
        if (function.returnType == null) return null
        //TODO: it's a temporary hack while original returns a function with platform types
        val enhancedFunction = function.enhanceSignature()
        return MyFunctionDescriptor.create(enhancedFunction)
    }

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<JetType>, name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
        var result: SmartList<FunctionDescriptor>? = null
        for (type in receiverTypes) {
            for (function in type.memberScope.getFunctions(name, location)) {
                val extension = extensionForFunction(function.original)
                if (extension != null) {
                    if (result == null) {
                        result = SmartList()
                    }
                    result.add(extension)
                }
            }
        }
        return when {
            result == null -> emptyList()
            result.size() > 1 -> result.toSet()
            else -> result
        }
    }

    override fun getSyntheticExtensionFunctions(receiverTypes: Collection<JetType>): Collection<FunctionDescriptor> {
        return receiverTypes.flatMapTo(LinkedHashSet<FunctionDescriptor>()) { type ->
            type.memberScope.getDescriptors(DescriptorKindFilter.FUNCTIONS)
                    .filterIsInstance<FunctionDescriptor>()
                    .map { extensionForFunction(it.original) }
                    .filterNotNull()
        }
    }

    private class MyFunctionDescriptor(
            containingDeclaration: DeclarationDescriptor,
            original: SimpleFunctionDescriptor?,
            annotations: Annotations,
            name: Name,
            kind: CallableMemberDescriptor.Kind,
            source: SourceElement
    ) : SamAdapterExtensionFunctionDescriptor, SimpleFunctionDescriptorImpl(containingDeclaration, original, annotations, name, kind, source) {

        override var sourceFunction: FunctionDescriptor by Delegates.notNull()
            private set

        private var toSourceFunctionTypeParameters: Map<TypeParameterDescriptor, TypeParameterDescriptor>? = null

        companion object {
            fun create(sourceFunction: FunctionDescriptor): MyFunctionDescriptor {
                val descriptor = MyFunctionDescriptor(DescriptorUtils.getContainingModule(sourceFunction),
                                                      null,
                                                      Annotations.EMPTY, //TODO
                                                      sourceFunction.name,
                                                      CallableMemberDescriptor.Kind.SYNTHESIZED,
                                                      sourceFunction.source)
                descriptor.sourceFunction = sourceFunction

                val sourceTypeParams = (sourceFunction.typeParameters).toArrayList()
                val ownerClass = sourceFunction.containingDeclaration as ClassDescriptor
                //TODO: should we go up parents for getters/setters too?
                //TODO: non-inner classes
                for (parent in ownerClass.parentsWithSelf) {
                    if (parent !is ClassDescriptor) break
                    sourceTypeParams += parent.typeConstructor.parameters
                }
                //TODO: duplicated parameter names

                val typeParameters = ArrayList<TypeParameterDescriptor>(sourceTypeParams.size())
                val typeSubstitutor = DescriptorSubstitutor.substituteTypeParameters(sourceTypeParams, TypeSubstitution.EMPTY, descriptor, typeParameters)

                descriptor.toSourceFunctionTypeParameters = typeParameters.zip(sourceTypeParams).toMap()

                val returnType = typeSubstitutor.safeSubstitute(sourceFunction.returnType!!, Variance.INVARIANT)
                val receiverType = typeSubstitutor.safeSubstitute(ownerClass.defaultType, Variance.INVARIANT)
                val valueParameters = SingleAbstractMethodUtils.createValueParametersForSamAdapter(sourceFunction, descriptor, typeSubstitutor)

                val visibility = syntheticExtensionVisibility(sourceFunction)

                descriptor.initialize(receiverType, null, typeParameters, valueParameters, returnType, Modality.FINAL, visibility, false)

                return descriptor
            }
        }

        override fun hasStableParameterNames() = sourceFunction.hasStableParameterNames()
        override fun hasSynthesizedParameterNames() = sourceFunction.hasSynthesizedParameterNames()

        override fun createSubstitutedCopy(newOwner: DeclarationDescriptor, original: FunctionDescriptor?, kind: CallableMemberDescriptor.Kind): MyFunctionDescriptor {
            return MyFunctionDescriptor(containingDeclaration, original as SimpleFunctionDescriptor?, annotations, name, kind, source).apply {
                sourceFunction = this@MyFunctionDescriptor.sourceFunction
            }
        }

        override fun doSubstitute(
                originalSubstitutor: TypeSubstitutor,
                newOwner: DeclarationDescriptor,
                newModality: Modality,
                newVisibility: Visibility,
                newIsOperator: Boolean,
                original: FunctionDescriptor?,
                copyOverrides: Boolean,
                kind: CallableMemberDescriptor.Kind,
                newValueParameterDescriptors: MutableList<ValueParameterDescriptor>,
                newExtensionReceiverParameterType: JetType?,
                newReturnType: JetType
        ): FunctionDescriptor? {
            val descriptor = super<SimpleFunctionDescriptorImpl>.doSubstitute(
                    originalSubstitutor, newOwner, newModality, newVisibility, newIsOperator, original,
                    copyOverrides, kind, newValueParameterDescriptors, newExtensionReceiverParameterType, newReturnType)
                as MyFunctionDescriptor? ?: return null

            if (original == null) {
                throw UnsupportedOperationException("doSubstitute with no original should not be called for synthetic extension")
            }

            original as MyFunctionDescriptor
            assert(original.original == original, "original in doSubstitute should have no other original")

            val substitutionMap = HashMap<TypeConstructor, TypeProjection>()
            for (typeParameter in original.typeParameters) {
                val typeProjection = originalSubstitutor.substitution[typeParameter.defaultType] ?: continue
                val sourceTypeParameter = original.toSourceFunctionTypeParameters!![typeParameter]!!
                substitutionMap[sourceTypeParameter.typeConstructor] = typeProjection

            }

            val sourceFunctionSubstitutor = TypeSubstitutor.create(substitutionMap)
            descriptor.sourceFunction = original.sourceFunction.substitute(sourceFunctionSubstitutor)!!

            return descriptor
        }
    }
}
