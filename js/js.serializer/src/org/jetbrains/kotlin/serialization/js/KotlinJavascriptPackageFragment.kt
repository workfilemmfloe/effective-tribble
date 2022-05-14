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

package org.jetbrains.kotlin.serialization.js

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.DeserializedPackageFragment
import org.jetbrains.kotlin.serialization.deserialization.NameResolverImpl
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.kotlin.storage.StorageManager
import java.io.InputStream

class KotlinJavascriptPackageFragment(
        fqName: FqName,
        storageManager: StorageManager,
        module: ModuleDescriptor,
        loadResource: (path: String) -> InputStream?
) : DeserializedPackageFragment(fqName, storageManager, module, loadResource) {
    private val nameResolver =
            NameResolverImpl.read(loadResourceSure(KotlinJavascriptSerializedResourcePaths.getStringTableFilePath(fqName)))

    override val classDataFinder = KotlinJavascriptClassDataFinder(nameResolver, loadResource)

    override fun computeMemberScope(): DeserializedPackageMemberScope {
        val packageStream = loadResourceSure(KotlinJavascriptSerializedResourcePaths.getPackageFilePath(fqName))
        val packageProto = ProtoBuf.Package.parseFrom(packageStream, JsSerializerProtocol.extensionRegistry)
        return DeserializedPackageMemberScope(
                this, packageProto, nameResolver, packagePartSource = null, components = components, classNames = { loadClassNames() }
        )
    }

    private fun loadClassNames(): Collection<Name> {
        val classesStream = loadResourceSure(KotlinJavascriptSerializedResourcePaths.getClassesInPackageFilePath(fqName))
        val classesProto = JsProtoBuf.Classes.parseFrom(classesStream, JsSerializerProtocol.extensionRegistry)
        return classesProto.classNameList?.map { id -> nameResolver.getName(id) } ?: listOf()
    }
}
