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

package org.jetbrains.kotlin.idea.decompiler

import com.google.protobuf.MessageLite
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.ClassIdWithTarget
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.AnnotatedCallableKind
import org.jetbrains.kotlin.serialization.deserialization.AnnotationAndConstantLoader
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.serialization.js.JsProtoBuf
import org.jetbrains.kotlin.types.JetType

public class AnnotationLoaderForKotlinJavaScriptStubBuilder() : AnnotationAndConstantLoader<ClassId, Unit, ClassIdWithTarget> {

    override fun loadClassAnnotations(
            classProto: ProtoBuf.Class, nameResolver: NameResolver
    ): List<ClassId> =
         classProto.getExtension(JsProtoBuf.classAnnotation).orEmpty().map { nameResolver.getClassId(it.id) }

    override fun loadCallableAnnotations(
            container: ProtoContainer,
            proto: MessageLite,
            kind: AnnotatedCallableKind
    ): List<ClassIdWithTarget> {
        proto as ProtoBuf.Callable // TODO

        return proto.getExtension(JsProtoBuf.callableAnnotation).orEmpty().map {
            ClassIdWithTarget(container.nameResolver.getClassId(it.id), null)
        }
    }

    override fun loadValueParameterAnnotations(
            container: ProtoContainer,
            message: MessageLite,
            kind: AnnotatedCallableKind,
            parameterIndex: Int,
            proto: ProtoBuf.ValueParameter
    ): List<ClassId> =
            proto.getExtension(JsProtoBuf.parameterAnnotation).orEmpty().map {
                container.nameResolver.getClassId(it.id)
            }

    override fun loadExtensionReceiverParameterAnnotations(
            container: ProtoContainer,
            message: MessageLite,
            kind: AnnotatedCallableKind
    ): List<ClassId> = emptyList()

    override fun loadTypeAnnotations(
            proto: ProtoBuf.Type,
            nameResolver: NameResolver
    ): List<ClassId> =
            proto.getExtension(JsProtoBuf.typeAnnotation).orEmpty().map { nameResolver.getClassId(it.id) }

    override fun loadPropertyConstant(
            container: ProtoContainer,
            proto: ProtoBuf.Callable,
            expectedType: JetType
    ) {}
}
