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

package org.jetbrains.kotlin.android.synthetic.idea

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.find.findUsages.JavaVariableFindUsagesOptions
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleServiceManager
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlAttribute
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import org.jetbrains.android.util.AndroidResourceUtil
import org.jetbrains.kotlin.android.synthetic.isAndroidSyntheticElement
import org.jetbrains.kotlin.android.synthetic.res.SyntheticFileGenerator
import org.jetbrains.kotlin.idea.caches.resolve.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.resolve.getModuleInfo
import org.jetbrains.kotlin.plugin.findUsages.handlers.KotlinFindUsagesHandlerDecorator
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import java.util.ArrayList

class AndroidFindUsageHandlerDecorator : KotlinFindUsagesHandlerDecorator {
    override fun decorateHandler(element: PsiElement, forHighlightUsages: Boolean, delegate: FindUsagesHandler): FindUsagesHandler {
        if (element !is KtNamedDeclaration) return delegate
        if (!isAndroidSyntheticElement(element)) return delegate

        return AndroidFindMemberUsagesHandler(element, delegate)
    }
}

class AndroidFindMemberUsagesHandler(
        private val declaration: KtNamedDeclaration,
        private val delegate: FindUsagesHandler? = null
) : FindUsagesHandler(declaration) {

    override fun getPrimaryElements(): Array<PsiElement> {
        assert(isAndroidSyntheticElement(declaration))

        val property = declaration as KtProperty
        val moduleInfo = declaration.getModuleInfo() as? ModuleSourceInfo ?: return super.getPrimaryElements()
        val parser = ModuleServiceManager.getService(moduleInfo.module, javaClass<SyntheticFileGenerator>())

        val psiElements = parser?.layoutXmlFileManager?.propertyToXmlAttributes(property)
        val valueElements = psiElements?.map { (it as? XmlAttribute)?.getValueElement() as? PsiElement }?.filterNotNull()
        if (valueElements != null && valueElements.isNotEmpty()) return valueElements.toTypedArray()

        return super.getPrimaryElements()
    }

    override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions {
        return delegate?.getFindUsagesOptions(dataContext) ?: super.getFindUsagesOptions(dataContext)
    }

    override fun getSecondaryElements(): Array<PsiElement> {
        assert(isAndroidSyntheticElement(declaration))

        val property = declaration as KtProperty
        val moduleInfo = declaration.getModuleInfo() as? ModuleSourceInfo ?: return super.getPrimaryElements()
        val parser = ModuleServiceManager.getService(moduleInfo.module, javaClass<SyntheticFileGenerator>())

        val psiElements = parser?.layoutXmlFileManager?.propertyToXmlAttributes(property) ?: listOf()

        val res = ArrayList<PsiElement>()
        for (psiElement in psiElements) {
            if (psiElement is XmlAttribute) {
                val fields = AndroidResourceUtil.findIdFields(psiElement)
                for (field in fields) {
                    res.add(field)
                }
                res.add(declaration)
            }
        }

        if (res.isNotEmpty()) return res.toTypedArray()

        return super.getSecondaryElements()
    }

    override fun processElementUsages(element: PsiElement, processor: Processor<UsageInfo>, options: FindUsagesOptions): Boolean {
        assert(isAndroidSyntheticElement(declaration))

        val findUsagesOptions = JavaVariableFindUsagesOptions(runReadAction { element.project })
        findUsagesOptions.isSearchForTextOccurrences = false
        findUsagesOptions.isSkipImportStatements = true
        findUsagesOptions.isUsages = true
        findUsagesOptions.isReadAccess = true
        findUsagesOptions.isWriteAccess = true
        return super.processElementUsages(element, processor, findUsagesOptions)
    }

    // Android extensions plugin has it's own runtime -> different function classes
    private fun runReadAction<T>(action: () -> T): T {
        return ApplicationManager.getApplication().runReadAction<T>(action)
    }
}
