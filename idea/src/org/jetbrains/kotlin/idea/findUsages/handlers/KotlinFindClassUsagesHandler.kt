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

import com.intellij.find.findUsages.AbstractFindUsagesDialog
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.*
import com.intellij.psi.meta.PsiMetaOwner
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.psi.search.PsiElementProcessorAdapter
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.usageView.UsageInfo
import com.intellij.util.FilteredQuery
import com.intellij.util.Processor
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.asJava.KtLightMethod
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.findUsages.KotlinClassFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.KotlinFindUsagesHandlerFactory
import org.jetbrains.kotlin.idea.findUsages.dialogs.KotlinFindClassUsagesDialog
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchInheritors
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchOptions
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchParameters
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.search.usagesSearch.isConstructorUsage
import org.jetbrains.kotlin.idea.search.usagesSearch.isImportUsage
import org.jetbrains.kotlin.idea.search.usagesSearch.processDelegationCallConstructorUsages
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.contains
import org.jetbrains.kotlin.psi.psiUtil.effectiveDeclarations
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import java.util.*

class KotlinFindClassUsagesHandler(
        ktClass: KtClassOrObject,
        factory: KotlinFindUsagesHandlerFactory
) : KotlinFindUsagesHandler<KtClassOrObject>(ktClass, factory) {
    override fun getFindUsagesDialog(
            isSingleFile: Boolean, toShowInNewTab: Boolean, mustOpenInNewTab: Boolean
    ): AbstractFindUsagesDialog {
        return KotlinFindClassUsagesDialog(getElement(),
                                           project,
                                           factory.findClassOptions,
                                           toShowInNewTab,
                                           mustOpenInNewTab,
                                           isSingleFile,
                                           this)
    }

    override fun searchReferences(element: PsiElement, processor: Processor<UsageInfo>, options: FindUsagesOptions): Boolean {
        val kotlinOptions = options as KotlinClassFindUsagesOptions

        fun processInheritors(): Boolean {
            val request = HierarchySearchRequest(element, options.searchScope, options.isCheckDeepInheritance)
            return request.searchInheritors().forEach(
                    PsiElementProcessorAdapter(
                            object : PsiElementProcessor<PsiClass> {
                                override fun execute(element: PsiClass): Boolean {
                                    val isInterface = element.isInterface
                                    return when {
                                        isInterface && options.isDerivedInterfaces || !isInterface && options.isDerivedClasses ->
                                            KotlinFindUsagesHandler.processUsage(processor, element.navigationElement)
                                        else -> true
                                    }
                                }
                            }
                    )
            )
        }

        val classOrObject = element as KtClassOrObject
        val referenceProcessor = KotlinFindUsagesHandler.createReferenceProcessor(processor)

        if (kotlinOptions.isUsages || kotlinOptions.searchConstructorUsages) {
            if (!processClassReferences(classOrObject, kotlinOptions, referenceProcessor)) return false
        }

        if (kotlinOptions.isFieldsUsages || kotlinOptions.isMethodsUsages) {
            if (!processMemberReferences(classOrObject, kotlinOptions, referenceProcessor)) return false
        }

        if (kotlinOptions.isUsages && classOrObject is KtObjectDeclaration && classOrObject.isCompanion() && classOrObject in options.searchScope) {
            if (!processCompanionObjectInternalReferences(classOrObject, referenceProcessor)) {
                return false
            }
        }

        if (kotlinOptions.searchConstructorUsages) {
            val result = runReadAction {
                val constructors = classOrObject.toLightClass()?.constructors ?: PsiMethod.EMPTY_ARRAY
                constructors.filterIsInstance<KtLightMethod>().all { constructor ->
                    constructor.processDelegationCallConstructorUsages(constructor.useScope.intersectWith(options.searchScope)) {
                        it.calleeExpression?.mainReference?.let { referenceProcessor.process(it) } ?: false
                    }
                }
            }
            if (!result) return false
        }

        if (options.isDerivedClasses || options.isDerivedInterfaces) {
            if (!processInheritors()) return false
        }

        return true
    }

    private fun processClassReferences(classOrObject: KtClassOrObject,
                                       options: KotlinClassFindUsagesOptions,
                                       processor: Processor<PsiReference>): Boolean {
        val searchParameters = KotlinReferencesSearchParameters(classOrObject,
                                                                scope = options.searchScope,
                                                                kotlinOptions = KotlinReferencesSearchOptions(acceptCompanionObjectMembers = true))
        var usagesQuery = ReferencesSearch.search(searchParameters)

        if (options.isSkipImportStatements) {
            usagesQuery = FilteredQuery(usagesQuery) { !it.isImportUsage() }
        }

        if (!options.searchConstructorUsages) {
            usagesQuery = FilteredQuery(usagesQuery) { !it.isConstructorUsage(classOrObject) }
        }
        else if (!options.isUsages) {
            usagesQuery = FilteredQuery(usagesQuery) { it.isConstructorUsage(classOrObject) }
        }
        return usagesQuery.forEach(processor)
    }

    private fun processCompanionObjectInternalReferences(companionObject: KtObjectDeclaration,
                                                         processor: Processor<PsiReference>): Boolean {
        var stop: Boolean = false
        runReadAction {
            val klass = companionObject.getStrictParentOfType<KtClass>() ?: return@runReadAction
            val companionObjectDescriptor = companionObject.descriptor
            klass.acceptChildren(object : KtVisitorVoid() {
                override fun visitKtElement(element: KtElement) {
                    if (element == companionObject) return // skip companion object itself
                    if (stop) return
                    element.acceptChildren(this)

                    val bindingContext = element.analyze()
                    val resolvedCall = bindingContext[BindingContext.CALL, element]?.getResolvedCall(bindingContext) ?: return
                    if ((resolvedCall.dispatchReceiver as? ImplicitClassReceiver)?.declarationDescriptor == companionObjectDescriptor
                        || (resolvedCall.extensionReceiver as? ImplicitClassReceiver)?.declarationDescriptor == companionObjectDescriptor) {
                        element.references.forEach {
                            if (!stop && !processor.process(it)) {
                                stop = true
                            }
                        }
                    }
                }
            })
        }
        return !stop
    }

    private fun processMemberReferences(classOrObject: KtClassOrObject,
                                        options: KotlinClassFindUsagesOptions,
                                        processor: Processor<PsiReference>): Boolean {
        for (decl in classOrObject.effectiveDeclarations()) {
            if ((decl is KtNamedFunction && options.isMethodsUsages) ||
                ((decl is KtProperty || decl is KtParameter) && options.isFieldsUsages)) {
                if (!ReferencesSearch.search(decl, options.searchScope).forEach(processor)) return false
            }
        }
        return true
    }

    override fun getStringsToSearch(element: PsiElement): Collection<String> {
        val psiClass = when (element) {
                           is PsiClass -> element
                           is KtClassOrObject -> getElement().toLightClass()
                           else -> null
                       } ?: return Collections.emptyList()

        return getElementNames(psiClass)
    }

    override fun isSearchForTextOccurencesAvailable(psiElement: PsiElement, isSingleFile: Boolean): Boolean {
        return !isSingleFile
    }

    override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions {
        return factory.findClassOptions
    }

    fun getElementNames(element: PsiElement): Set<String> {
        if (element is PsiDirectory) {
            // normalize a directory to a corresponding package
            val aPackage = runReadAction { JavaDirectoryService.getInstance().getPackage(element) }
            return if (aPackage == null) emptySet<String>() else getElementNames(aPackage)
        }

        val result = HashSet<String>()

        runReadAction {
            when (element) {
                is PsiPackage -> {
                    ContainerUtil.addIfNotNull(result, element.qualifiedName)
                }
                is PsiClass -> {
                    val qname = element.qualifiedName
                    if (qname != null) {
                        result.add(qname)
                        val topLevelClass = PsiUtil.getTopLevelClass(element)
                        if (topLevelClass != null) {
                            val topName = topLevelClass.qualifiedName
                            assert(topName != null)
                            if (qname.length > topName!!.length) {
                                result.add(topName + qname.substring(topName.length).replace('.', '$'))
                            }
                        }
                    }
                }
                is PsiMethod -> {
                    ContainerUtil.addIfNotNull(result, element.name)
                }
                is PsiVariable -> {
                    ContainerUtil.addIfNotNull(result, element.name)
                }
                is PsiMetaOwner -> {
                    val metaData = element.metaData
                    if (metaData != null) {
                        ContainerUtil.addIfNotNull(result, metaData.name)
                    }
                }
                is PsiNamedElement -> {
                    ContainerUtil.addIfNotNull(result, element.name)
                }
                is XmlAttributeValue -> {
                    ContainerUtil.addIfNotNull(result, element.value)
                }
                else -> {
                }
            }
        }

        return result
    }
}
