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

package org.jetbrains.jet.lang.resolve.kotlin

import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.util.sure
import org.jetbrains.jet.lang.resolve.name.ClassId

public abstract class VirtualFileKotlinClassFinder : VirtualFileFinder {
    override fun findKotlinClass(classId: ClassId): KotlinJvmBinaryClass? {
        val file = findVirtualFileWithHeader(classId.asSingleFqName().toSafe()) ?: return null
        return KotlinBinaryClassCache.getKotlinBinaryClass(file)
    }

    override fun findKotlinClass(javaClass: JavaClass): KotlinJvmBinaryClass? {
        var file = (javaClass as JavaClassImpl).getPsi().getContainingFile()!!.getVirtualFile() ?: return null
        if (javaClass.getOuterClass() != null) {
            // For nested classes we get a file of the containing class, to get the actual class file for A.B.C,
            // we take the file for A, take its parent directory, then in this directory we look for A$B$C.class
            file = file.getParent()!!.findChild(classFileName(javaClass) + ".class").sure("Virtual file not found for $javaClass")
        }

        return KotlinBinaryClassCache.getKotlinBinaryClass(file)
    }

    private fun classFileName(jClass: JavaClass): String {
        val outerClass = jClass.getOuterClass()
        if (outerClass == null) {
            return jClass.getName().asString()
        }

        return classFileName(outerClass) + "$" + jClass.getName().asString()
    }
}
