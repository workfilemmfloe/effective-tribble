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

package org.jetbrains.kotlin.idea.stubindex.resolve

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.lazy.data.KtClassLikeInfo
import org.jetbrains.kotlin.resolve.lazy.declarations.*
import org.jetbrains.kotlin.storage.StorageManager

class PluginDeclarationProviderFactory(
        private val project: Project,
        private val indexedFilesScope: GlobalSearchScope,
        private val storageManager: StorageManager,
        private val nonIndexedFiles: Collection<KtFile>
) : AbstractDeclarationProviderFactory(storageManager) {
    private val fileBasedDeclarationProviderFactory = FileBasedDeclarationProviderFactory(storageManager, nonIndexedFiles)

    override fun getClassMemberDeclarationProvider(classLikeInfo: KtClassLikeInfo): ClassMemberDeclarationProvider {
        return PsiBasedClassMemberDeclarationProvider(storageManager, classLikeInfo)
    }

    override fun createPackageMemberDeclarationProvider(name: FqName): PackageMemberDeclarationProvider? {
        val fileBasedProvider = fileBasedDeclarationProviderFactory.getPackageMemberDeclarationProvider(name)
        val stubBasedProvider = getStubBasedPackageMemberDeclarationProvider(name)
        return when {
            fileBasedProvider == null && stubBasedProvider == null -> null
            fileBasedProvider == null -> stubBasedProvider
            stubBasedProvider == null -> fileBasedProvider
            else -> CombinedPackageMemberDeclarationProvider(listOf(stubBasedProvider, fileBasedProvider))
        }
    }

    private fun getStubBasedPackageMemberDeclarationProvider(name: FqName): PackageMemberDeclarationProvider? {
        if (!PackageIndexUtil.packageExists(name, indexedFilesScope, project)) return null

        return StubBasedPackageMemberDeclarationProvider(name, project, indexedFilesScope)
    }

    override fun diagnoseMissingPackageFragment(file: KtFile) {
        throw IllegalStateException("Cannot find package fragment for file ${file.name} with package ${file.packageFqName}, " +
                                    "vFile ${file.virtualFile}, nonIndexed ${file in nonIndexedFiles}")
    }

    // trying to diagnose org.jetbrains.kotlin.resolve.lazy.NoDescriptorForDeclarationException in completion
    private val onCreationDebugInfo = debugInfo()

    fun debugToString(): String {
        return "PluginDeclarationProviderFactory\nOn failure:\n${debugInfo()}On creation:\n$onCreationDebugInfo"
    }

    private fun debugInfo(): String {
        if (nonIndexedFiles.isEmpty()) return "-no synthetic files-\n"

        return buildString {
            nonIndexedFiles.forEach {
                append(it.name)
                append(" isPhysical=${it.isPhysical}")
                append(" modStamp=${it.modificationStamp}")
                appendln()
            }
        }
    }
}
