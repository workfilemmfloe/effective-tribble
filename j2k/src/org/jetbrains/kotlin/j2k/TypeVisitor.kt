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

package org.jetbrains.kotlin.j2k

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.kotlin.j2k.ast.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType

private val PRIMITIVE_TYPES_NAMES = JvmPrimitiveType.values().map { it.getJavaKeywordName() }

class TypeVisitor(
        private val converter: Converter,
        private val topLevelType: PsiType,
        private val topLevelTypeMutability: Mutability,
        private val inAnnotationType: Boolean
) : PsiTypeVisitor<Type>() {

    private val typeConverter: TypeConverter = converter.typeConverter

    //TODO: support for all types
    override fun visitType(type: PsiType) = ErrorType()

    override fun visitPrimitiveType(primitiveType: PsiPrimitiveType): Type {
        val name = primitiveType.getCanonicalText()
        return if (name == "void") {
            UnitType()
        }
        else if (PRIMITIVE_TYPES_NAMES.contains(name)) {
            PrimitiveType(Identifier(StringUtil.capitalize(name)).assignNoPrototype())
        }
        else if (name == "null") {
            NullType()
        }
        else {
            PrimitiveType(Identifier(name).assignNoPrototype())
        }
    }

    override fun visitArrayType(arrayType: PsiArrayType): Type {
        return ArrayType(typeConverter.convertType(arrayType.getComponentType(), inAnnotationType = inAnnotationType), Nullability.Default, converter.settings)
    }

    override fun visitClassType(classType: PsiClassType): Type {
        val mutability = if (classType === topLevelType) topLevelTypeMutability else Mutability.Default
        val refElement = constructReferenceElement(classType, mutability)
        return ClassType(refElement, Nullability.Default, converter.settings)
    }

    private fun constructReferenceElement(classType: PsiClassType, mutability: Mutability): ReferenceElement {
        val typeArgs = convertTypeArgs(classType)

        val psiClass = classType.resolve()
        if (psiClass != null) {
            val javaClassName = psiClass.getQualifiedName()
            val kotlinClassName = (if (mutability.isMutable(converter.settings)) toKotlinMutableTypesMap[javaClassName] else null)
                                  ?: toKotlinTypesMap[javaClassName]
            if (kotlinClassName != null) {
                return ReferenceElement(Identifier(getShortName(kotlinClassName)).assignNoPrototype(), typeArgs).assignNoPrototype()
            }

            if (inAnnotationType && javaClassName == "java.lang.Class") {
                val fqName = FqName("kotlin.reflect.KClass")
                val identifier = Identifier(fqName.shortName().getIdentifier(), imports = listOf(fqName)).assignNoPrototype()
                return ReferenceElement(identifier, typeArgs).assignNoPrototype()
            }
        }

        if (classType is PsiClassReferenceType) {
            return converter.convertCodeReferenceElement(classType.getReference(), hasExternalQualifier = false, typeArgsConverted = typeArgs)
        }

        return ReferenceElement(Identifier(classType.getClassName() ?: "").assignNoPrototype(), typeArgs).assignNoPrototype()
    }

    private fun getShortName(className: String): String = className.substringAfterLast('.', className)

    private fun convertTypeArgs(classType: PsiClassType): List<Type> {
        if (classType.getParameterCount() == 0) {
            return createTypeArgsForRawTypeUsage(classType, Mutability.Default)
        }
        else {
            return typeConverter.convertTypes(classType.getParameters())
        }
    }

    private fun createTypeArgsForRawTypeUsage(classType: PsiClassType, mutability: Mutability): List<Type> {
        if (classType is PsiClassReferenceType) {
            val targetClass = classType.getReference().resolve() as? PsiClass
            if (targetClass != null) {
                return targetClass.getTypeParameters().map {
                    val superType = it.getSuperTypes().first() // there must be at least one super type always
                    typeConverter.convertType(superType, Nullability.Default, mutability, inAnnotationType).assignNoPrototype()
                }
            }
        }
        return listOf()
    }

    override fun visitWildcardType(wildcardType: PsiWildcardType): Type {
        return when {
            wildcardType.isExtends() -> OutProjectionType(typeConverter.convertType(wildcardType.getExtendsBound()))
            wildcardType.isSuper() -> InProjectionType(typeConverter.convertType(wildcardType.getSuperBound()))
            else -> StarProjectionType()
        }
    }

    override fun visitEllipsisType(ellipsisType: PsiEllipsisType): Type {
        return VarArgType(typeConverter.convertType(ellipsisType.getComponentType(), inAnnotationType = inAnnotationType))
    }

    companion object {
        private val toKotlinTypesMap: Map<String, String> = mapOf(
                CommonClassNames.JAVA_LANG_OBJECT to "kotlin.Any",
                CommonClassNames.JAVA_LANG_BYTE to "kotlin.Byte",
                CommonClassNames.JAVA_LANG_CHARACTER to "kotlin.Char",
                CommonClassNames.JAVA_LANG_DOUBLE to "kotlin.Double",
                CommonClassNames.JAVA_LANG_FLOAT to "kotlin.Float",
                CommonClassNames.JAVA_LANG_INTEGER to "kotlin.Int",
                CommonClassNames.JAVA_LANG_LONG to "kotlin.Long",
                CommonClassNames.JAVA_LANG_SHORT to "kotlin.Short",
                CommonClassNames.JAVA_LANG_BOOLEAN to "kotlin.Boolean",
                CommonClassNames.JAVA_LANG_ITERABLE to "kotlin.Iterable",
                CommonClassNames.JAVA_UTIL_ITERATOR to "kotlin.Iterator",
                CommonClassNames.JAVA_UTIL_LIST to "kotlin.List",
                CommonClassNames.JAVA_UTIL_COLLECTION to "kotlin.Collection",
                CommonClassNames.JAVA_UTIL_SET to "kotlin.Set",
                CommonClassNames.JAVA_UTIL_MAP to "kotlin.Map"
        )

        public val toKotlinMutableTypesMap: Map<String, String> = mapOf(
                CommonClassNames.JAVA_UTIL_LIST to "kotlin.MutableList",
                CommonClassNames.JAVA_UTIL_COLLECTION to "kotlin.MutableCollection",
                CommonClassNames.JAVA_UTIL_SET to "kotlin.MutableSet",
                CommonClassNames.JAVA_UTIL_MAP to "kotlin.MutableMap"
        )
    }
}
