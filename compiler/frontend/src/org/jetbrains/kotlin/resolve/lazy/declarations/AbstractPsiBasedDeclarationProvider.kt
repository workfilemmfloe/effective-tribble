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

package org.jetbrains.kotlin.resolve.lazy.declarations

import com.google.common.collect.ArrayListMultimap
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.lazy.ResolveSessionUtils
import org.jetbrains.kotlin.resolve.lazy.data.JetClassInfoUtil
import org.jetbrains.kotlin.resolve.lazy.data.JetClassLikeInfo
import org.jetbrains.kotlin.resolve.lazy.data.JetScriptInfo
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.resolve.lazy.ResolveSessionUtils.safeNameForLazyResolve
import java.util.ArrayList
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

public abstract class AbstractPsiBasedDeclarationProvider(storageManager: StorageManager) : DeclarationProvider {

    protected class Index {
        // This mutable state is only modified under inside the computable
        val allDeclarations = ArrayList<KtDeclaration>()
        val functions = ArrayListMultimap.create<Name, KtNamedFunction>()
        val properties = ArrayListMultimap.create<Name, KtProperty>()
        val classesAndObjects = ArrayListMultimap.create<Name, JetClassLikeInfo>() // order matters here

        public fun putToIndex(declaration: KtDeclaration) {
            if (declaration is KtClassInitializer || declaration is KtSecondaryConstructor) return

            allDeclarations.add(declaration)
            if (declaration is KtNamedFunction) {
                functions.put(safeNameForLazyResolve(declaration), declaration)
            }
            else if (declaration is KtProperty) {
                properties.put(safeNameForLazyResolve(declaration), declaration)
            }
            else if (declaration is KtClassOrObject) {
                classesAndObjects.put(safeNameForLazyResolve(declaration.getNameAsName()), JetClassInfoUtil.createClassLikeInfo(declaration))
            }
            else if (declaration is KtScript) {
                val scriptInfo = JetScriptInfo(declaration)
                classesAndObjects.put(scriptInfo.fqName.shortName(), scriptInfo)
            }
            else if (declaration is KtParameter || declaration is KtTypedef || declaration is KtMultiDeclaration) {
                // Do nothing, just put it into allDeclarations is enough
            }
            else {
                throw IllegalArgumentException("Unknown declaration: " + declaration)
            }
        }
    }

    private val index = storageManager.createLazyValue<Index> {
        val index = Index()
        doCreateIndex(index)
        index
    }

    protected abstract fun doCreateIndex(index: Index)

    override fun getDeclarations(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): List<KtDeclaration>
            = index().allDeclarations

    override fun getFunctionDeclarations(name: Name): List<KtNamedFunction>
            = index().functions[ResolveSessionUtils.safeNameForLazyResolve(name)].toList()

    override fun getPropertyDeclarations(name: Name): List<KtProperty>
            = index().properties[ResolveSessionUtils.safeNameForLazyResolve(name)].toList()

    override fun getClassOrObjectDeclarations(name: Name): Collection<JetClassLikeInfo>
            = index().classesAndObjects[ResolveSessionUtils.safeNameForLazyResolve(name)]
}
