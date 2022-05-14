/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.jvm.modules

import org.jetbrains.kotlin.storage.LockBasedStorageManager

class JavaModuleGraph(getModuleInfo: (String) -> JavaModuleInfo) {
    private val moduleInfo: (String) -> JavaModuleInfo = LockBasedStorageManager.NO_LOCKS.createMemoizedFunction(getModuleInfo)

    fun getAllReachable(rootModules: List<String>): List<String> {
        val visited = linkedSetOf<String>()

        fun dfs(module: String) {
            if (!visited.add(module)) return
            for (dependency in moduleInfo(module).requires) {
                if (dependency.isTransitive) {
                    dfs(dependency.moduleName)
                }
            }
        }

        rootModules.forEach(::dfs)
        return visited.toList()
    }
}
