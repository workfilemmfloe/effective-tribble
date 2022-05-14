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

package org.jetbrains.kotlin.load.java.lazy.descriptors

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.structure.JavaMethod
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

public abstract class LazyJavaStaticScope(c: LazyJavaResolverContext) : LazyJavaScope(c) {

    override fun getDispatchReceiverParameter() = null

    // Package fragments are not nested
    override fun getPackage(name: Name) = null
    abstract fun getSubPackages(): Collection<FqName>

    override fun resolveMethodSignature(
            method: JavaMethod, methodTypeParameters: List<TypeParameterDescriptor>, returnType: KotlinType,
            valueParameters: LazyJavaScope.ResolvedValueParameters
    ): LazyJavaScope.MethodSignatureData {
        val effectiveSignature = c.components.externalSignatureResolver.resolveAlternativeMethodSignature(
                method, false, returnType, null, valueParameters.descriptors, methodTypeParameters, false)
        return LazyJavaScope.MethodSignatureData(effectiveSignature, effectiveSignature.getErrors())
    }

    override fun computeNonDeclaredProperties(name: Name, result: MutableCollection<PropertyDescriptor>) {
        //no undeclared properties
    }
}
