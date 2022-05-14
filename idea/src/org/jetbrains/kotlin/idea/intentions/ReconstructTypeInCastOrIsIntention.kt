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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.replaced
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.JetType

public class ReconstructTypeInCastOrIsIntention : JetSelfTargetingOffsetIndependentIntention<JetTypeReference>(javaClass(), "Replace by reconstructed type") {
    override fun isApplicableTo(element: JetTypeReference): Boolean {
        // Only user types (like Foo) are interesting
        val typeElement = element.getTypeElement() as? JetUserType ?: return false

        // If there are generic arguments already, there's nothing to reconstruct
        if (typeElement.getTypeArguments().isNotEmpty()) return false

        // We must be on the RHS of as/as?/is/!is or inside an is/!is-condition in when()
        val expression = element.getParentOfType<JetExpression>(true)
        if (expression !is JetBinaryExpressionWithTypeRHS && element.getParentOfType<JetWhenConditionIsPattern>(true) == null) return false

        val type = getReconstructedType(element)
        if (type == null || type.isError()) return false

        // No type parameters expected => nothing to reconstruct
        if (type.getConstructor().getParameters().isEmpty()) return false

        val typePresentation = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type)
        setText("Replace by '$typePresentation'")

        return true
    }

    override fun applyTo(element: JetTypeReference, editor: Editor) {
        val type = getReconstructedType(element)!!
        val newType = JetPsiFactory(element).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type))
        ShortenReferences.DEFAULT.process(element.replaced(newType))
    }

    private fun getReconstructedType(typeRef: JetTypeReference): JetType? {
        return typeRef.analyze().get(BindingContext.TYPE, typeRef)
    }
}
