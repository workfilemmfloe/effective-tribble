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

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.PackageFragmentProviderImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.storage.StorageManager
import java.io.InputStream

public fun createBuiltInPackageFragmentProvider(
        storageManager: StorageManager,
        module: ModuleDescriptor,
        packageFqNames: Set<FqName>,
        classDescriptorFactory: ClassDescriptorFactory,
        loadResource: (String) -> InputStream?
): PackageFragmentProvider {
    val packageFragments = packageFqNames.map { fqName ->
        BuiltinsPackageFragment(fqName, storageManager, module, loadResource)
    }
    val provider = PackageFragmentProviderImpl(packageFragments)

    val localClassResolver = LocalClassResolverImpl()

    val components = DeserializationComponents(
            storageManager,
            module,
            ResourceLoadingClassDataFinder(provider, BuiltInsSerializedResourcePaths, loadResource),
            BuiltInsAnnotationAndConstantLoader(module),
            provider,
            localClassResolver,
            ErrorReporter.DO_NOTHING,
            FlexibleTypeCapabilitiesDeserializer.ThrowException,
            classDescriptorFactory
    )

    localClassResolver.setDeserializationComponents(components)

    for (packageFragment in packageFragments) {
        packageFragment.setDeserializationComponents(components)
    }

    return provider
}
