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

package org.jetbrains.kotlin.idea.decompiler.stubBuilder

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.load.kotlin.AbstractBinaryClassAnnotationAndConstantLoader
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.AnnotationAndConstantLoader
import org.jetbrains.kotlin.serialization.deserialization.ClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.ErrorReporter
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import org.jetbrains.kotlin.storage.LockBasedStorageManager

data class ClassIdWithTarget(val classId: ClassId, val target: AnnotationUseSiteTarget?)

class ClsStubBuilderComponents(
        val classDataFinder: ClassDataFinder,
        val annotationLoader: AnnotationAndConstantLoader<ClassId, Unit, ClassIdWithTarget>
) {
    fun createContext(
            nameResolver: NameResolver,
            packageFqName: FqName
    ): ClsStubBuilderContext {
        return ClsStubBuilderContext(this, nameResolver, packageFqName, EmptyTypeParameters)
    }
}

interface TypeParameters {
    fun get(id: Int): Name

    fun child(nameResolver: NameResolver, innerTypeParameters: List<ProtoBuf.TypeParameter>)
            = TypeParametersImpl(nameResolver, innerTypeParameters, parent = this)
}

object EmptyTypeParameters : TypeParameters {
    override fun get(id: Int): Name = throw IllegalStateException("Unknown type parameter with id = $id")
}

class TypeParametersImpl(
        nameResolver: NameResolver,
        typeParameterProtos: Collection<ProtoBuf.TypeParameter>,
        private val parent: TypeParameters
) : TypeParameters {
    private val typeParametersById = typeParameterProtos.map { Pair(it.getId(), nameResolver.getName(it.getName())) }.toMap()

    override fun get(id: Int): Name = typeParametersById[id] ?: parent[id]
}

class ClsStubBuilderContext(
        val components: ClsStubBuilderComponents,
        val nameResolver: NameResolver,
        val containerFqName: FqName,
        val typeParameters: TypeParameters
)

private fun ClsStubBuilderContext.child(typeParameterList: List<ProtoBuf.TypeParameter>, name: Name? = null): ClsStubBuilderContext {
    return ClsStubBuilderContext(
            this.components,
            this.nameResolver,
            if (name != null) this.containerFqName.child(name) else this.containerFqName,
            this.typeParameters.child(nameResolver, typeParameterList)
    )
}

internal fun ClsStubBuilderContext.child(nameResolver: NameResolver): ClsStubBuilderContext {
    return ClsStubBuilderContext(
            this.components,
            nameResolver,
            this.containerFqName,
            this.typeParameters
    )
}

class AnnotationLoaderForStubBuilder(
        kotlinClassFinder: KotlinClassFinder,
        errorReporter: ErrorReporter
) : AbstractBinaryClassAnnotationAndConstantLoader<ClassId, Unit, ClassIdWithTarget>(
        LockBasedStorageManager.NO_LOCKS, kotlinClassFinder, errorReporter) {

    override fun loadClassAnnotations(
            classProto: ProtoBuf.Class,
            nameResolver: NameResolver
    ): List<ClassId> {
        val binaryAnnotationDescriptors = super.loadClassAnnotations(classProto, nameResolver)
        val serializedAnnotations = classProto.getExtension(JvmProtoBuf.classAnnotation).orEmpty()
        val serializedAnnotationDescriptors = serializedAnnotations.map { nameResolver.getClassId(it.id) }
        return binaryAnnotationDescriptors + serializedAnnotationDescriptors
    }

    override fun loadTypeAnnotation(proto: ProtoBuf.Annotation, nameResolver: NameResolver): ClassId =
            nameResolver.getClassId(proto.getId())

    override fun loadConstant(desc: String, initializer: Any) = null

    override fun loadAnnotation(
            annotationClassId: ClassId, source: SourceElement, result: MutableList<ClassId>
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        result.add(annotationClassId)
        return null
    }

    override fun loadPropertyAnnotations(propertyAnnotations: List<ClassId>, fieldAnnotations: List<ClassId>): List<ClassIdWithTarget> {
        return propertyAnnotations.map { ClassIdWithTarget(it, null) } +
               fieldAnnotations.map { ClassIdWithTarget(it, AnnotationUseSiteTarget.FIELD) }
    }

    override fun transformAnnotations(annotations: List<ClassId>) = annotations.map { ClassIdWithTarget(it, null) }
}
