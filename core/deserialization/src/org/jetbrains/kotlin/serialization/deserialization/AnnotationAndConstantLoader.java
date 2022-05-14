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

package org.jetbrains.kotlin.serialization.deserialization;

import com.google.protobuf.MessageLite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.serialization.ProtoBuf;
import org.jetbrains.kotlin.types.JetType;

import java.util.List;

// The MessageLite instance everywhere should be Constructor, Function or Property
// TODO: simplify this interface
public interface AnnotationAndConstantLoader<A, C, T> {
    @NotNull
    List<A> loadClassAnnotations(
            @NotNull ProtoBuf.Class classProto,
            @NotNull NameResolver nameResolver
    );

    @NotNull
    List<T> loadCallableAnnotations(
            @NotNull ProtoContainer container,
            @NotNull MessageLite message,
            @NotNull AnnotatedCallableKind kind
    );

    @NotNull
    List<A> loadValueParameterAnnotations(
            @NotNull ProtoContainer container,
            @NotNull MessageLite message,
            @NotNull AnnotatedCallableKind kind,
            int parameterIndex,
            @NotNull ProtoBuf.ValueParameter proto
    );

    @NotNull
    List<A> loadExtensionReceiverParameterAnnotations(
            @NotNull ProtoContainer container,
            @NotNull MessageLite message,
            @NotNull AnnotatedCallableKind kind
    );

    @NotNull
    List<A> loadTypeAnnotations(
            @NotNull ProtoBuf.Type type,
            @NotNull NameResolver nameResolver
    );

    @Nullable
    C loadPropertyConstant(
            @NotNull ProtoContainer container,
            @NotNull ProtoBuf.Callable proto,
            @NotNull JetType expectedType
    );
}
