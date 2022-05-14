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

import com.google.protobuf.ByteString
import org.jetbrains.kotlin.builtins.createBuiltInPackageFragmentProvider
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeCapabilitiesDeserializer
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.platform.platformStatic

public object KotlinJavascriptSerializationUtil {
    private val PACKAGE_FILE_EXT = ".kotlin_package"

    platformStatic
    public fun createPackageFragmentProvider(moduleDescriptor: ModuleDescriptor, metadata: ByteArray): PackageFragmentProvider? {
        val gzipInputStream = GZIPInputStream(ByteArrayInputStream(metadata))
        val content = JsProtoBuf.Library.parseFrom(gzipInputStream)
        gzipInputStream.close()

        val contentMap: MutableMap<String, ByteArray> = hashMapOf()
        for (index in content.getEntryCount().indices) {
            val entry = content.getEntry(index)
            contentMap[entry.getPath()] = entry.getContent().toByteArray()
        }

        val packageFqNames = getPackages(contentMap).map { FqName(it) }.toSet()
        if (packageFqNames.isEmpty()) return null

        return createBuiltInPackageFragmentProvider(
                LockBasedStorageManager(), moduleDescriptor, packageFqNames, FlexibleTypeCapabilitiesDeserializer.Dynamic
        ) {
            path ->
            if (!contentMap.containsKey(path)) null else ByteArrayInputStream(contentMap.get(path))
        }
    }

    public fun contentMapToByteArray(contentMap: Map<String, ByteArray>): ByteArray {
        val contentBuilder = JsProtoBuf.Library.newBuilder()
        contentMap forEach {
            val entry = JsProtoBuf.Library.FileEntry.newBuilder().setPath(it.getKey()).setContent(ByteString.copyFrom(it.getValue())).build()
            contentBuilder.addEntry(entry)
        }

        val byteStream = ByteArrayOutputStream()
        val gzipOutputStream = GZIPOutputStream(byteStream)
        contentBuilder.build().writeTo(gzipOutputStream)
        gzipOutputStream.close()

        return byteStream.toByteArray()
    }

    private fun getPackageName(filePath: String): String {
        val lastIndexOfSep = filePath.lastIndexOf('/')
        assert(lastIndexOfSep >= 0, "expected / in $filePath")
        return filePath.substring(0, lastIndexOfSep).replace('/', '.')
    }

    private fun getPackages(contentMap: Map<String, ByteArray>): List<String> =
            contentMap.keySet().filter { it.endsWith(PACKAGE_FILE_EXT) }.map { getPackageName(it) }
}
