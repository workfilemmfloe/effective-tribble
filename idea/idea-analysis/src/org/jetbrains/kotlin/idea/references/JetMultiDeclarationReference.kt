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

package org.jetbrains.kotlin.idea.references

import org.jetbrains.kotlin.psi.JetMultiDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze

class JetMultiDeclarationReference(element: JetMultiDeclaration) : JetMultiReference<JetMultiDeclaration>(element) {
    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        return expression.getEntries().map { entry ->
            context.get(BindingContext.COMPONENT_RESOLVED_CALL, entry)?.getCandidateDescriptor()
        }.filterNotNull()
    }

    override fun getRangeInElement(): TextRange? {
        val start = expression.getLPar()
        val end = expression.getRPar()
        if (start == null || end == null) return TextRange.EMPTY_RANGE
        return TextRange(start.getStartOffsetInParent(), end.getStartOffsetInParent())
    }

    override fun canRename(): Boolean {
        val bindingContext = expression.analyze() //TODO: should it use full body resolve?
        return resolveToDescriptors(bindingContext).all { it is CallableMemberDescriptor && it.getKind() == CallableMemberDescriptor.Kind.SYNTHESIZED}
    }

    override fun handleElementRename(newElementName: String?): PsiElement? {
        if (canRename()) return expression
        throw IncorrectOperationException()
    }
}
