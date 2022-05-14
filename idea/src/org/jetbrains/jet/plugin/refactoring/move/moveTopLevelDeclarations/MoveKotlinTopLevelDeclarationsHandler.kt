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

package org.jetbrains.jet.plugin.refactoring.move.moveTopLevelDeclarations

import com.intellij.refactoring.move.MoveHandlerDelegate
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.move.MoveCallback
import com.intellij.psi.PsiReference
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetClassOrObject
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.plugin.refactoring.isInJavaSourceRoot
import org.jetbrains.jet.lang.psi.JetObjectDeclaration
import org.jetbrains.jet.lang.psi.JetClass
import com.intellij.psi.PsiPackage
import com.intellij.psi.PsiDirectory
import org.jetbrains.jet.lang.psi.psiUtil.getPackage
import org.jetbrains.jet.plugin.refactoring.move.getFileNameAfterMove
import java.util.HashSet
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesImpl
import org.jetbrains.jet.plugin.refactoring.move.moveTopLevelDeclarations.ui.MoveKotlinTopLevelDeclarationsDialog
import com.intellij.refactoring.JavaRefactoringSettings
import com.intellij.openapi.actionSystem.LangDataKeys
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.JetNamedDeclaration

public class MoveKotlinTopLevelDeclarationsHandler : MoveHandlerDelegate() {
    private fun doMoveWithCheck(
            project: Project, elements: Array<out PsiElement>, targetContainer: PsiElement?, callback: MoveCallback?
    ): Boolean {
        fun checkNameConflicts(): Boolean {
            val fileNames = HashSet<String>()
            for (element in elements) {
                if (element !is JetNamedDeclaration) continue

                val fileName = element.getFileNameAfterMove()
                if (fileName != null && !fileNames.add(fileName)) {
                    val message = RefactoringBundle.getCannotRefactorMessage(
                            RefactoringBundle.message("there.are.going.to.be.multiple.destination.files.with.the.same.name")
                    )
                    CommonRefactoringUtil.showErrorMessage(RefactoringBundle.message("move.title"), message, null, project)
                    return false
                }
            }

            return true
        }

        if (!checkNameConflicts()) return false
        if (!CommonRefactoringUtil.checkReadOnlyStatusRecursively(project, elements.toList(), true)) return false

        [suppress("UNCHECKED_CAST")]
        val elementsToSearch = elements.toList() as List<JetNamedDeclaration>
        val targetPackageName = MoveClassesOrPackagesImpl.getInitialTargetPackageName(targetContainer, elements)
        val targetDirectory = MoveClassesOrPackagesImpl.getInitialTargetDirectory(targetContainer, elements)
        val searchInComments = JavaRefactoringSettings.getInstance()!!.MOVE_SEARCH_IN_COMMENTS
        val searchInText = JavaRefactoringSettings.getInstance()!!.MOVE_SEARCH_FOR_TEXT

        MoveKotlinTopLevelDeclarationsDialog(
                project, elementsToSearch, targetPackageName, targetDirectory, searchInComments, searchInText, callback
        ).show()

        return true
    }

    override fun canMove(elements: Array<out PsiElement>, targetContainer: PsiElement?): Boolean {
        return super.canMove(elements, targetContainer) && elements.all { e ->
            if (e is JetClass || (e is JetObjectDeclaration && !e.isObjectLiteral()) || e is JetNamedFunction || e is JetProperty) {
                val parent = e.getParent()
                parent is JetFile && parent.isInJavaSourceRoot()
            }
            else false
        }
    }

    override fun isValidTarget(psiElement: PsiElement?, sources: Array<out PsiElement>): Boolean {
        return psiElement is PsiPackage || (psiElement is PsiDirectory && psiElement.getPackage() != null)
    }

    override fun doMove(project: Project, elements: Array<out PsiElement>, targetContainer: PsiElement?, callback: MoveCallback?) {
        doMoveWithCheck(project, elements, targetContainer, callback)
    }

    override fun tryToMove(
            element: PsiElement, project: Project, dataContext: DataContext?, reference: PsiReference?, editor: Editor?
    ): Boolean {
        val elementsToMove = array(element)
        val targetContainer = dataContext?.let { dataContext -> LangDataKeys.TARGET_PSI_ELEMENT.getData(dataContext) }

        return canMove(elementsToMove, targetContainer) && doMoveWithCheck(project, elementsToMove, targetContainer, null)
    }
}