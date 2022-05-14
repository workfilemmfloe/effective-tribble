/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.uast

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.psi.PsiElementBacked

class KotlinULiteralExpression(
        override val psi: KtConstantExpression,
        override val parent: UElement
) : KotlinAbstractUElement(), ULiteralExpression, PsiElementBacked, KotlinUElementWithType, KotlinEvaluatableUElement {
    override val isNull: Boolean
        get() = psi.isNullExpression()

    override val value by lz { evaluate() }
}

class KotlinStringULiteralExpression(
        override val psi: PsiElement,
        override val parent: UElement,
        val text: String? = null
) : KotlinAbstractUElement(), ULiteralExpression, PsiElementBacked, KotlinUElementWithType{
    override val value: String
        get() = text ?: StringUtil.unescapeStringCharacters(psi.text)

    override fun evaluate() = value
}