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

package org.jetbrains.kotlin.descriptors.impl

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeUtils

abstract class AbstractTypeAliasDescriptor(
        containingDeclaration: DeclarationDescriptor,
        annotations: Annotations,
        name: Name,
        sourceElement: SourceElement,
        private val visibilityImpl: Visibility
) : DeclarationDescriptorNonRootImpl(containingDeclaration, annotations, name, sourceElement),
        TypeAliasDescriptor {

    // TODO kotlinize some interfaces
    private lateinit var declaredTypeParametersImpl: List<TypeParameterDescriptor>

    fun initialize(declaredTypeParameters: List<TypeParameterDescriptor>) {
        this.declaredTypeParametersImpl = declaredTypeParameters
    }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R =
            visitor.visitTypeAliasDescriptor(this, data)

    override fun isInner(): Boolean =
            // NB: it's ok to use underlyingType here, since referenced inner type aliases also capture type parameters.
            // Using expandedType looks "proper", but in fact will cause a recursion in expandedType resolution,
            // which will silently produce wrong result.
            TypeUtils.contains(underlyingType) {
                !it.isError && run {
                    val constructorDescriptor = it.constructor.declarationDescriptor
                    constructorDescriptor is TypeParameterDescriptor &&
                    constructorDescriptor.containingDeclaration != this@AbstractTypeAliasDescriptor
                }
            }

    override fun getDeclaredTypeParameters(): List<TypeParameterDescriptor> =
            declaredTypeParametersImpl

    override fun getModality() = Modality.FINAL

    override fun getVisibility() = visibilityImpl

    override fun isHeader(): Boolean = false

    override fun isImpl(): Boolean = false

    override fun isExternal() = false

    override fun getTypeConstructor(): TypeConstructor =
            typeConstructor

    override fun toString(): String = "typealias ${name.asString()}"

    protected abstract fun getTypeConstructorTypeParameters(): List<TypeParameterDescriptor>

    protected fun computeDefaultType(): SimpleType =
            TypeUtils.makeUnsubstitutedType(this, classDescriptor?.unsubstitutedMemberScope ?: MemberScope.Empty)

    private val typeConstructor = object : TypeConstructor {
        override fun getDeclarationDescriptor(): TypeAliasDescriptor =
                this@AbstractTypeAliasDescriptor

        override fun getParameters(): List<TypeParameterDescriptor> =
                getTypeConstructorTypeParameters()

        override fun getSupertypes(): Collection<KotlinType> =
                declarationDescriptor.underlyingType.constructor.supertypes

        override fun isFinal(): Boolean =
                declarationDescriptor.underlyingType.constructor.isFinal

        override fun isDenotable(): Boolean =
                true

        override fun getBuiltIns(): KotlinBuiltIns =
                declarationDescriptor.builtIns

        override fun toString(): String = "[typealias ${declarationDescriptor.name.asString()}]"
    }
}
