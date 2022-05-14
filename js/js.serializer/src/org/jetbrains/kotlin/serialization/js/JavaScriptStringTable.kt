/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.serialization.StringTableImpl

class JavaScriptStringTable : StringTableImpl() {
    override fun getFqNameIndexOfLocalAnonymousClass(descriptor: ClassifierDescriptorWithTypeParameters): Int {
        return if (descriptor.containingDeclaration is CallableMemberDescriptor) {
            val superClassifiers = descriptor.getAllSuperClassifiers()
                    .mapNotNull { it as ClassifierDescriptorWithTypeParameters }
                    .filter { it != descriptor }
                    .toList()
            if (superClassifiers.size == 1) {
                getFqNameIndex(superClassifiers[0])
            }
            else {
                val superClass = superClassifiers.find { !DescriptorUtils.isInterface(it) }
                getFqNameIndex(superClass ?: descriptor.module.builtIns.any)
            }
        }
        else {
            super.getFqNameIndexOfLocalAnonymousClass(descriptor)
        }
    }
}
