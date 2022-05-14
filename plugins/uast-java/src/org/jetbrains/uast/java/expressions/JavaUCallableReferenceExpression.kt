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

import com.intellij.psi.PsiMethodReferenceExpression
import org.jetbrains.uast.UCallableReferenceExpression
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UastContext
import org.jetbrains.uast.psi.PsiElementBacked

class JavaUCallableReferenceExpression(
        override val psi: PsiMethodReferenceExpression,
        override val parent: UElement
) : JavaAbstractUElement(), UCallableReferenceExpression, PsiElementBacked, JavaUElementWithType {
    override val qualifierExpression by lz { JavaConverter.convertOrNull(psi.qualifierExpression, this) }
    override val qualifierType by lz { JavaConverter.convert(psi.qualifierType?.type, this) }
    override val callableName: String
        get() = psi.referenceName.orAnonymous()

    override fun resolve(context: UastContext) = context.convert(psi.resolve()) as? UDeclaration
}