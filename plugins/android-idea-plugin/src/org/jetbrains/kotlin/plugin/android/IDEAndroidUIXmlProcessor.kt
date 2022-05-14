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

package org.jetbrains.kotlin.plugin.android

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.module.Module
import com.intellij.psi.JavaPsiFacade
import org.jetbrains.kotlin.plugin.android.IDEAndroidResourceManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.plugin.android.AndroidXmlVisitor
import com.intellij.psi.impl.*
import kotlin.properties.*
import org.jetbrains.kotlin.lang.resolve.android.*
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider.Result

class IDEAndroidUIXmlProcessor(val module: Module) : AndroidUIXmlProcessor(module.getProject()) {

    init {
        val scope = module.getModuleWithDependenciesAndLibrariesScope(false)
        supportV4 = JavaPsiFacade.getInstance(module.getProject())
                .findClasses(AndroidConst.SUPPORT_FRAGMENT_FQNAME, scope).isNotEmpty()
    }

    override val resourceManager: IDEAndroidResourceManager = IDEAndroidResourceManager(module)

    private val psiTreeChangePreprocessor by Delegates.lazy {
        module.getProject().getExtensions(PsiTreeChangePreprocessor.EP_NAME).first { it is AndroidPsiTreeChangePreprocessor }
    }

    override val cachedSources: CachedValue<List<AndroidSyntheticFile>> by Delegates.lazy {
        cachedValue {
            Result.create(parse(), psiTreeChangePreprocessor)
        }
    }

    override fun parseLayout(files: List<PsiFile>): List<AndroidResource> {
        val widgets = arrayListOf<AndroidResource>()
        val visitor = AndroidXmlVisitor { id, widgetType, attribute ->
            widgets.add(parseAndroidResource(id, widgetType))
        }

        files.forEach { it.accept(visitor) }
        return removeDuplicates(widgets)
    }

}