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

package org.jetbrains.kotlin.serialization.deserialization.descriptors

import com.google.protobuf.MessageLite
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.TypeTable

interface DeserializedCallableMemberDescriptor : CallableMemberDescriptor {
    val proto: MessageLite

    val nameResolver: NameResolver

    val typeTable: TypeTable

    // Information about the origin of this top-level callable or null, if it's not top-level or there's no such information.
    // On JVM, this is JvmPackagePartSource which contains the internal name of the package part class
    val packagePartSource: PackagePartSource?
}

interface PackagePartSource

class DeserializedSimpleFunctionDescriptor(
        containingDeclaration: DeclarationDescriptor,
        original: SimpleFunctionDescriptor?,
        annotations: Annotations,
        name: Name,
        kind: CallableMemberDescriptor.Kind,
        override val proto: ProtoBuf.Function,
        override val nameResolver: NameResolver,
        override val typeTable: TypeTable,
        override val packagePartSource: PackagePartSource?
) : DeserializedCallableMemberDescriptor,
        SimpleFunctionDescriptorImpl(containingDeclaration, original, annotations, name, kind, SourceElement.NO_SOURCE) {

    override fun createSubstitutedCopy(
            newOwner: DeclarationDescriptor,
            original: FunctionDescriptor?,
            kind: CallableMemberDescriptor.Kind,
            newName: Name?,
            preserveSource: Boolean
    ): FunctionDescriptorImpl {
        return DeserializedSimpleFunctionDescriptor(
                newOwner, original as SimpleFunctionDescriptor?, annotations, newName ?: name, kind,
                proto, nameResolver, typeTable, packagePartSource
        )
    }
}

class DeserializedPropertyDescriptor(
        containingDeclaration: DeclarationDescriptor,
        original: PropertyDescriptor?,
        annotations: Annotations,
        modality: Modality,
        visibility: Visibility,
        isVar: Boolean,
        name: Name,
        kind: CallableMemberDescriptor.Kind,
        isLateInit: Boolean,
        isConst: Boolean,
        override val proto: ProtoBuf.Property,
        override val nameResolver: NameResolver,
        override val typeTable: TypeTable,
        override val packagePartSource: PackagePartSource?
) : DeserializedCallableMemberDescriptor,
        PropertyDescriptorImpl(containingDeclaration, original, annotations,
                               modality, visibility, isVar, name, kind, SourceElement.NO_SOURCE, isLateInit, isConst) {

    override fun createSubstitutedCopy(
            newOwner: DeclarationDescriptor,
            newModality: Modality,
            newVisibility: Visibility,
            original: PropertyDescriptor?,
            kind: CallableMemberDescriptor.Kind
    ): PropertyDescriptorImpl {
        return DeserializedPropertyDescriptor(
                newOwner, original, annotations, newModality, newVisibility, isVar, name, kind, isLateInit, isConst,
                proto, nameResolver, typeTable, packagePartSource
        )
    }
}

class DeserializedConstructorDescriptor(
        containingDeclaration: ClassDescriptor,
        original: ConstructorDescriptor?,
        annotations: Annotations,
        isPrimary: Boolean,
        kind: CallableMemberDescriptor.Kind,
        override val proto: ProtoBuf.Constructor,
        override val nameResolver: NameResolver,
        override val typeTable: TypeTable,
        override val packagePartSource: PackagePartSource?
) : DeserializedCallableMemberDescriptor,
        ConstructorDescriptorImpl(containingDeclaration, original, annotations, isPrimary, kind, SourceElement.NO_SOURCE) {

    override fun createSubstitutedCopy(
            newOwner: DeclarationDescriptor,
            original: FunctionDescriptor?,
            kind: CallableMemberDescriptor.Kind,
            newName: Name?,
            preserveSource: Boolean
    ): DeserializedConstructorDescriptor {
        return DeserializedConstructorDescriptor(
                newOwner as ClassDescriptor, original as ConstructorDescriptor?, annotations, isPrimary, kind,
                proto, nameResolver, typeTable, packagePartSource
        )
    }

    override fun isExternal(): Boolean = false

    override fun isInline(): Boolean = false

    override fun isTailrec(): Boolean = false
}
