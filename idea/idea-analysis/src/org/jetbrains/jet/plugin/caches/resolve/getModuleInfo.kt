/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.caches.resolve

import com.intellij.psi.PsiElement
import org.jetbrains.jet.asJava.KotlinLightElement
import org.jetbrains.jet.lang.psi.*
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.JdkOrderEntry
import org.jetbrains.jet.asJava.FakeLightClassForFileOfPackage
import org.jetbrains.jet.asJava.KotlinLightClassForPackage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

fun PsiElement.getModuleInfo(): IdeaModuleInfo {
    fun logAndReturnDefault(message: String): IdeaModuleInfo {
        LOG.error("Could not find correct module information.\nReason: $message")
        return NotUnderContentRootModuleInfo
    }

    if (this is KotlinLightElement<*, *>) return this.getModuleInfoForLightElement()

    val containingJetFile = (this as? JetElement)?.getContainingFile() as? JetFile
    val context = containingJetFile?.analysisContext
    if (context != null) return context.getModuleInfo()

    val doNotAnalyze = containingJetFile?.doNotAnalyze
    if (doNotAnalyze != null) {
        return logAndReturnDefault(
                "Should not analyze element: ${getText()} in file ${containingJetFile?.getName() ?: " <no file>"}\n$doNotAnalyze"
        )
    }

    if (containingJetFile is JetCodeFragment) {
        return containingJetFile.getContext()?.getModuleInfo()
               ?: logAndReturnDefault("Analyzing code fragment of type ${containingJetFile.javaClass} with no context element\nText:\n${containingJetFile.getText()}")
    }

    val project = getProject()
    val containingFile = getContainingFile()
            ?: return logAndReturnDefault("Analyzing element of type $javaClass with no containing file\nText:\n${getText()}")

    val virtualFile = containingFile.getOriginalFile().getVirtualFile()
            ?: return logAndReturnDefault("Analyzing non-physical file $containingFile of type ${containingFile.javaClass}")

    return getModuleInfoByVirtualFile(project, virtualFile, (containingFile as? JetFile)?.isCompiled() ?: false)
}

private fun getModuleInfoByVirtualFile(project: Project, virtualFile: VirtualFile, isDecompiledFile: Boolean): IdeaModuleInfo {
    val projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project)

    val module = projectFileIndex.getModuleForFile(virtualFile)
    if (module != null) {
        if (isDecompiledFile) {
            LOG.error("Decompiled file for ${virtualFile.getCanonicalPath()} is in content of $module")
        }
        if (projectFileIndex.isInTestSourceContent(virtualFile)) {
            return module.testSourceInfo()
        }
        else {
            return module.productionSourceInfo()
        }
    }

    val orderEntries = projectFileIndex.getOrderEntriesForFile(virtualFile)

    @entries for (orderEntry in orderEntries) {
        when (orderEntry) {
            is LibraryOrderEntry -> {
                val library = orderEntry.getLibrary() ?: continue @entries
                if (projectFileIndex.isInLibraryClasses(virtualFile) && !isDecompiledFile) {
                    return LibraryInfo(project, library)
                }
                else if (projectFileIndex.isInLibrarySource(virtualFile) || isDecompiledFile) {
                    return LibrarySourceInfo(project, library)
                }
            }
            is JdkOrderEntry -> {
                val sdk = orderEntry.getJdk() ?: continue @entries
                return SdkInfo(project, sdk)
            }
        }
    }
    return NotUnderContentRootModuleInfo
}

private fun KotlinLightElement<*, *>.getModuleInfoForLightElement(): IdeaModuleInfo {
    val element = origin ?: when (this) {
        is FakeLightClassForFileOfPackage -> this.getContainingFile()!!
        is KotlinLightClassForPackage -> this.getFiles().first()
        else -> throw IllegalStateException("Unknown light class without origin is referenced by IDE lazy resolve: $javaClass")
    }
    return element.getModuleInfo()
}