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

package org.jetbrains.kotlin.serialization.js

import com.google.protobuf.MessageLite
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.types.KotlinType

class KotlinJavascriptAnnotationAndConstantLoader(
        module: ModuleDescriptor
) : AnnotationAndConstantLoader<AnnotationDescriptor, ConstantValue<*>, AnnotationWithTarget> {
    private val deserializer = AnnotationDeserializer(module)

    override fun loadClassAnnotations(
            classProto: ProtoBuf.Class,
            nameResolver: NameResolver
    ): List<AnnotationDescriptor> {
        val annotations = classProto.getExtension(JsProtoBuf.classAnnotation).orEmpty()
        return annotations.map { proto -> deserializer.deserializeAnnotation(proto, nameResolver) }
    }

    override fun loadCallableAnnotations(
            container: ProtoContainer,
            proto: MessageLite,
            kind: AnnotatedCallableKind
    ): List<AnnotationWithTarget> {
        val annotations = when (proto) {
            is ProtoBuf.Constructor -> proto.getExtension(JsProtoBuf.constructorAnnotation)
            is ProtoBuf.Function -> proto.getExtension(JsProtoBuf.functionAnnotation)
            is ProtoBuf.Property -> proto.getExtension(JsProtoBuf.propertyAnnotation)
            else -> error("Unknown message: $proto")
        }.orEmpty()
        return annotations.map { proto -> AnnotationWithTarget(deserializer.deserializeAnnotation(proto, container.nameResolver), null) }
    }

    override fun loadValueParameterAnnotations(
            container: ProtoContainer,
            message: MessageLite,
            kind: AnnotatedCallableKind,
            parameterIndex: Int,
            proto: ProtoBuf.ValueParameter
    ): List<AnnotationDescriptor> {
        val annotations = proto.getExtension(JsProtoBuf.parameterAnnotation).orEmpty()
        return annotations.map { proto -> deserializer.deserializeAnnotation(proto, container.nameResolver) }
    }

    override fun loadExtensionReceiverParameterAnnotations(
            container: ProtoContainer,
            message: MessageLite,
            kind: AnnotatedCallableKind
    ): List<AnnotationDescriptor> = emptyList()

    override fun loadTypeAnnotations(proto: ProtoBuf.Type, nameResolver: NameResolver): List<AnnotationDescriptor> {
        val annotations = proto.getExtension(JsProtoBuf.typeAnnotation).orEmpty()
        return annotations.map { proto -> deserializer.deserializeAnnotation(proto, nameResolver) }
    }

    override fun loadTypeParameterAnnotations(proto: ProtoBuf.TypeParameter, nameResolver: NameResolver): List<AnnotationDescriptor> {
        val annotations = proto.getExtension(JsProtoBuf.typeParameterAnnotation).orEmpty()
        return annotations.map { proto -> deserializer.deserializeAnnotation(proto, nameResolver) }
    }

    override fun loadPropertyConstant(
            container: ProtoContainer,
            proto: ProtoBuf.Property,
            expectedType: KotlinType
    ): ConstantValue<*>? {
        if (!proto.hasExtension(JsProtoBuf.compileTimeValue)) return null
        val value = proto.getExtension(JsProtoBuf.compileTimeValue)
        return deserializer.resolveValue(expectedType, value, container.nameResolver)
    }
}
