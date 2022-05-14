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

package org.jetbrains.jet.plugin.hierarchy.overrides

import com.intellij.ide.hierarchy.MethodHierarchyBrowserBase
import com.intellij.psi.PsiElement
import javax.swing.JPanel
import com.intellij.ide.hierarchy.HierarchyTreeStructure
import com.intellij.openapi.project.Project
import org.jetbrains.jet.plugin.hierarchy.HierarchyUtils
import org.jetbrains.jet.plugin.JetBundle
import com.intellij.ide.hierarchy.method.MethodHierarchyBrowser
import org.jetbrains.jet.asJava.getRepresentativeLightMethod
import com.intellij.psi.PsiMethod
import java.text.MessageFormat
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.refactoring.util.RefactoringDescriptionLocation
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.asJava.unwrapped

class KotlinOverrideHierarchyBrowser(
        project: Project, baseElement: PsiElement
) : MethodHierarchyBrowser(project, baseElement.getRepresentativeLightMethod()) {
    override fun createLegendPanel(): JPanel? =
            MethodHierarchyBrowserBase.createStandardLegendPanel(
                    JetBundle.message("hierarchy.legend.member.is.defined.in.class"),
                    JetBundle.message("hierarchy.legend.member.defined.in.superclass"),
                    JetBundle.message("hierarchy.legend.member.should.be.defined")
            )

    override fun isApplicableElement(psiElement: PsiElement): Boolean =
            HierarchyUtils.IS_OVERRIDE_HIERARCHY_ELEMENT(psiElement)

    [suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")]
    override fun createHierarchyTreeStructure(typeName: String, psiElement: PsiElement): HierarchyTreeStructure? =
            if (typeName == MethodHierarchyBrowserBase.METHOD_TYPE) KotlinOverrideTreeStructure(myProject, psiElement) else null

    override fun getBaseMethod(): PsiMethod? {
        val builder = myBuilders.get(myCurrentViewType)
        if (builder == null) return null

        return (builder.getTreeStructure() as KotlinOverrideTreeStructure).javaTreeStructures.get(0).getBaseMethod()
    }

    override fun getContentDisplayName(typeName: String, element: PsiElement): String? {
        val targetElement = element.unwrapped
        if (targetElement is JetDeclaration) {
            return ElementDescriptionUtil.getElementDescription(targetElement, RefactoringDescriptionLocation.WITHOUT_PARENT)
        }
        return super.getContentDisplayName(typeName, element)
    }
}
