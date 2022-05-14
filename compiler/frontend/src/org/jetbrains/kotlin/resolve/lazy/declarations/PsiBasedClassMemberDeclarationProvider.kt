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

import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.resolve.lazy.data.JetClassLikeInfo

public class PsiBasedClassMemberDeclarationProvider(
        storageManager: StorageManager,
        private val classInfo: JetClassLikeInfo)
: AbstractPsiBasedDeclarationProvider(storageManager), ClassMemberDeclarationProvider {

    override fun getOwnerInfo() = classInfo

    override fun doCreateIndex(index: AbstractPsiBasedDeclarationProvider.Index) {
        for (declaration in classInfo.getDeclarations()) {
            if (declaration !is JetClassObject) { // Do nothing for class object because it will be taken directly from the classInfo
                index.putToIndex(declaration)
            }
        }

        for (parameter in classInfo.getPrimaryConstructorParameters()) {
            if (parameter.hasValOrVarNode()) {
                index.putToIndex(parameter)
            }
        }
    }

    override fun toString() = "Declarations for $classInfo"
}
