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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import java.util.*

object CompanionObjectMapping {
    private val classesFqNames = linkedSetOf<FqName>()

    init {
        for (type in PrimitiveType.NUMBER_TYPES) {
            classesFqNames.add(KotlinBuiltIns.getPrimitiveFqName(type))
        }
        classesFqNames.add(KotlinBuiltIns.FQ_NAMES.string.toSafe())
        classesFqNames.add(KotlinBuiltIns.FQ_NAMES._enum.toSafe())
    }

    fun allClassesWithIntrinsicCompanions(): Set<FqName> =
            Collections.unmodifiableSet(classesFqNames)

    fun isMappedIntrinsicCompanionObject(classDescriptor: ClassDescriptor): Boolean {
        if (!DescriptorUtils.isCompanionObject(classDescriptor)) return false
        val fqName = DescriptorUtils.getFqName(classDescriptor.containingDeclaration)
        if (!fqName.isSafe) return false

        return fqName.toSafe() in classesFqNames
    }
}
