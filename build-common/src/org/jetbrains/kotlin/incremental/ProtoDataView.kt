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

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolverImpl
import org.jetbrains.kotlin.serialization.js.JsProtoBuf
import org.jetbrains.kotlin.serialization.js.JsSerializerProtocol

class ProtoDataView {
    private val _classes = hashMapOf<ClassId, ClassProtoData>()
    private val _packages = hashMapOf<FqName, PackagePartProtoData>()

    val classes: Map<ClassId, ClassProtoData>
        get() = _classes
    val packages: Map<FqName, PackagePartProtoData>
        get() = _packages
    val allProto: Sequence<ProtoData>
        get() = _classes.values.asSequence() + _packages.values.asSequence()

    fun addDataFromFragment(bytes: ByteArray) {
        addDataFrom(ProtoBuf.PackageFragment.parseFrom(bytes, JsSerializerProtocol.extensionRegistry))
    }

    fun addDataFrom(proto: ProtoBuf.PackageFragment) {
        val nameResolver = NameResolverImpl(proto.strings, proto.qualifiedNames)

        proto.class_List.forEach {
            val classId = nameResolver.getClassId(it.fqName)
            _classes[classId] = ClassProtoData(it, nameResolver)
        }

        proto.`package`.apply {
            val packageFqName = if (hasExtension(JsProtoBuf.packageFqName)) {
                nameResolver.getPackageFqName(getExtension(JsProtoBuf.packageFqName))
            }
            else FqName.ROOT

            if (packageFqName in _packages) {
                throw IllegalStateException("More than one package part with the same fq-name!")
            }

            _packages[packageFqName] = PackagePartProtoData(this, nameResolver, packageFqName)
        }
    }

    fun addDataFrom(library: JsProtoBuf.Library) {
        library.packageFragmentList.forEach {
            addDataFrom(it)
        }
    }
}