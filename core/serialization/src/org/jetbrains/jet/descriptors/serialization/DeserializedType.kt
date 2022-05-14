/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.descriptors.serialization

import org.jetbrains.jet.descriptors.serialization.context.DeserializationContext
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.descriptors.annotations.Annotations
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.types.*

class DeserializedType(
        c: DeserializationContext,
        private val typeProto: ProtoBuf.Type
) : AbstractJetType(), LazyType {
    private val typeDeserializer = c.typeDeserializer

    private val constructor = c.storageManager.createLazyValue {
        typeDeserializer.typeConstructor(typeProto)
    }

    private val arguments = typeDeserializer.typeArguments(typeProto.getArgumentList())

    private val memberScope = c.storageManager.createLazyValue {
        computeMemberScope()
    }

    override fun getConstructor(): TypeConstructor = constructor()

    override fun getArguments(): List<TypeProjection> = arguments

    override fun isNullable(): Boolean = typeProto.getNullable()

    private fun computeMemberScope(): JetScope =
            if (isError()) {
                ErrorUtils.createErrorScope(getConstructor().toString())
            }
            else {
                getTypeMemberScope(getConstructor(), getArguments())
            }

    private fun getTypeMemberScope(constructor: TypeConstructor, typeArguments: List<TypeProjection>): JetScope {
        val descriptor = constructor.getDeclarationDescriptor()
        return when (descriptor) {
            is TypeParameterDescriptor -> descriptor.getDefaultType().getMemberScope()
            is ClassDescriptor -> descriptor.getMemberScope(typeArguments)
            else -> throw IllegalStateException("Unsupported classifier: $descriptor")
        }
    }

    override fun getMemberScope(): JetScope = memberScope()

    override fun isError(): Boolean {
        val descriptor = getConstructor().getDeclarationDescriptor()
        return descriptor != null && ErrorUtils.isError(descriptor)
    }

    override fun getAnnotations(): Annotations = Annotations.EMPTY
}
