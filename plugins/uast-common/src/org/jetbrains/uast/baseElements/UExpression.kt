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
package org.jetbrains.uast

import org.jetbrains.uast.visitor.UastVisitor

interface UExpression : UElement {
    open fun evaluate(): Any? = null
    fun evaluateString(): String? = evaluate() as? String
    fun getExpressionType(): UType? = null

    override fun accept(visitor: UastVisitor) {
        visitor.visitElement(this)
        visitor.afterVisitElement(this)
    }
}

interface NoAnnotations : UAnnotated {
    override val annotations: List<UAnnotation>
        get() = emptyList()
}

interface NoModifiers : UModifierOwner {
    override fun hasModifier(modifier: UastModifier) = false
}

class EmptyUExpression(override val parent: UElement) : UExpression {
    override fun logString() = "EmptyExpression"
}