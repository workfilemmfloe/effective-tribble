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

package org.jetbrains.kotlin.resolve.lazy

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportsFactory
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.bindingContextUtil.recordScope
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.SubpackagesImportingScope
import org.jetbrains.kotlin.resolve.scopes.utils.memberScopeAsImportingScope
import org.jetbrains.kotlin.resolve.scopes.utils.withParent
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.utils.sure

public open class FileScopeProviderImpl(
        private val topLevelDescriptorProvider: TopLevelDescriptorProvider,
        private val storageManager: StorageManager,
        private val moduleDescriptor: ModuleDescriptor,
        private val qualifiedExpressionResolver: QualifiedExpressionResolver,
        private val bindingTrace: BindingTrace,
        private val ktImportsFactory: KtImportsFactory,
        private val additionalScopes: Iterable<FileScopeProvider.AdditionalScopes>
) : FileScopeProvider {

    private val defaultImports by storageManager.createLazyValue {
        ktImportsFactory.createImportDirectives(moduleDescriptor.defaultImports)
    }

    private class FileData(val scope: LexicalScope, val importResolver: ImportResolver)

    private val cache = storageManager.createMemoizedFunction { file: KtFile -> createScopeChainAndImportResolver(file) }

    override fun getFileResolutionScope(file: KtFile) = cache(file).scope

    override fun getImportResolver(file: KtFile) = cache(file).importResolver

    private fun createScopeChainAndImportResolver(file: KtFile): FileData {
        val debugName = "LazyFileScope for file " + file.getName()
        val tempTrace = TemporaryBindingTrace.create(bindingTrace, "Transient trace for default imports lazy resolve")

        val imports = file.importDirectives

        val packageView = moduleDescriptor.getPackage(file.getPackageFqName())
        val packageFragment = topLevelDescriptorProvider.getPackageFragment(file.getPackageFqName())
                .sure { "Could not find fragment ${file.getPackageFqName()} for file ${file.getName()}" }

        fun createImportResolver(indexedImports: IndexedImports, trace: BindingTrace)
                = LazyImportResolver(storageManager, qualifiedExpressionResolver, moduleDescriptor, indexedImports, trace, packageFragment)

        val aliasImportResolver = createImportResolver(AliasImportsIndexed(imports), bindingTrace)
        val allUnderImportResolver = createImportResolver(AllUnderImportsIndexed(imports), bindingTrace)
        val defaultAliasImportResolver = createImportResolver(AliasImportsIndexed(defaultImports), tempTrace)
        val defaultAllUnderImportResolver = createImportResolver(AllUnderImportsIndexed(defaultImports), tempTrace)

        var scope: ImportingScope

        scope = LazyImportScope(null, defaultAllUnderImportResolver, LazyImportScope.FilteringKind.INVISIBLE_CLASSES,
                "Default all under imports in $debugName (invisible classes only)")

        scope = LazyImportScope(scope, allUnderImportResolver, LazyImportScope.FilteringKind.INVISIBLE_CLASSES,
                "All under imports in $debugName (invisible classes only)")

        for (additionalScope in additionalScopes.flatMap { it.scopes }) {
            assert(additionalScope.parent == null)
            scope = additionalScope.withParent(scope)
        }

        scope = LazyImportScope(scope, defaultAllUnderImportResolver, LazyImportScope.FilteringKind.VISIBLE_CLASSES,
                "Default all under imports in $debugName (visible classes)")

        scope = LazyImportScope(scope, allUnderImportResolver, LazyImportScope.FilteringKind.VISIBLE_CLASSES,
                "All under imports in $debugName (visible classes)")

        scope = LazyImportScope(scope, defaultAliasImportResolver, LazyImportScope.FilteringKind.ALL,
                "Default alias imports in $debugName")

        scope = SubpackagesImportingScope(scope, moduleDescriptor, FqName.ROOT)

        scope = packageView.memberScope.memberScopeAsImportingScope(scope) //TODO: problems with visibility too

        scope = LazyImportScope(scope, aliasImportResolver, LazyImportScope.FilteringKind.ALL, "Alias imports in $debugName")

        val lexicalScope = LexicalScope.empty(scope, packageFragment)

        bindingTrace.recordScope(lexicalScope, file)

        val importResolver = object : ImportResolver {
            override fun forceResolveAllImports() {
                aliasImportResolver.forceResolveAllImports()
                allUnderImportResolver.forceResolveAllImports()
            }

            override fun forceResolveImport(importDirective: KtImportDirective) {
                if (importDirective.isAllUnder) {
                    allUnderImportResolver.forceResolveImport(importDirective)
                }
                else {
                    aliasImportResolver.forceResolveImport(importDirective)
                }
            }
        }

        return FileData(lexicalScope, importResolver)
    }
}
