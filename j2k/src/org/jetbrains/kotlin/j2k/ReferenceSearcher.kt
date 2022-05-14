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

import com.intellij.psi.PsiVariable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiClass
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.OverridingMethodsSearch

public trait ReferenceSearcher {
    fun findVariableUsages(variable: PsiVariable, scope: PsiElement): Collection<PsiReferenceExpression>
    fun findMethodCalls(method: PsiMethod, scope: PsiElement): Collection<PsiMethodCallExpression>
    fun hasInheritors(`class`: PsiClass): Boolean
    fun hasOverrides(method: PsiMethod): Boolean
}

public object EmptyReferenceSearcher: ReferenceSearcher {
    override fun findVariableUsages(variable: PsiVariable, scope: PsiElement) = listOf<PsiReferenceExpression>()
    override fun findMethodCalls(method: PsiMethod, scope: PsiElement) = listOf<PsiMethodCallExpression>()
    override fun hasInheritors(`class`: PsiClass) = false
    override fun hasOverrides(method: PsiMethod) = false
}

public object IdeaReferenceSearcher : ReferenceSearcher {
    override fun findVariableUsages(variable: PsiVariable, scope: PsiElement): Collection<PsiReferenceExpression> {
        return ReferencesSearch.search(variable, LocalSearchScope(scope)).findAll().filterIsInstance<PsiReferenceExpression>()
    }

    override fun findMethodCalls(method: PsiMethod, scope: PsiElement): Collection<PsiMethodCallExpression> {
        return ReferencesSearch.search(method, LocalSearchScope(scope)).findAll().map {
            if (it is PsiReferenceExpression) {
                val methodCall = it.getParent() as? PsiMethodCallExpression
                if (methodCall?.getMethodExpression() == it) methodCall else null
            }
            else {
                null
            }
        }.filterNotNull()
    }

    override fun hasInheritors(`class`: PsiClass) = ClassInheritorsSearch.search(`class`, false).any()

    override fun hasOverrides(method: PsiMethod) = OverridingMethodsSearch.search(method, false).any()
}
