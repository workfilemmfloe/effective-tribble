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

import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.impl.SubpackagesScope
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetCodeFragment
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetImportDirective
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.NoSubpackagesInPackageScope
import org.jetbrains.kotlin.resolve.scopes.ChainedScope
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.utils.sure
import java.util.ArrayList

class LazyFileScope private constructor(
        scopeChain: List<JetScope>,
        private val aliasImportResolver: LazyImportResolver,
        private val allUnderImportResolver: LazyImportResolver,
        containingDeclaration: PackageFragmentDescriptor,
        debugName: String
) : ChainedScope(containingDeclaration, debugName, *scopeChain.toTypedArray()) {

    public fun forceResolveAllImports() {
        aliasImportResolver.forceResolveAllContents()
        allUnderImportResolver.forceResolveAllContents()
    }

    public fun forceResolveImport(importDirective: JetImportDirective) {
        if (importDirective.isAllUnder()) {
            allUnderImportResolver.forceResolveImportDirective(importDirective)
        }
        else {
            aliasImportResolver.forceResolveImportDirective(importDirective)
        }
    }

    companion object Factory {
        public fun create(
                resolveSession: ResolveSession,
                file: JetFile,
                defaultImports: Collection<JetImportDirective>,
                additionalScopes: List<JetScope>,
                traceForImportResolve: BindingTrace,
                traceForDefaultImportResolve: BindingTrace,
                debugName: String
        ): LazyFileScope {
            val imports = if (file is JetCodeFragment)
                file.importsAsImportList()?.getImports() ?: listOf()
            else
                file.getImportDirectives()

            val moduleDescriptor = resolveSession.getModuleDescriptor()
            val packageView = moduleDescriptor.getPackage(file.getPackageFqName())
            val packageFragment = resolveSession.getPackageFragment(file.getPackageFqName())
                    .sure { "Could not find fragment ${file.getPackageFqName()} for file ${file.getName()}" }

            val aliasImportResolver = LazyImportResolver(resolveSession, moduleDescriptor, AliasImportsIndexed(imports), traceForImportResolve)
            val allUnderImportResolver = LazyImportResolver(resolveSession, moduleDescriptor, AllUnderImportsIndexed(imports), traceForImportResolve)
            val defaultAliasImportResolver = LazyImportResolver(resolveSession, moduleDescriptor, AliasImportsIndexed(defaultImports), traceForDefaultImportResolve)
            val defaultAllUnderImportResolver = LazyImportResolver(resolveSession, moduleDescriptor, AllUnderImportsIndexed(defaultImports), traceForDefaultImportResolve)

            val scopeChain = ArrayList<JetScope>()

            scopeChain.add(LazyImportScope(packageFragment, aliasImportResolver, LazyImportScope.FilteringKind.ALL, "Alias imports in $debugName"))

            scopeChain.add(NoSubpackagesInPackageScope(packageView)) //TODO: problems with visibility too
            scopeChain.add(SubpackagesScope(moduleDescriptor, FqName.ROOT))

            scopeChain.add(LazyImportScope(packageFragment, defaultAliasImportResolver, LazyImportScope.FilteringKind.ALL, "Default alias imports in $debugName"))

            scopeChain.add(LazyImportScope(packageFragment, defaultAllUnderImportResolver, LazyImportScope.FilteringKind.VISIBLE_CLASSES, "Default all under imports in $debugName (visible classes)"))
            scopeChain.add(LazyImportScope(packageFragment, allUnderImportResolver, LazyImportScope.FilteringKind.VISIBLE_CLASSES, "All under imports in $debugName (visible classes)"))

            scopeChain.addAll(additionalScopes)

            scopeChain.add(LazyImportScope(packageFragment, defaultAllUnderImportResolver, LazyImportScope.FilteringKind.INVISIBLE_CLASSES, "Default all under imports in $debugName (invisible classes only)"))
            scopeChain.add(LazyImportScope(packageFragment, allUnderImportResolver, LazyImportScope.FilteringKind.INVISIBLE_CLASSES, "All under imports in $debugName (invisible classes only)"))

            return LazyFileScope(scopeChain, aliasImportResolver, allUnderImportResolver, packageFragment, debugName)
        }
    }
}
