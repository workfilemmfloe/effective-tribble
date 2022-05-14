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

package org.jetbrains.jet.plugin.search.ideaExtensions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.DirectClassInheritorsSearch
import com.intellij.util.Processor
import org.jetbrains.jet.lang.psi.JetClassOrObject
import org.jetbrains.jet.plugin.libraries.JetSourceNavigationHelper
import org.jetbrains.jet.plugin.stubindex.JetSuperClassIndex
import org.jetbrains.jet.lang.resolve.*
import java.util.Collections

public open class KotlinDirectInheritorsSearcher() : QueryExecutorBase<PsiClass, DirectClassInheritorsSearch.SearchParameters>(true) {
    public override fun processQuery(queryParameters: DirectClassInheritorsSearch.SearchParameters, consumer: Processor<PsiClass>) {
        val baseClass = queryParameters.getClassToProcess()
        if (baseClass == null) return

        val name = baseClass.getName()
        if (name == null) return

        val originalScope = queryParameters.getScope()
        val scope = if (originalScope is GlobalSearchScope)
            originalScope
        else
            baseClass.getContainingFile()?.let { file -> GlobalSearchScope.fileScope(file) }

        if (scope == null) return

        ApplicationManager.getApplication()?.runReadAction {
            JetSuperClassIndex.getInstance().get(name, baseClass.getProject(), scope).iterator()
                    .map { candidate -> JetSourceNavigationHelper.getOriginalPsiClassOrCreateLightClass(candidate)}
                    .filterNotNull()
                    .filter { candidate -> candidate.isInheritor(baseClass, false) }
                    .forEach { candidate -> consumer.process(candidate) }
        }
    }
}
