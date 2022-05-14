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

package org.jetbrains.jet.lang.resolve.java.resolver

import org.jetbrains.jet.lang.resolve.source.PsiSourceElement
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.resolve.java.structure.JavaElement
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaElementImpl
import org.jetbrains.jet.lang.resolve.java.sources.JavaSourceElementFactory
import org.jetbrains.jet.lang.descriptors.SourceElement
import org.jetbrains.jet.lang.resolve.java.sources.JavaSourceElement

private class JavaSourceElementImpl(override val javaElement: JavaElement) : PsiSourceElement, JavaSourceElement {
    override val psi: PsiElement?
        get() = (javaElement as JavaElementImpl<*>).getPsi()
}

public class JavaSourceElementFactoryImpl : JavaSourceElementFactory {
    override fun source(javaElement: JavaElement): JavaSourceElement = JavaSourceElementImpl(javaElement)
}