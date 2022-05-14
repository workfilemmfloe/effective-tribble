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

package org.jetbrains.kotlin.load.java.components

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.OverrideResolver
import org.jetbrains.kotlin.serialization.deserialization.BinaryVersion
import org.jetbrains.kotlin.serialization.deserialization.ErrorReporter
import org.jetbrains.kotlin.util.slicedMap.Slices
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

public class TraceBasedErrorReporter(private val trace: BindingTrace) : ErrorReporter {

    companion object {
        private val LOG = Logger.getInstance(TraceBasedErrorReporter::class.java)

        @JvmField
        public val ABI_VERSION_ERRORS: WritableSlice<String, AbiVersionErrorData> = Slices.createCollectiveSlice()

        // TODO: MutableList is a workaround for KT-5792 Covariant types in Kotlin translated to wildcard types in Java
        @JvmField
        public val INCOMPLETE_HIERARCHY: WritableSlice<ClassDescriptor, MutableList<String>> = Slices.createCollectiveSlice()
    }

    public data class AbiVersionErrorData(
            public val actualVersion: BinaryVersion,
            public val filePath: String,
            public val classId: ClassId
    )

    override fun reportIncompatibleAbiVersion(classId: ClassId, filePath: String, actualVersion: BinaryVersion) {
        trace.record(ABI_VERSION_ERRORS, filePath, AbiVersionErrorData(actualVersion, filePath, classId))
    }

    override fun reportIncompleteHierarchy(descriptor: ClassDescriptor, unresolvedSuperClasses: List<String>) {
        // TODO: MutableList is a workaround for KT-5792 Covariant types in Kotlin translated to wildcard types in Java
        trace.record(INCOMPLETE_HIERARCHY, descriptor, unresolvedSuperClasses as MutableList)
    }

    override fun reportCannotInferVisibility(descriptor: CallableMemberDescriptor) {
        OverrideResolver.createCannotInferVisibilityReporter(trace).invoke(descriptor)
    }

    override fun reportLoadingError(message: String, exception: Exception?) {
        LOG.error(message, exception)
    }
}
