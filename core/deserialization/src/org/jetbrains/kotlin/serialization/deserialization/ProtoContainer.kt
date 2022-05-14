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

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.descriptors.PackagePartSource

sealed class ProtoContainer(
        val nameResolver: NameResolver,
        val typeTable: TypeTable
) {
    class Class(
            val classProto: ProtoBuf.Class,
            nameResolver: NameResolver,
            typeTable: TypeTable,
            val outerClassKind: ClassKind?
    ) : ProtoContainer(nameResolver, typeTable) {
        val classId: ClassId = nameResolver.getClassId(classProto.fqName)

        val kind: ProtoBuf.Class.Kind = Flags.CLASS_KIND.get(classProto.flags) ?: ProtoBuf.Class.Kind.CLASS
        val isInner: Boolean = Flags.IS_INNER.get(classProto.flags)

        override fun debugFqName(): FqName = classId.asSingleFqName()
    }

    class Package(
            val fqName: FqName,
            nameResolver: NameResolver,
            typeTable: TypeTable,
            val packagePartSource: PackagePartSource?
    ) : ProtoContainer(nameResolver, typeTable) {
        override fun debugFqName(): FqName = fqName
    }

    abstract fun debugFqName(): FqName
}
