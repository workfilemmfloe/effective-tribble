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

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.ClassData
import org.jetbrains.kotlin.serialization.ClassDataWithSource
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.SerializedResourcePaths
import java.io.InputStream

class ResourceLoadingClassDataFinder(
        private val packageFragmentProvider: PackageFragmentProvider,
        private val serializedResourcePaths: SerializedResourcePaths,
        private val loadResource: (path: String) -> InputStream?
) : ClassDataFinder {
    override fun findClassData(classId: ClassId): ClassDataWithSource? {
        val packageFragment = packageFragmentProvider.getPackageFragments(classId.packageFqName).singleOrNull()
                                      as? DeserializedPackageFragment ?: return null

        val classProto = packageFragment.classIdToProto?.get(classId) ?: run {
            val stream = loadResource(serializedResourcePaths.getClassMetadataPath(classId)) ?: return null
            ProtoBuf.Class.parseFrom(stream, serializedResourcePaths.extensionRegistry)
        }

        return ClassDataWithSource(ClassData(packageFragment.nameResolver, classProto))
    }
}
