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

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.name.Name

open public class JetExpressionWithLabel(node: ASTNode) : JetExpressionImpl(node) {

    public fun getTargetLabel(): JetSimpleNameExpression? =
            labelQualifier?.findChildByType(JetNodeTypes.LABEL) as? JetSimpleNameExpression

    public val labelQualifier: JetContainerNode?
        get() = findChildByType(JetNodeTypes.LABEL_QUALIFIER)

    public fun getLabelName(): String? = getTargetLabel()?.getReferencedName()
    public fun getLabelNameAsName(): Name? = getTargetLabel()?.getReferencedNameAsName()

    override fun <R, D> accept(visitor: JetVisitor<R, D>, data: D) = visitor.visitExpressionWithLabel(this, data)
}
