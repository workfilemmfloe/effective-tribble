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

@file:JvmName("FileClasses")
package org.jetbrains.kotlin.fileClasses

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.Type

public interface JvmFileClassesProvider {
    public fun getFileClassInfo(file: JetFile): JvmFileClassInfo
}

public fun FqName.getInternalName(): String =
        JvmClassName.byFqNameWithoutInnerClasses(this).internalName

public fun FqName.getClassType(): Type =
        Type.getObjectType(getInternalName())

public fun JvmFileClassesProvider.getFileClassFqName(file: JetFile): FqName =
        getFileClassInfo(file).fileClassFqName

public fun JvmFileClassesProvider.getFileClassInternalName(file: JetFile): String =
        getFileClassFqName(file).getInternalName()

public fun JvmFileClassesProvider.getFileClassType(file: JetFile): Type =
        getFileClassFqName(file).getClassType()

public fun JvmFileClassesProvider.getFacadeClassFqName(file: JetFile): FqName =
        getFileClassInfo(file).facadeClassFqName

public fun JvmFileClassesProvider.getFacadeClassInternalName(file: JetFile): String =
        getFacadeClassFqName(file).getInternalName()

public fun JvmFileClassesProvider.getFacadeClassType(file: JetFile): Type =
        getFacadeClassFqName(file).getClassType()