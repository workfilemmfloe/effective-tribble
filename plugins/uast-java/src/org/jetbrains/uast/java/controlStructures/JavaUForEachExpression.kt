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

import com.intellij.psi.PsiForeachStatement
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UForEachExpression
import org.jetbrains.uast.psi.PsiElementBacked

class JavaUForEachExpression(
        override val psi: PsiForeachStatement,
        override val parent: UElement
) : JavaAbstractUElement(), UForEachExpression, PsiElementBacked {
    override val variable by lz { JavaConverter.convert(psi.iterationParameter, this) }

    override val iteratedValue by lz { JavaConverter.convertOrEmpty(psi.iteratedValue, this) }
    override val body by lz { JavaConverter.convertOrEmpty(psi.body, this) }
}