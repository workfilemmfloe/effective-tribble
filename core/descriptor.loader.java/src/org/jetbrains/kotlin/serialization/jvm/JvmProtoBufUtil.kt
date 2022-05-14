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

package org.jetbrains.kotlin.serialization.jvm

import com.google.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.load.kotlin.JvmNameResolver
import org.jetbrains.kotlin.serialization.ClassData
import org.jetbrains.kotlin.serialization.PackageData
import org.jetbrains.kotlin.serialization.ProtoBuf
import java.io.ByteArrayInputStream

public object JvmProtoBufUtil {
    public val EXTENSION_REGISTRY: ExtensionRegistryLite = run {
        val registry = ExtensionRegistryLite.newInstance()
        JvmProtoBuf.registerAllExtensions(registry)
        registry
    }

    @JvmStatic
    public fun readClassDataFrom(data: Array<String>, strings: Array<String>): ClassData =
            readClassDataFrom(BitEncoding.decodeBytes(data), strings)

    @JvmStatic
    public fun readClassDataFrom(bytes: ByteArray, strings: Array<String>): ClassData {
        val input = ByteArrayInputStream(bytes)
        val nameResolver = JvmNameResolver(JvmProtoBuf.StringTableTypes.parseDelimitedFrom(input, EXTENSION_REGISTRY), strings)
        val classProto = ProtoBuf.Class.parseFrom(input, EXTENSION_REGISTRY)
        return ClassData(nameResolver, classProto)
    }

    @JvmStatic
    public fun readPackageDataFrom(data: Array<String>, strings: Array<String>): PackageData =
            readPackageDataFrom(BitEncoding.decodeBytes(data), strings)

    @JvmStatic
    public fun readPackageDataFrom(bytes: ByteArray, strings: Array<String>): PackageData {
        val input = ByteArrayInputStream(bytes)
        val nameResolver = JvmNameResolver(JvmProtoBuf.StringTableTypes.parseDelimitedFrom(input, EXTENSION_REGISTRY), strings)
        val packageProto = ProtoBuf.Package.parseFrom(input, EXTENSION_REGISTRY)
        return PackageData(nameResolver, packageProto)
    }
}
