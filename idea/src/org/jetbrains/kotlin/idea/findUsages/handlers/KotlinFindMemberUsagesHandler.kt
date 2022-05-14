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

package org.jetbrains.kotlin.idea.findUsages.handlers

import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector
import com.intellij.find.findUsages.AbstractFindUsagesDialog
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usageView.UsageInfo
import com.intellij.util.*
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.idea.findUsages.KotlinCallableFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.KotlinFindUsagesHandlerFactory
import org.jetbrains.kotlin.idea.findUsages.KotlinFunctionFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.KotlinPropertyFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.dialogs.KotlinFindFunctionUsagesDialog
import org.jetbrains.kotlin.idea.findUsages.dialogs.KotlinFindPropertyUsagesDialog
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchOverriders
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReadWriteAccessDetector
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchOptions
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchParameters
import org.jetbrains.kotlin.idea.search.usagesSearch.isImportUsage
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.JetFunction
import org.jetbrains.kotlin.psi.JetNamedDeclaration

public abstract class KotlinFindMemberUsagesHandler<T : JetNamedDeclaration>
    protected constructor(declaration: T, elementsToSearch: Collection<PsiElement>, factory: KotlinFindUsagesHandlerFactory)
    : KotlinFindUsagesHandler<T>(declaration, elementsToSearch, factory) {

    private class Function(declaration: JetFunction,
                           elementsToSearch: Collection<PsiElement>,
                           factory: KotlinFindUsagesHandlerFactory) : KotlinFindMemberUsagesHandler<JetFunction>(declaration, elementsToSearch, factory) {

        override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions = factory.findFunctionOptions

        override fun getFindUsagesDialog(isSingleFile: Boolean, toShowInNewTab: Boolean, mustOpenInNewTab: Boolean): AbstractFindUsagesDialog {
            val options = factory.findFunctionOptions
            val lightMethod = getElement().toLightMethods().firstOrNull()
            if (lightMethod != null) {
                return KotlinFindFunctionUsagesDialog(lightMethod, getProject(), options, toShowInNewTab, mustOpenInNewTab, isSingleFile, this)
            }

            return super.getFindUsagesDialog(isSingleFile, toShowInNewTab, mustOpenInNewTab)
        }

        override fun createKotlinReferencesSearchOptions(options: FindUsagesOptions): KotlinReferencesSearchOptions {
            val kotlinOptions = options as KotlinFunctionFindUsagesOptions
            return KotlinReferencesSearchOptions(true,
                                                 kotlinOptions.isIncludeOverloadUsages,
                                                 kotlinOptions.isIncludeOverloadUsages)
        }

        override fun applyQueryFilters(element: PsiElement, options: FindUsagesOptions, query: Query<PsiReference>): Query<PsiReference> {
            val kotlinOptions = options as KotlinFunctionFindUsagesOptions
            return query
                .applyFilter(kotlinOptions.isSkipImportStatements) { !it.isImportUsage() }
        }
    }

    private class Property(declaration: JetNamedDeclaration, elementsToSearch: Collection<PsiElement>, factory: KotlinFindUsagesHandlerFactory) : KotlinFindMemberUsagesHandler<JetNamedDeclaration>(declaration, elementsToSearch, factory) {

        override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions = factory.findPropertyOptions

        override fun getFindUsagesDialog(isSingleFile: Boolean, toShowInNewTab: Boolean, mustOpenInNewTab: Boolean): AbstractFindUsagesDialog {
            return KotlinFindPropertyUsagesDialog(getElement(), getProject(), factory.findPropertyOptions, toShowInNewTab, mustOpenInNewTab, isSingleFile, this)
        }

        override fun applyQueryFilters(element: PsiElement, options: FindUsagesOptions, query: Query<PsiReference>): Query<PsiReference> {
            var kotlinOptions = options as KotlinPropertyFindUsagesOptions

            if (!kotlinOptions.isReadAccess && !kotlinOptions.isWriteAccess) {
                return EmptyQuery()
            }

            val result = query
                    .applyFilter(kotlinOptions.isSkipImportStatements) { !it.isImportUsage() }

            if (!kotlinOptions.isReadAccess || !kotlinOptions.isWriteAccess) {
                val detector = KotlinReadWriteAccessDetector()

                return FilteredQuery(result) {
                    val access = detector.getReferenceAccess(element, it)
                   ^when (access) {
                        ReadWriteAccessDetector.Access.Read -> kotlinOptions.isReadAccess
                        ReadWriteAccessDetector.Access.Write -> kotlinOptions.isWriteAccess
                        ReadWriteAccessDetector.Access.ReadWrite -> true
                    }
                }
            }
            return result
        }

        override fun createKotlinReferencesSearchOptions(options: FindUsagesOptions): KotlinReferencesSearchOptions {
            return KotlinReferencesSearchOptions(true, false, false)
        }
    }

    override fun searchReferences(element: PsiElement, processor: Processor<UsageInfo>, options: FindUsagesOptions): Boolean {
        val kotlinOptions = options as KotlinCallableFindUsagesOptions

        val referenceProcessor = KotlinFindUsagesHandler.createReferenceProcessor(processor)
        val uniqueProcessor = CommonProcessors.UniqueProcessor(processor)

        if (options.isUsages) {
            val searchParameters = KotlinReferencesSearchParameters(element,
                                                                    scope = options.searchScope,
                                                                    kotlinOptions = createKotlinReferencesSearchOptions(options))

            val query = applyQueryFilters(element, options, ReferencesSearch.search(searchParameters))
            if (!query.forEach(referenceProcessor)) return false

            val psiMethods = when (element) {
                is PsiMethod -> listOf(element)
                is JetFunction -> runReadAction { LightClassUtil.getLightClassMethods(element) }
                else -> listOf<PsiMethod>()
            }
            for (psiMethod in psiMethods) {
                val query = applyQueryFilters(element, options, MethodReferencesSearch.search(psiMethod, options.searchScope, true))
                if (!query.forEach(referenceProcessor)) return false
            }
        }

        if (kotlinOptions.searchOverrides) {
            for (method in HierarchySearchRequest(element, options.searchScope, true).searchOverriders()) {
                if (!KotlinFindUsagesHandler.processUsage(uniqueProcessor, method.getNavigationElement())) break
            }
        }

        return true
    }

    protected abstract fun createKotlinReferencesSearchOptions(options: FindUsagesOptions): KotlinReferencesSearchOptions

    protected abstract fun applyQueryFilters(element: PsiElement,
                                             options: FindUsagesOptions,
                                             query: Query<PsiReference>): Query<PsiReference>

    override fun isSearchForTextOccurencesAvailable(psiElement: PsiElement, isSingleFile: Boolean): Boolean = !isSingleFile

    companion object {

        public fun getInstance(declaration: JetNamedDeclaration,
                               elementsToSearch: Collection<PsiElement> = emptyList(),
                               factory: KotlinFindUsagesHandlerFactory): KotlinFindMemberUsagesHandler<out JetNamedDeclaration> {
            return if (declaration is JetFunction)
                Function(declaration, elementsToSearch, factory)
            else
                Property(declaration, elementsToSearch, factory)
        }
    }
}


fun Query<PsiReference>.applyFilter(flag: Boolean, condition: (PsiReference) -> Boolean): Query<PsiReference> {
    return if (flag) FilteredQuery(this, condition) else this
}
