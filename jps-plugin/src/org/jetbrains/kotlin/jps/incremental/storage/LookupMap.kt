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

package org.jetbrains.kotlin.jps.incremental.storage

import java.io.File

internal class LookupMap(storage: File) : BasicMap<LookupSymbolKey, Set<Int>>(storage, LookupSymbolKeyDescriptor, IntSetExternalizer) {
    override fun dumpKey(key: LookupSymbolKey): String = key.toString()

    override fun dumpValue(value: Set<Int>): String = value.toString()

    public fun add(name: String, scope: String, fileId: Int) {
        storage.append(LookupSymbolKey(name, scope)) { out -> out.writeInt(fileId) }
    }

    public operator fun get(key: LookupSymbolKey): Set<Int>? = storage[key]

    public operator fun set(key: LookupSymbolKey, fileIds: Set<Int>) {
        storage[key] = fileIds
    }

    public fun remove(key: LookupSymbolKey) {
        storage.remove(key)
    }

    public val keys: Collection<LookupSymbolKey>
        get() = storage.keys
}
