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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ConstructorDescriptorImpl
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver

public class DeserializedConstructorDescriptor(
        containingDeclaration: ClassDescriptor,
        original: ConstructorDescriptor?,
        annotations: Annotations,
        isPrimary: Boolean,
        kind: CallableMemberDescriptor.Kind,
        override val proto: ProtoBuf.Callable,
        override val nameResolver: NameResolver
) : ConstructorDescriptorImpl(containingDeclaration, original, annotations, isPrimary, kind, SourceElement.NO_SOURCE),
        DeserializedCallableMemberDescriptor {

    override fun createSubstitutedCopy(
            newOwner: DeclarationDescriptor,
            original: FunctionDescriptor?,
            kind: CallableMemberDescriptor.Kind
    ): DeserializedConstructorDescriptor {
        return DeserializedConstructorDescriptor(
                newOwner as ClassDescriptor, original as ConstructorDescriptor?, getAnnotations(), isPrimary, kind, proto, nameResolver
        )
    }
}
