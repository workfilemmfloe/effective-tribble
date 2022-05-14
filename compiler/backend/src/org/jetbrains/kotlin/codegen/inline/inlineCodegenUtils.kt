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

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClass
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor

public val FunctionDescriptor.sourceFilePath: String
    get() {
        val source = source as PsiSourceElement
        val containingFile = source.psi?.containingFile
        return containingFile?.virtualFile?.canonicalPath!!
    }

public fun FunctionDescriptor.getClassFilePath(cache: IncrementalCache): String {
    val container = containingDeclaration as? DeclarationDescriptorWithSource
    val source = container?.source

    return when (source) {
        is KotlinJvmBinarySourceElement -> {
            assert(this is DeserializedSimpleFunctionDescriptor) { "Expected DeserializedSimpleFunctionDescriptor, got: $this" }
            val kotlinClass = source.binaryClass as VirtualFileKotlinClass
            kotlinClass.file.canonicalPath!!
        }
        else -> {
            val classId = InlineCodegenUtil.getContainerClassId(this)!!
            val className = JvmClassName.byClassId(classId).internalName
            cache.getClassFilePath(className)
        }
    }
}