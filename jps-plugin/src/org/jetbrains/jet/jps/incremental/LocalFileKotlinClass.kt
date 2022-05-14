/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.jps.incremental

import org.jetbrains.jet.lang.resolve.kotlin.FileBasedKotlinClass
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader
import org.jetbrains.jet.lang.resolve.name.ClassId
import java.io.File

class LocalFileKotlinClass private(
        private val file: File,
        private val fileContents: ByteArray,
        className: ClassId,
        classHeader: KotlinClassHeader,
        innerClasses: FileBasedKotlinClass.InnerClassesInfo
) : FileBasedKotlinClass(className, classHeader, innerClasses) {

    class object {
        fun create(file: File): LocalFileKotlinClass? {
            val fileContents = file.readBytes()
            return FileBasedKotlinClass.create(fileContents) {
                className, classHeader, innerClasses ->
                LocalFileKotlinClass(file, fileContents, className, classHeader, innerClasses)
            }
        }
    }

    public override fun getFileContents(): ByteArray = fileContents

    override fun hashCode(): Int = file.hashCode()
    override fun equals(other: Any?): Boolean = other is LocalFileKotlinClass && file == other.file
    override fun toString(): String = "$javaClass: $file"
}
