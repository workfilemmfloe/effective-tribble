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

package org.jetbrains.kotlin.load.java.structure.reflect

import org.jetbrains.kotlin.load.java.structure.JavaValueParameter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.Name.guess

public class ReflectJavaValueParameter(
        private val returnType: ReflectJavaType,
        private val annotations: Array<Annotation>,
        private val name: String?,
        private val isVararg: Boolean
) : ReflectJavaElement(), JavaValueParameter {
    override fun getAnnotations() = getAnnotations(annotations)

    override fun findAnnotation(fqName: FqName) = findAnnotation(annotations, fqName)

    override fun isDeprecatedInJavaDoc() = false

    override fun getName() = name?.let(Name::guess)
    override fun getType() = returnType
    override fun isVararg() = isVararg

    override fun toString() = javaClass.getName() + ": " + (if (isVararg) "vararg " else "") + getName() + ": " + returnType
}
