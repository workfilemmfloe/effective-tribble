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

package org.jetbrains.kotlin.idea.caches.resolve

import org.jetbrains.kotlin.asJava.KotlinWrappingLightClass
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.compiled.ClsClassImpl
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.name.FqName

class KotlinLightClassForDecompiledDeclaration(
        private val clsClass: ClsClassImpl,
        private val origin: JetClassOrObject?
) : KotlinWrappingLightClass(clsClass.getManager()) {
    override fun copy() = this

    override fun getOwnInnerClasses(): List<PsiClass> {
        val nestedClasses = origin?.getDeclarations()?.filterIsInstance<JetClassOrObject>() ?: emptyList()
        return clsClass.getOwnInnerClasses().map { innerClsClass ->
            KotlinLightClassForDecompiledDeclaration(innerClsClass as ClsClassImpl,
                                                     nestedClasses.firstOrNull { innerClsClass.getName() == it.getName() })
        }
    }

    override fun getNavigationElement() = origin?.getNavigationElement() ?: super.getNavigationElement()

    override fun getDelegate() = clsClass

    override fun getOrigin() = origin

    override fun getFqName() = origin?.getFqName() ?: FqName(clsClass.getQualifiedName())
}
