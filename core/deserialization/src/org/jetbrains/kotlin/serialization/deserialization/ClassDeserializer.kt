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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.ClassDataWithSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor

class ClassDeserializer(private val components: DeserializationComponents) {
    private val classes: (ClassKey) -> ClassDescriptor? =
            components.storageManager.createMemoizedFunctionWithNullableValues { key -> createClass(key) }

    // Additional ClassDataWithSource parameter is needed to avoid calling ClassDataFinder#findClassData()
    // if it is already computed at the call site
    fun deserializeClass(classId: ClassId, classDataWithSource: ClassDataWithSource? = null): ClassDescriptor? =
            classes(ClassKey(classId, classDataWithSource))

    private fun createClass(key: ClassKey): ClassDescriptor? {
        val classId = key.classId
        components.fictitiousClassDescriptorFactory.createClass(classId)?.let { return it }
        val (classData, sourceElement) = key.classDataWithSource
                                         ?: components.classDataFinder.findClassData(classId)
                                         ?: return null
        val (nameResolver, classProto) = classData

        val outerContext = if (classId.isNestedClass) {
            val outerClass = deserializeClass(classId.outerClassId) as? DeserializedClassDescriptor ?: return null

            // Find the outer class first and check if he knows anything about the nested class we're looking for
            if (!outerClass.hasNestedClass(classId.shortClassName)) return null

            outerClass.c
        }
        else {
            val fragments = components.packageFragmentProvider.getPackageFragments(classId.packageFqName)
            assert(fragments.size == 1) { "There should be exactly one package: $fragments, class id is $classId" }

            val fragment = fragments.single()
            if (fragment is DeserializedPackageFragment) {
                // Similarly, verify that the containing package has information about this class
                if (!fragment.hasTopLevelClass(classId.shortClassName)) return null
            }

            components.createContext(fragment, nameResolver, TypeTable(classProto.typeTable), containerSource = null)
        }

        return DeserializedClassDescriptor(outerContext, classProto, nameResolver, sourceElement)
    }

    private class ClassKey(val classId: ClassId, val classDataWithSource: ClassDataWithSource?) {
        // classDataWithSource *intentionally* not used in equals() / hashCode()
        override fun equals(other: Any?) = other is ClassKey && classId == other.classId

        override fun hashCode() = classId.hashCode()
    }
}
