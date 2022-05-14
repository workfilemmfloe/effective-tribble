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

package org.jetbrains.kotlin.j2k

import com.intellij.core.JavaCoreApplicationEnvironment
import com.intellij.core.JavaCoreProjectEnvironment
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.net.URLClassLoader
import com.intellij.codeInsight.NullableNotNullManager
import com.intellij.psi.PsiModifierListOwner

public object JavaToKotlinTranslator {
    val DISPOSABLE = Disposer.newDisposable()

    private fun createFile(text: String): PsiFile? {
        val javaCoreEnvironment: JavaCoreProjectEnvironment? = setUpJavaCoreEnvironment()
        return PsiFileFactory.getInstance(javaCoreEnvironment?.getProject()!!).createFileFromText("test.java", JavaLanguage.INSTANCE, text)
    }

    fun setUpJavaCoreEnvironment(): JavaCoreProjectEnvironment {
        val applicationEnvironment = JavaCoreApplicationEnvironment(DISPOSABLE)
        val javaCoreEnvironment = JavaCoreProjectEnvironment(DISPOSABLE, applicationEnvironment)

        javaCoreEnvironment.getProject().registerService(javaClass<NullableNotNullManager>(), object : NullableNotNullManager() {
            override fun isNullable(owner: PsiModifierListOwner, checkBases: Boolean) = !isNotNull(owner, checkBases)
            override fun isNotNull(owner: PsiModifierListOwner, checkBases: Boolean) = true
        })

        for (root in PathUtil.getJdkClassesRoots()) {
            javaCoreEnvironment.addJarToClassPath(root)
        }
        val annotations: File? = findAnnotations()
        if (annotations != null && annotations.exists()) {
            javaCoreEnvironment.addJarToClassPath(annotations)
        }
        return javaCoreEnvironment
    }

    fun prettify(code: String?): String {
        if (code == null) {
            return ""
        }

        return code
                .trim()
                .replaceAll("\r\n", "\n")
                .replaceAll(" \n", "\n")
                .replaceAll("\n ", "\n")
                .replaceAll("\n+", "\n")
                .replaceAll(" +", " ")
                .trim()
    }

    fun findAnnotations(): File? {
        var classLoader = javaClass<JavaToKotlinTranslator>().getClassLoader()
        while (classLoader != null) {
            val loader = classLoader
            if (loader is URLClassLoader) {
                for (url in loader.getURLs()) {
                    if ("file" == url.getProtocol() && url.getFile()!!.endsWith("/annotations.jar")) {
                        return File(url.getFile()!!)
                    }
                }
            }
            classLoader = classLoader?.getParent()
        }
        return null
    }

    fun generateKotlinCode(javaCode: String): String {
        val file = createFile(javaCode)
        if (file is PsiJavaFile) {
            val converter = JavaToKotlinConverter(file.getProject(), ConverterSettings.defaultSettings, EmptyReferenceSearcher, EmptyResolverForConverter, null)
            return prettify(converter.elementsToKotlin(listOf(JavaToKotlinConverter.InputElement(file, null))).single()!!.text)
        }
        return ""
    }
}

fun main(args: Array<String>) {
    if (args.size() == 1) {
        try {
            val kotlinCode = JavaToKotlinTranslator.generateKotlinCode(args[0])
            if (kotlinCode.isEmpty()) {
                println("EXCEPTION: generated code is empty.")
            }
            else {
                println(kotlinCode)
            }
        }
        catch (e: Exception) {
            println("EXCEPTION: " + e.getMessage())
        }
    }
    else {
        println("EXCEPTION: wrong number of arguments (should be 1).")
    }
}

//used in Kotlin Web Demo
public fun translateToKotlin(code: String): String {
    return JavaToKotlinTranslator.generateKotlinCode(code)
}
