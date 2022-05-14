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

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations

public abstract class TypeSubstitution {
    companion object {
        @JvmField
        public val EMPTY: TypeSubstitution = object : TypeSubstitution() {
            override fun get(key: KotlinType) = null
            override fun isEmpty() = true
            override fun toString() = "Empty TypeSubstitution"
        }
    }

    abstract operator fun get(key: KotlinType): TypeProjection?

    open fun isEmpty(): Boolean = false

    open fun approximateCapturedTypes(): Boolean = false
    open fun approximateContravariantCapturedTypes(): Boolean = false

    open fun filterAnnotations(annotations: Annotations) = annotations

    fun buildSubstitutor(): TypeSubstitutor = TypeSubstitutor.create(this)
}

public abstract class TypeConstructorSubstitution : TypeSubstitution() {
    override fun get(key: KotlinType) = get(key.constructor)

    public abstract fun get(key: TypeConstructor): TypeProjection?

    companion object {
        @JvmStatic
        public fun createByConstructorsMap(map: Map<TypeConstructor, TypeProjection>): TypeConstructorSubstitution =
            object : TypeConstructorSubstitution() {
                override fun get(key: TypeConstructor) = map[key]
                override fun isEmpty() = map.isEmpty()
            }

        @JvmStatic
        public fun createByParametersMap(map: Map<TypeParameterDescriptor, TypeProjection>): TypeConstructorSubstitution =
            object : TypeConstructorSubstitution() {
                override fun get(key: TypeConstructor) = map[key.declarationDescriptor]
                override fun isEmpty() = map.isEmpty()
            }

        @JvmStatic
        public fun create(kotlinType: KotlinType) = create(kotlinType.constructor, kotlinType.arguments)

        @JvmStatic
        public fun create(typeConstructor: TypeConstructor, arguments: List<TypeProjection>): TypeSubstitution {
            val parameters = typeConstructor.parameters

            if (parameters.lastOrNull()?.isCapturedFromOuterDeclaration ?: false) {
                return createByConstructorsMap(typeConstructor.parameters.map { it.typeConstructor }.zip(arguments).toMap())
            }

            return IndexedParametersSubstitution(parameters, arguments)
        }
    }
}

public class IndexedParametersSubstitution(
    val parameters: Array<TypeParameterDescriptor>,
    val arguments: Array<TypeProjection>,
    private val approximateCapturedTypes: Boolean = false
) : TypeSubstitution() {
    init {
        assert(parameters.size <= arguments.size) {
            "Number of arguments should not be less then number of parameters, but: parameters=${parameters.size}, args=${arguments.size}"
        }
    }

    constructor(
            parameters: List<TypeParameterDescriptor>, argumentsList: List<TypeProjection>
    ) : this(parameters.toTypedArray(), argumentsList.toTypedArray())

    override fun isEmpty(): Boolean = arguments.isEmpty()

    override fun approximateContravariantCapturedTypes() = approximateCapturedTypes

    override fun get(key: KotlinType): TypeProjection? {
        val parameter = key.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return null
        val index = parameter.index

        if (index < parameters.size && parameters[index].typeConstructor == parameter.typeConstructor) {
            return arguments[index]
        }

        return null
    }
}

public fun KotlinType.computeNewSubstitution(
        typeConstructor: TypeConstructor,
        newArguments: List<TypeProjection>
): TypeSubstitution {
    val newSubstitution = TypeConstructorSubstitution.create(typeConstructor, newArguments)

    // If previous substitution was trivial just replace it with indexed one
    val substitutionToComposeWith = getCapability<CustomSubstitutionCapability>()?.substitutionToComposeWith ?: return newSubstitution
    val composedSubstitution = CompositeTypeSubstitution(newSubstitution, substitutionToComposeWith)

    return composedSubstitution
}

public fun KotlinType.replace(
        newArguments: List<TypeProjection>,
        annotations: Annotations = this@replace.annotations
): KotlinType {
    if (newArguments.isEmpty() && annotations === this.annotations) return this

    if (newArguments.isEmpty()) {
        return KotlinTypeImpl.create(
                annotations,
                constructor,
                isMarkedNullable,
                arguments,
                substitution,
                memberScope,
                capabilities
        )
    }

    val newSubstitution = computeNewSubstitution(constructor, newArguments)

    val declarationDescriptor = constructor.declarationDescriptor
    val newScope =
            if (declarationDescriptor is ClassDescriptor)
                declarationDescriptor.getMemberScope(newSubstitution)
            else ErrorUtils.createErrorScope("Unexpected declaration descriptor for type constructor: $constructor")

    return KotlinTypeImpl.create(
            annotations,
            constructor,
            isMarkedNullable,
            newArguments,
            newSubstitution,
            newScope,
            capabilities
    )
}

private class CompositeTypeSubstitution(
    private val first: TypeSubstitution,
    private val second: TypeSubstitution
) : TypeSubstitution() {

    override fun get(key: KotlinType): TypeProjection? {
        val firstResult = first[key] ?: return second[key]
        return second.buildSubstitutor().substitute(firstResult)
    }

    override fun isEmpty() = first.isEmpty() && second.isEmpty()

    override fun approximateCapturedTypes() = first.approximateCapturedTypes() || second.approximateCapturedTypes()
    override fun approximateContravariantCapturedTypes() = first.approximateContravariantCapturedTypes() || second.approximateContravariantCapturedTypes()

    override fun filterAnnotations(annotations: Annotations): Annotations = second.filterAnnotations(first.filterAnnotations(annotations))
}

public open class DelegatedTypeSubstitution(val substitution: TypeSubstitution): TypeSubstitution() {
    override fun get(key: KotlinType) = substitution.get(key)

    override fun isEmpty() = substitution.isEmpty()

    override fun approximateCapturedTypes() = substitution.approximateCapturedTypes()
    override fun approximateContravariantCapturedTypes() = substitution.approximateContravariantCapturedTypes()

    override fun filterAnnotations(annotations: Annotations) = substitution.filterAnnotations(annotations)
}
