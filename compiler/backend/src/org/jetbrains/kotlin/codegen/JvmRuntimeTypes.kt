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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.coroutines.controllerTypeIfCoroutine
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.MutablePackageFragmentDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.util.createFunctionType
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils

class JvmRuntimeTypes(module: ModuleDescriptor) {
    private val kotlinJvmInternalPackage = MutablePackageFragmentDescriptor(module, FqName("kotlin.jvm.internal"))

    private fun klass(name: String) = lazy { createClass(kotlinJvmInternalPackage, name) }

    private val lambda: ClassDescriptor by klass("Lambda")
    private val functionReference: ClassDescriptor by klass("FunctionReference")
    private val localVariableReference: ClassDescriptor by klass("LocalVariableReference")
    private val mutableLocalVariableReference: ClassDescriptor by klass("MutableLocalVariableReference")

    private val propertyReferences: List<ClassDescriptor> by lazy {
        (0..2).map { i -> createClass(kotlinJvmInternalPackage, "PropertyReference$i") }
    }

    private val mutablePropertyReferences: List<ClassDescriptor> by lazy {
        (0..2).map { i -> createClass(kotlinJvmInternalPackage, "MutablePropertyReference$i") }
    }

    private val defaultContinuationSupertype: KotlinType by lazy { createNullableAnyContinuation(module) }

    /**
     * @return `Continuation<Any?>` type
     */
    private fun createNullableAnyContinuation(module: ModuleDescriptor): KotlinType {
        // TODO: create a synthesized class descriptor here instead of looking at the classpath, or report a proper diagnostic
        // This code will throw when compiling with "-no-stdlib" because no built-ins would be found in the classpath either
        val classDescriptor = module.resolveTopLevelClass(
                DescriptorUtils.CONTINUATION_INTERFACE_FQ_NAME, NoLookupLocation.FROM_BACKEND
        ) ?: throw AssertionError("${DescriptorUtils.CONTINUATION_INTERFACE_FQ_NAME} was not found in built-ins")

        return TypeConstructorSubstitution.createByParametersMap(
                mapOf(classDescriptor.declaredTypeParameters.single() to TypeProjectionImpl(module.builtIns.nullableAnyType))
        ).buildSubstitutor().substitute(classDescriptor.defaultType, Variance.INVARIANT)!!
    }

    private fun createClass(packageFragment: PackageFragmentDescriptor, name: String): ClassDescriptor =
            MutableClassDescriptor(packageFragment, ClassKind.CLASS, false, Name.identifier(name), SourceElement.NO_SOURCE).apply {
                modality = Modality.FINAL
                visibility = Visibilities.PUBLIC
                setTypeParameterDescriptors(emptyList())
                createTypeConstructor()
            }

    fun getSupertypesForClosure(descriptor: FunctionDescriptor): Collection<KotlinType> {
        val functionType = createFunctionType(
                descriptor.builtIns,
                Annotations.EMPTY,
                descriptor.extensionReceiverParameter?.type,
                ExpressionTypingUtils.getValueParametersTypes(descriptor.valueParameters),
                null,
                descriptor.returnType!!
        )

        val coroutineControllerType = descriptor.controllerTypeIfCoroutine
        if (coroutineControllerType != null) {
            return listOf(lambda.defaultType, functionType, /*coroutineType,*/ defaultContinuationSupertype)
        }

        return listOf(lambda.defaultType, functionType)
    }

    fun getSupertypesForFunctionReference(descriptor: FunctionDescriptor, isBound: Boolean): Collection<KotlinType> {
        val functionType = createFunctionType(
                descriptor.builtIns,
                Annotations.EMPTY,
                if (isBound) null else descriptor.extensionReceiverParameter?.type ?: descriptor.dispatchReceiverParameter?.type,
                ExpressionTypingUtils.getValueParametersTypes(descriptor.valueParameters),
                null,
                descriptor.returnType!!
        )

        return listOf(functionReference.defaultType, functionType)
    }

    fun getSupertypeForPropertyReference(descriptor: VariableDescriptorWithAccessors, isMutable: Boolean, isBound: Boolean): KotlinType {
        if (descriptor is LocalVariableDescriptor) {
            return (if (isMutable) mutableLocalVariableReference else localVariableReference).defaultType
        }

        val arity =
                (if (descriptor.extensionReceiverParameter != null) 1 else 0) +
                (if (descriptor.dispatchReceiverParameter != null) 1 else 0) -
                if (isBound) 1 else 0

        return (if (isMutable) mutablePropertyReferences else propertyReferences)[arity].defaultType
    }
}
