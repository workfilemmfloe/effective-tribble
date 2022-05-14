/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.search.declarationsSearch

import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiUtil
import org.jetbrains.jet.lang.psi.JetDeclaration
import com.intellij.psi.PsiElement
import com.intellij.util.Processor
import com.intellij.util.Query
import com.intellij.psi.search.GlobalSearchScope
import java.util.HashMap
import org.jetbrains.jet.lang.psi.psiUtil.*
import org.jetbrains.jet.lang.psi.JetNamedFunction
import java.util.Collections
import org.jetbrains.jet.asJava.LightClassUtil
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.JetPropertyAccessor
import com.intellij.psi.PsiClass
import org.jetbrains.jet.lang.psi.JetParameter
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.util.MethodSignatureUtil
import com.intellij.psi.PsiModifier
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.searches.DirectClassInheritorsSearch
import com.intellij.util.EmptyQuery
import com.intellij.util.MergeQuery
import com.intellij.util.UniqueResultsQuery
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.jet.asJava.toLightMethods

fun PsiElement.isOverridableElement(): Boolean = when (this) {
    is PsiMethod -> PsiUtil.canBeOverriden(this)
    is JetDeclaration -> isOverridable()
    else -> false
}

public fun HierarchySearchRequest<PsiElement>.searchOverriders(): Query<PsiMethod> {
    val psiMethods = originalElement.toLightMethods()
    if (psiMethods.isEmpty()) return EmptyQuery.getEmptyQuery()

    return psiMethods
            .map { psiMethod -> KotlinPsiMethodOverridersSearch.search(copy(psiMethod)) }
            .reduce {(query1, query2) -> MergeQuery(query1, query2)}
}

public object KotlinPsiMethodOverridersSearch : HierarchySearch<PsiMethod>(PsiMethodOverridingHierarchyTraverser) {
    fun searchDirectOverriders(psiMethod: PsiMethod): Iterable<PsiMethod> {
        fun PsiMethod.isAcceptable(inheritor: PsiClass, baseMethod: PsiMethod, baseClass: PsiClass): Boolean =
                when {
                    hasModifierProperty(PsiModifier.STATIC) -> false
                    baseMethod.hasModifierProperty(PsiModifier.PACKAGE_LOCAL) ->
                        JavaPsiFacade.getInstance(getProject()).arePackagesTheSame(baseClass, inheritor)
                    else -> true
                }

        val psiClass = psiMethod.getContainingClass()
        if (psiClass == null) return Collections.emptyList()

        val classToMethod = HashMap<PsiClass, PsiMethod>()
        val classTraverser = object : HierarchyTraverser<PsiClass> {
            override fun nextElements(current: PsiClass): Iterable<PsiClass> =
                    DirectClassInheritorsSearch.search(
                            aClass = current,
                            scope = GlobalSearchScope.allScope(current.getProject()),
                            checkInheritance = true,
                            includeAnonymous = true
                    )

            override fun shouldDescend(element: PsiClass): Boolean =
                    element.isInheritable() && !classToMethod.containsKey(element)
        }

        classTraverser.forEach(psiClass) { inheritor ->
            val substitutor = TypeConversionUtil.getSuperClassSubstitutor(psiClass, inheritor, PsiSubstitutor.EMPTY)
            val signature = psiMethod.getSignature(substitutor)
            val candidate = MethodSignatureUtil.findMethodBySuperSignature(inheritor, signature, false)
            if (candidate != null && candidate.isAcceptable(inheritor, psiMethod, psiClass)) {
                classToMethod.put(inheritor, candidate)
            }
        }

        return classToMethod.values()
    }

    protected override fun isApplicable(request: HierarchySearchRequest<PsiMethod>): Boolean =
            request.originalElement.isOverridableElement()

    override fun doSearchDirect(request: HierarchySearchRequest<PsiMethod>, consumer: Processor<PsiMethod>) {
        searchDirectOverriders(request.originalElement).forEach { method -> consumer.process(method) }
    }
}

object PsiMethodOverridingHierarchyTraverser: HierarchyTraverser<PsiMethod> {
    override fun nextElements(current: PsiMethod): Iterable<PsiMethod> = KotlinPsiMethodOverridersSearch.searchDirectOverriders(current)
    override fun shouldDescend(element: PsiMethod): Boolean = PsiUtil.canBeOverriden(element)
}