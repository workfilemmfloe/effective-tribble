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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.JavaRefactoringSettings
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.rename.naming.AutomaticRenamer
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelFunctionFqnNameIndex
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

public class AutomaticOverloadsRenamer(function: KtNamedFunction, newName: String) : AutomaticRenamer() {
    init {
        myElements.addAll(function.getOverloads().filter { it != function })
        suggestAllNames(function.name, newName)
    }

    override fun getDialogTitle() = "Rename Overloads"
    override fun getDialogDescription() = "Rename overloads to:"
    override fun entityName() = "Overload"
    override fun isSelectedByDefault(): Boolean = true
}

private fun KtNamedFunction.getOverloads(): Collection<KtNamedFunction> {
    val parent = parent
    when (parent) {
        is KtFile -> {
            val module = ModuleUtilCore.findModuleForPsiElement(this)
            if (module != null) {
                val searchScope = GlobalSearchScope.moduleScope(module)
                val fqName = fqName
                if (fqName != null) {
                    return KotlinTopLevelFunctionFqnNameIndex.getInstance().get(fqName.asString(), project, searchScope)
                }
            }
        }
        is KtClassBody -> {
            return parent.declarations.filterIsInstance<KtNamedFunction>().filter { it.name == this.name }
        }
    }
    return emptyList()
}

public class AutomaticOverloadsRenamerFactory : AutomaticRenamerFactory {
    override fun isApplicable(element: PsiElement): Boolean {
        return element is KtNamedFunction && element.name != null
               && (element.parent is KtFile || element.parent is KtClassBody)
    }

    override fun getOptionName() = RefactoringBundle.message("rename.overloads")

    override fun isEnabled() = JavaRefactoringSettings.getInstance().isRenameOverloads

    override fun setEnabled(enabled: Boolean) {
        JavaRefactoringSettings.getInstance().isRenameOverloads = enabled
    }

    override fun createRenamer(element: PsiElement, newName: String, usages: Collection<UsageInfo>)
            = AutomaticOverloadsRenamer(element as KtNamedFunction, newName)
}