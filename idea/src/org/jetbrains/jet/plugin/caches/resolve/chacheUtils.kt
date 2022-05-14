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

import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiModificationTracker
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.SLRUCache
import com.intellij.psi.util.CachedValueProvider

class SynchronizedCachedValue<V>(project: Project, provider: () -> CachedValueProvider.Result<V>, trackValue: Boolean = true) {
    private val cachedValue = CachedValuesManager.getManager(project).createCachedValue(
            provider,
            trackValue
    )

    public fun getValue(): V {
        return synchronized(cachedValue) {
            cachedValue.getValue() as V
        }
    }
}