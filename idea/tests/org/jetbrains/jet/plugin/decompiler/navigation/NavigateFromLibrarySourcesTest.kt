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

package org.jetbrains.jet.plugin.decompiler.navigation

import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.jet.plugin.JdkAndMockLibraryProjectDescriptor
import org.jetbrains.jet.plugin.PluginTestCaseBase
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiClass
import org.jetbrains.jet.lang.psi.JetClass
import kotlin.test.assertEquals
import kotlin.test.fail
import org.jetbrains.kotlin.util.sure
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.jet.lang.psi.JetClassOrObject
import org.jetbrains.jet.asJava.LightClassUtil
import org.jetbrains.jet.asJava.KotlinLightClass
import kotlin.test.assertTrue

public class NavigateFromLibrarySourcesTest: LightCodeInsightFixtureTestCase() {
    public fun testJdkClass() {
        checkNavigationFromLibrarySource("Thread", "java.lang.Thread")
    }

    public fun testOurKotlinClass() {
        checkNavigationFromLibrarySource("Foo", "a.Foo")
    }

    public fun testBuiltinClass() {
        checkNavigationFromLibrarySource("String", "kotlin.String")
    }

    // This test is not exactly for navigation, but separating it to another class doesn't worth it.
    public fun testNoLightClassForLibrarySource() {
        val navigationElement = navigationElementForReferenceInLibrarySource("Foo")
        assertTrue(navigationElement is JetClassOrObject, "Foo should navigate to JetClassOrObject")
        val lightClass = LightClassUtil.getPsiClass(navigationElement as JetClassOrObject)
        assertTrue(lightClass !is KotlinLightClass, "Do not create Kotlin Light Class for file from library sources")
    }

    private fun checkNavigationFromLibrarySource(referenceText: String, targetFqName: String) {
        checkNavigationElement(navigationElementForReferenceInLibrarySource(referenceText), targetFqName)
    }

    private fun navigationElementForReferenceInLibrarySource(referenceText: String): PsiElement {
        val libraryOrderEntry = ModuleRootManager.getInstance(myModule!!).getOrderEntries().first { it is LibraryOrderEntry }
        val libSourcesRoot = libraryOrderEntry.getUrls(OrderRootType.SOURCES)[0]
        val vf = VirtualFileManager.getInstance().findFileByUrl(libSourcesRoot + "/usage.kt")!!
        val psiFile = getPsiManager().findFile(vf)!!
        val indexOf = psiFile.getText()!!.indexOf(referenceText)
        val reference = psiFile.findReferenceAt(indexOf)
        return reference.sure("Couldn't find reference").resolve().sure("Couldn't resolve reference").getNavigationElement()!!
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return JdkAndMockLibraryProjectDescriptor(PluginTestCaseBase.getTestDataPathBase() + "/decompiler/navigation/fromLibSource", true)
    }

    private fun checkNavigationElement(element: PsiElement, expectedFqName: String) {
        when (element) {
            is PsiClass -> {
                assertEquals(expectedFqName, element.getQualifiedName())
            }
            is JetClass -> {
                val name = element.getFqName()
                assert(name != null)
                assertEquals(expectedFqName, name!!.asString())
            }
            else -> {
                fail("Navigation element should be JetClass or PsiClass: " + element.javaClass + ", " + element.getText())
            }
        }
    }

}