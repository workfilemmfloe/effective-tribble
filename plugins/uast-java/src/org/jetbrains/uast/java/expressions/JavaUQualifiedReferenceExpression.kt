/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.uast.java

import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReferenceExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UastQualifiedExpressionAccessType
import org.jetbrains.uast.psi.PsiElementBacked

class JavaUQualifiedReferenceExpression(
        override val psi: PsiReferenceExpression,
        override val containingElement: UElement?
) : JavaAbstractUExpression(), UQualifiedReferenceExpression, PsiElementBacked {
    override val receiver by lz { JavaConverter.convertOrEmpty(psi.qualifierExpression, this) }
    override val selector by lz { 
        JavaUSimpleNameReferenceExpression(psi.referenceNameElement, psi.referenceName ?: "<error>", this, psi) }
    
    override val accessType: UastQualifiedExpressionAccessType
        get() = UastQualifiedExpressionAccessType.SIMPLE

    override val resolvedName: String?
        get() = (psi.resolve() as? PsiNamedElement)?.name
    
    override fun resolve() = psi.resolve()
}