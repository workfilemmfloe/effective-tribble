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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.JetScope

public trait ResolutionFacade {

    public fun analyze(element: JetElement, bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL): BindingContext

    public fun analyzeFullyAndGetResult(elements: Collection<JetElement>): AnalysisResult

    public fun resolveToDescriptor(declaration: JetDeclaration): DeclarationDescriptor

    public fun getFileTopLevelScope(file: JetFile): JetScope

    //TODO: better pass ModuleDescriptor here
    public fun resolveImportReference(file: JetFile, fqName: FqName, isDefaultImport: Boolean = false): Collection<DeclarationDescriptor>

    public fun findModuleDescriptor(element: JetElement): ModuleDescriptor

    public fun <T> get(extension: CacheExtension<T>): T

    companion object {
        //NOTE: idea default API returns module search scope for file under module but not in source or production source (for example, test data )
        // this scope can't be used to search for kotlin declarations in index in order to resolve in that case
        // see com.intellij.psi.impl.file.impl.ResolveScopeManagerImpl.getInherentResolveScope
        public fun getResolveScope(file: JetFile): GlobalSearchScope {
            return when (file.getModuleInfo()) {
                is ModuleSourceInfo -> file.getResolveScope()
                else -> GlobalSearchScope.EMPTY_SCOPE
            }
        }
    }
}
