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

package org.jetbrains.kotlin.j2k

import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil

public interface ReferenceSearcher {
    fun findLocalUsages(element: PsiElement, scope: PsiElement): Collection<PsiReference>
    fun hasInheritors(`class`: PsiClass): Boolean
    fun hasOverrides(method: PsiMethod): Boolean

    fun findUsagesForExternalCodeProcessing(element: PsiElement, searchJava: Boolean, searchKotlin: Boolean): Collection<PsiReference>
}

public fun ReferenceSearcher.findVariableUsages(variable: PsiVariable, scope: PsiElement): Collection<PsiReferenceExpression>
        = findLocalUsages(variable, scope).filterIsInstance<PsiReferenceExpression>()

public fun ReferenceSearcher.findMethodCalls(method: PsiMethod, scope: PsiElement): Collection<PsiMethodCallExpression> {
    return findLocalUsages(method, scope).map {
        if (it is PsiReferenceExpression) {
            val methodCall = it.getParent() as? PsiMethodCallExpression
            if (methodCall?.getMethodExpression() == it) methodCall else null
        }
        else {
            null
        }
    }.filterNotNull()
}

public fun PsiField.isVal(searcher: ReferenceSearcher): Boolean {
    if (hasModifierProperty(PsiModifier.FINAL)) return true
    if (!hasModifierProperty(PsiModifier.PRIVATE)) return false
    val containingClass = getContainingClass() ?: return false
    val writes = searcher.findVariableUsages(this, containingClass).filter { PsiUtil.isAccessedForWriting(it) }
    if (writes.size() == 0) return true
    if (writes.size() > 1) return false
    val write = writes.single()
    val parent = write.getParent()
    if (parent is PsiAssignmentExpression &&
        parent.getOperationSign().getTokenType() == JavaTokenType.EQ &&
        write.isQualifierEmptyOrThis()) {
        val constructor = write.getContainingConstructor()
        return constructor != null &&
               constructor.getContainingClass() == containingClass &&
               parent.getParent() is PsiExpressionStatement &&
               parent.getParent()?.getParent() == constructor.getBody()
    }
    return false
}

public fun PsiVariable.hasWriteAccesses(searcher: ReferenceSearcher, scope: PsiElement?): Boolean
        = if (scope != null) searcher.findVariableUsages(this, scope).any { PsiUtil.isAccessedForWriting(it) } else false

public object EmptyReferenceSearcher: ReferenceSearcher {
    override fun findLocalUsages(element: PsiElement, scope: PsiElement): Collection<PsiReference> = emptyList()
    override fun hasInheritors(`class`: PsiClass) = false
    override fun hasOverrides(method: PsiMethod) = false
    override fun findUsagesForExternalCodeProcessing(element: PsiElement, searchJava: Boolean, searchKotlin: Boolean): Collection<PsiReference>
            = throw UnsupportedOperationException()
}