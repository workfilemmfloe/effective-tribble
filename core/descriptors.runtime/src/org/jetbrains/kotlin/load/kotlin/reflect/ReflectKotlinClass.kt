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

package org.jetbrains.kotlin.load.kotlin.reflect

import org.jetbrains.kotlin.load.java.structure.reflect.classId
import org.jetbrains.kotlin.load.java.structure.reflect.desc
import org.jetbrains.kotlin.load.java.structure.reflect.isEnumClassOrSpecializedEnumEntryClass
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.header.ReadKotlinClassHeaderAnnotationVisitor
import org.jetbrains.kotlin.name.Name
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
private val TYPES_ELIGIBLE_FOR_SIMPLE_VISIT = setOf<Class<*>>(
        // Primitives
        java.lang.Integer::class.java, java.lang.Character::class.java, java.lang.Byte::class.java, java.lang.Long::class.java,
        java.lang.Short::class.java, java.lang.Boolean::class.java, java.lang.Double::class.java, java.lang.Float::class.java,
        // Arrays of primitives
        IntArray::class.java, CharArray::class.java, ByteArray::class.java, LongArray::class.java,
        ShortArray::class.java, BooleanArray::class.java, DoubleArray::class.java, FloatArray::class.java,
        // Others
        Class::class.java, String::class.java
)

public class ReflectKotlinClass private constructor(
        public val klass: Class<*>,
        private val classHeader: KotlinClassHeader
) : KotlinJvmBinaryClass {

    companion object Factory {

        public fun create(klass: Class<*>): ReflectKotlinClass? {
            val headerReader = ReadKotlinClassHeaderAnnotationVisitor()
            ReflectClassStructure.loadClassAnnotations(klass, headerReader)
            return ReflectKotlinClass(klass, headerReader.createHeader() ?: return null)
        }
    }

    override fun getLocation() = klass.getName().replace('.', '/') + ".class"

    override fun getClassId() = klass.classId

    override fun getClassHeader() = classHeader

    override fun loadClassAnnotations(visitor: KotlinJvmBinaryClass.AnnotationVisitor) {
        ReflectClassStructure.loadClassAnnotations(klass, visitor)
    }

    override fun visitMembers(visitor: KotlinJvmBinaryClass.MemberVisitor) {
        ReflectClassStructure.visitMembers(klass, visitor)
    }

    override fun equals(other: Any?) = other is ReflectKotlinClass && klass == other.klass

    override fun hashCode() = klass.hashCode()

    override fun toString() = javaClass.getName() + ": " + klass
}

private object ReflectClassStructure {
    fun loadClassAnnotations(klass: Class<*>, visitor: KotlinJvmBinaryClass.AnnotationVisitor) {
        for (annotation in klass.getDeclaredAnnotations()) {
            processAnnotation(visitor, annotation)
        }
        visitor.visitEnd()
    }

    fun visitMembers(klass: Class<*>, memberVisitor: KotlinJvmBinaryClass.MemberVisitor) {
        loadMethodAnnotations(klass, memberVisitor)
        loadConstructorAnnotations(klass, memberVisitor)
        loadFieldAnnotations(klass, memberVisitor)
    }

    private fun loadMethodAnnotations(klass: Class<*>, memberVisitor: KotlinJvmBinaryClass.MemberVisitor) {
        for (method in klass.getDeclaredMethods()) {
            val visitor = memberVisitor.visitMethod(Name.identifier(method.getName()), SignatureSerializer.methodDesc(method)) ?: continue

            for (annotation in method.getDeclaredAnnotations()) {
                processAnnotation(visitor, annotation)
            }

            for ((parameterIndex, annotations) in method.getParameterAnnotations().withIndex()) {
                for (annotation in annotations) {
                    val annotationType = annotation.annotationType()
                    visitor.visitParameterAnnotation(parameterIndex, annotationType.classId, ReflectAnnotationSource(annotation))?.let {
                        processAnnotationArguments(it, annotation, annotationType)
                    }
                }
            }

            visitor.visitEnd()
        }
    }

    private fun loadConstructorAnnotations(klass: Class<*>, memberVisitor: KotlinJvmBinaryClass.MemberVisitor) {
        for (constructor in klass.getDeclaredConstructors()) {
            val visitor = memberVisitor.visitMethod(Name.special("<init>"), SignatureSerializer.constructorDesc(constructor)) ?: continue

            for (annotation in constructor.getDeclaredAnnotations()) {
                processAnnotation(visitor, annotation)
            }

            val parameterAnnotations = constructor.getParameterAnnotations()
            if (parameterAnnotations.isNotEmpty()) {
                // Constructors of some classes have additional synthetic parameters:
                // - inner classes have one parameter, instance of the outer class
                // - enum classes have two parameters, String name and int ordinal
                // - local/anonymous classes may have many parameters for captured values
                // At the moment this seems like a working heuristic for computing number of synthetic parameters for Kotlin classes,
                // although this is wrong and likely to change, see KT-6886
                val shift = constructor.getParameterTypes().size() - parameterAnnotations.size()

                for ((parameterIndex, annotations) in parameterAnnotations.withIndex()) {
                    for (annotation in annotations) {
                        val annotationType = annotation.annotationType()
                        visitor.visitParameterAnnotation(
                                parameterIndex + shift, annotationType.classId, ReflectAnnotationSource(annotation)
                        )?.let {
                            processAnnotationArguments(it, annotation, annotationType)
                        }
                    }
                }
            }

            visitor.visitEnd()
        }
    }

    private fun loadFieldAnnotations(klass: Class<*>, memberVisitor: KotlinJvmBinaryClass.MemberVisitor) {
        for (field in klass.getDeclaredFields()) {
            val visitor = memberVisitor.visitField(Name.identifier(field.getName()), SignatureSerializer.fieldDesc(field), null) ?: continue

            for (annotation in field.getDeclaredAnnotations()) {
                processAnnotation(visitor, annotation)
            }

            visitor.visitEnd()
        }
    }

    private fun processAnnotation(visitor: KotlinJvmBinaryClass.AnnotationVisitor, annotation: Annotation) {
        val annotationType = annotation.annotationType()
        visitor.visitAnnotation(annotationType.classId, ReflectAnnotationSource(annotation))?.let {
            processAnnotationArguments(it, annotation, annotationType)
        }
    }

    private fun processAnnotationArguments(
            visitor: KotlinJvmBinaryClass.AnnotationArgumentVisitor,
            annotation: Annotation,
            annotationType: Class<*>
    ) {
        for (method in annotationType.getDeclaredMethods()) {
            processAnnotationArgumentValue(visitor, Name.identifier(method.getName()), method(annotation)!!)
        }
        visitor.visitEnd()
    }

    private fun processAnnotationArgumentValue(visitor: KotlinJvmBinaryClass.AnnotationArgumentVisitor, name: Name, value: Any) {
        val clazz = value.javaClass
        when {
            clazz in TYPES_ELIGIBLE_FOR_SIMPLE_VISIT -> {
                visitor.visit(name, value)
            }
            clazz.isEnumClassOrSpecializedEnumEntryClass() -> {
                // isEnum returns false for specialized enum constants (enum entries which are anonymous enum subclasses)
                val classId = (if (clazz.isEnum()) clazz else clazz.getEnclosingClass()).classId
                visitor.visitEnum(name, classId, Name.identifier((value as Enum<*>).name()))
            }
            javaClass<Annotation>().isAssignableFrom(clazz) -> {
                val annotationClass = clazz.getInterfaces().single()
                val v = visitor.visitAnnotation(name, annotationClass.classId) ?: return
                processAnnotationArguments(v, value as Annotation, annotationClass)
            }
            clazz.isArray() -> {
                val v = visitor.visitArray(name) ?: return
                val componentType = clazz.getComponentType()
                if (componentType.isEnum()) {
                    val enumClassId = componentType.classId
                    for (element in value as Array<*>) {
                        v.visitEnum(enumClassId, Name.identifier((element as Enum<*>).name()))
                    }
                }
                else {
                    for (element in value as Array<*>) {
                        v.visit(element)
                    }
                }
                v.visitEnd()
            }
            else -> {
                throw UnsupportedOperationException("Unsupported annotation argument value ($clazz): $value")
            }
        }
    }
}

private object SignatureSerializer {
    fun methodDesc(method: Method): String {
        val sb = StringBuilder()
        sb.append("(")
        for (parameterType in method.getParameterTypes()) {
            sb.append(parameterType.desc)
        }
        sb.append(")")
        sb.append(method.getReturnType().desc)
        return sb.toString()
    }

    fun constructorDesc(constructor: Constructor<*>): String {
        val sb = StringBuilder()
        sb.append("(")
        for (parameterType in constructor.getParameterTypes()) {
            sb.append(parameterType.desc)
        }
        sb.append(")V")
        return sb.toString()
    }

    fun fieldDesc(field: Field): String {
        return field.getType().desc
    }
}
