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

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import java.lang.reflect.Field
import kotlin.reflect.jvm.internal.JvmPropertySignature.JavaField
import kotlin.reflect.jvm.internal.JvmPropertySignature.KotlinProperty

internal abstract class DescriptorBasedProperty<out R> protected constructor(
        val container: KDeclarationContainerImpl,
        name: String,
        val signature: String,
        descriptorInitialValue: PropertyDescriptor?
) : KCallableImpl<R> {
    constructor(container: KDeclarationContainerImpl, name: String, signature: String) : this(
            container, name, signature, null
    )

    constructor(container: KDeclarationContainerImpl, descriptor: PropertyDescriptor) : this(
            container,
            descriptor.name.asString(),
            RuntimeTypeMapper.mapPropertySignature(descriptor).asString(),
            descriptor
    )

    private val descriptor_ = ReflectProperties.lazySoft<PropertyDescriptor>(descriptorInitialValue) {
        container.findPropertyDescriptor(name, signature)
    }

    override val descriptor: PropertyDescriptor get() = descriptor_()

    private val javaField_ = ReflectProperties.lazySoft {
        val jvmSignature = RuntimeTypeMapper.mapPropertySignature(descriptor)
        when (jvmSignature) {
            is KotlinProperty -> {
                JvmProtoBufUtil.getJvmFieldSignature(jvmSignature.proto, jvmSignature.nameResolver, jvmSignature.typeTable)?.let {
                    container.findFieldBySignature(jvmSignature.proto, jvmSignature.nameResolver, it.name)
                }
            }
            is JavaField -> jvmSignature.field
        }
    }

    // Used in subclasses as an implementation of an irrelevant property from KPropertyImpl
    val javaField: Field? get() = javaField_()

    override fun equals(other: Any?): Boolean {
        val that = other.asKPropertyImpl() ?: return false
        return container == that.container && name == that.name && signature == that.signature
    }

    override fun hashCode(): Int =
            (container.hashCode() * 31 + name.hashCode()) * 31 + signature.hashCode()

    override fun toString(): String =
            ReflectionObjectRenderer.renderProperty(descriptor)
}
