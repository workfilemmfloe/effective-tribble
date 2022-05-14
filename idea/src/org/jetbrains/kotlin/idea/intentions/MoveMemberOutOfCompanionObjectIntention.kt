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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.idea.refactoring.checkConflictsInteractively
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*
import org.jetbrains.kotlin.idea.refactoring.runSynchronouslyWithProgress
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isAncestor

class MoveMemberOutOfCompanionObjectIntention : SelfTargetingRangeIntention<KtNamedDeclaration>(KtNamedDeclaration::class.java,
                                                                                                "Move out of companion object") {
    override fun applicabilityRange(element: KtNamedDeclaration): TextRange? {
        if (element !is KtNamedFunction && element !is KtProperty && element !is KtClassOrObject) return null
        val container = element.containingClassOrObject
        if (!(container is KtObjectDeclaration && container.isCompanion())) return null
        if (container.containingClassOrObject == null) return null
        return element.nameIdentifier?.textRange
    }

    override fun applyTo(element: KtNamedDeclaration, editor: Editor?) {
        val project = element.project

        val companionObject = element.containingClassOrObject!!
        val targetClass = companionObject.containingClassOrObject!!

        fun deleteCompanionIfEmpty() {
            if (companionObject.declarations.isEmpty()) {
                companionObject.delete()
            }
        }

        if (element is KtClassOrObject) {
            val moveDescriptor = MoveDeclarationsDescriptor(listOf(element),
                                                            KotlinMoveTargetForExistingElement(targetClass),
                                                            MoveDeclarationsDelegate.NestedClass())
            MoveKotlinDeclarationsProcessor(project, moveDescriptor).run()
            deleteCompanionIfEmpty()
            return
        }

        val externalRefs = project.runSynchronouslyWithProgress("Searching for ${element.name}", true) {
            ReferencesSearch.search(element).filter {
                val refElement = it.element ?: return@filter false
                !targetClass.isAncestor(refElement) || companionObject.isAncestor(refElement)
            }
        } ?: return

        val conflicts = MultiMap<PsiElement, String>()
        for (ref in externalRefs) {
            val refElement = ref.element ?: continue
            conflicts.putValue(refElement, "Class instance required: ${refElement.text}")
        }

        project.checkConflictsInteractively(conflicts) {
            Mover.Default(element, targetClass)
            deleteCompanionIfEmpty()
        }
    }
}