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

import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.openapi.project.Project
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewDescriptor
import com.intellij.refactoring.MoveDestination
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.MoveMultipleElementsViewDescriptor
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesUtil
import com.intellij.usageView.UsageViewUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.refactoring.util.NonCodeUsageInfo
import com.intellij.refactoring.move.moveClassesOrPackages.CommonMoveUtil
import com.intellij.util.IncorrectOperationException
import com.intellij.refactoring.util.RefactoringUIUtil
import org.jetbrains.jet.utils.keysToMap
import com.intellij.refactoring.rename.RenameUtil
import com.intellij.refactoring.move.MoveClassesOrPackagesCallback
import org.jetbrains.jet.plugin.refactoring.JetRefactoringBundle
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDirectory
import org.jetbrains.jet.lang.psi.psiUtil.getPackage
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.plugin.refactoring.move.PackageNameInfo
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.plugin.refactoring.createKotlinFile
import org.jetbrains.jet.plugin.refactoring.move.updateInternalReferencesOnPackageNameChange
import org.jetbrains.jet.plugin.codeInsight.addToShorteningWaitSet
import org.jetbrains.jet.plugin.refactoring.move.getFileNameAfterMove
import org.jetbrains.jet.lang.psi.JetNamedDeclaration
import org.jetbrains.jet.asJava.toLightElements
import java.util.HashMap
import org.jetbrains.jet.utils.flatten
import java.util.ArrayList
import java.util.HashSet
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.psi.PsiClass
import com.intellij.refactoring.util.TextOccurrencesUtil
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassHandler
import com.intellij.util.containers.MultiMap
import org.jetbrains.jet.asJava.namedUnwrappedElement
import org.jetbrains.jet.lang.psi.psiUtil.isPrivate
import org.jetbrains.jet.plugin.refactoring.getUsageContext
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.jet.lang.psi.psiUtil.isInsideOf
import org.jetbrains.jet.plugin.codeInsight.JetFileReferencesResolver
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.plugin.codeInsight.DescriptorToDeclarationUtil
import org.jetbrains.jet.lang.psi.JetModifierListOwner
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiModifier
import com.intellij.util.VisibilityUtil
import com.intellij.openapi.util.Ref
import org.jetbrains.jet.plugin.refactoring.getKotlinFqName
import org.jetbrains.jet.plugin.references.JetSimpleNameReference.ShorteningMode

public class MoveKotlinTopLevelDeclarationsOptions(
        val elementsToMove: Collection<JetNamedDeclaration>,
        val moveDestination: MoveDestination,
        val searchInCommentsAndStrings: Boolean = true,
        val searchInNonCode: Boolean = true,
        val moveCallback: MoveCallback? = null
)

public class MoveKotlinTopLevelDeclarationsProcessor(project: Project, val options: MoveKotlinTopLevelDeclarationsOptions) : BaseRefactoringProcessor(project) {
    class object {
        private val LOG: Logger = Logger.getInstance(javaClass<MoveKotlinTopLevelDeclarationsProcessor>())

        private val REFACTORING_NAME: String = JetRefactoringBundle.message("refactoring.move.top.level.declarations")!!
    }

    private var nonCodeUsages: Array<NonCodeUsageInfo>? = null
    private val kotlinToLightElements = options.elementsToMove.keysToMap { it.toLightElements() }
    private val conflicts = MultiMap<PsiElement, String>()

    override fun createUsageViewDescriptor(usages: Array<out UsageInfo>?): UsageViewDescriptor {
        return MoveMultipleElementsViewDescriptor(
                options.elementsToMove.copyToArray(),
                MoveClassesOrPackagesUtil.getPackageName(options.moveDestination.getTargetPackage())
        )
    }

    override fun findUsages(): Array<UsageInfo> {
        val newPackageName = options.moveDestination.getTargetPackage()?.getQualifiedName() ?: ""

        fun collectUsages(): List<UsageInfo> {
            return kotlinToLightElements.values().flatten().flatMap { lightElement ->
                val newFqName = StringUtil.getQualifiedName(newPackageName, lightElement.getName())

                val foundReferences = HashSet<PsiReference>()
                val projectScope = GlobalSearchScope.projectScope(lightElement.getProject())
                val results = ReferencesSearch
                        .search(lightElement, projectScope, false)
                        .mapTo(ArrayList<UsageInfo?>()) { ref ->
                            if (foundReferences.add(ref)) {
                                val range = ref.getRangeInElement()!!
                                MoveRenameUsageInfo(ref.getElement(), ref, range.getStartOffset(), range.getEndOffset(), lightElement, false)
                            }
                            else null
                        }
                        .filterNotNull()

                val name = lightElement.getKotlinFqName()?.asString()
                if (name != null) {
                    TextOccurrencesUtil.findNonCodeUsages(
                            lightElement,
                            name,
                            options.searchInCommentsAndStrings,
                            options.searchInNonCode,
                            newFqName,
                            results
                    )
                }

                MoveClassHandler.EP_NAME.getExtensions().forEach { handler -> handler.preprocessUsages(results) }

                results
            }
        }

        /*
            There must be a conflict if all of the following conditions are satisfied:
                declaration is package-private
                usage does not belong to target package
                usage is not being moved together with declaration
         */

        fun collectConflictsInUsages(usages: List<UsageInfo>) {
            val declarationToContainers = HashMap<JetNamedDeclaration, MutableSet<PsiElement>>()
            for (usage in usages) {
                val element = usage.getElement()
                if (element == null || usage !is MoveRenameUsageInfo || usage is NonCodeUsageInfo) continue

                val declaration = usage.getReferencedElement()?.namedUnwrappedElement as? JetNamedDeclaration
                if (declaration == null || !declaration.isPrivate()) continue

                if (element.isInsideOf(options.elementsToMove)) continue

                val container = element.getUsageContext()
                if (!declarationToContainers.getOrPut(declaration) { HashSet<PsiElement>() }.add(container)) continue

                val currentPackage = element.getContainingFile()?.getContainingDirectory()?.getPackage()
                if (currentPackage?.getQualifiedName() == newPackageName) continue

                conflicts.putValue(
                        declaration,
                        JetRefactoringBundle.message(
                                "package.private.0.will.no.longer.be.accessible.from.1",
                                RefactoringUIUtil.getDescription(declaration, true),
                                RefactoringUIUtil.getDescription(container, true)
                        )
                )
            }
        }

        fun collectConflictsInDeclarations() {
            val declarationToReferenceTargets = HashMap<JetNamedDeclaration, MutableSet<PsiElement>>()
            for (declaration in options.elementsToMove) {
                val referenceToContext = JetFileReferencesResolver.resolve(element = declaration, visitReceivers = false)
                for ((refExpr, bindingContext) in referenceToContext) {
                    val refTarget = bindingContext[BindingContext.REFERENCE_TARGET, refExpr]?.let { descriptor ->
                        DescriptorToDeclarationUtil.getDeclaration(declaration.getProject(), descriptor, bindingContext)
                    }
                    if (refTarget == null || refTarget.isInsideOf(options.elementsToMove)) continue

                    val packagePrivate = when(refTarget) {
                        is JetModifierListOwner ->
                            refTarget.isPrivate()
                        is PsiModifierListOwner ->
                            VisibilityUtil.getVisibilityModifier(refTarget.getModifierList()) == PsiModifier.PACKAGE_LOCAL
                        else -> false
                    }

                    if (!packagePrivate) continue

                    if (!declarationToReferenceTargets.getOrPut(declaration) { HashSet<PsiElement>() }.add(refTarget)) continue

                    val currentPackage = declaration.getContainingFile()?.getContainingDirectory()?.getPackage()
                    if (currentPackage?.getQualifiedName() == newPackageName) continue

                    conflicts.putValue(
                            declaration,
                            JetRefactoringBundle.message(
                                    "0.uses.package.private.1",
                                    RefactoringUIUtil.getDescription(declaration, true),
                                    RefactoringUIUtil.getDescription(refTarget, true)
                            )!!.capitalize()
                    )
                }
            }
        }

        val usages = collectUsages()
        collectConflictsInUsages(usages)
        collectConflictsInDeclarations()
        return UsageViewUtil.removeDuplicatedUsages(usages.copyToArray())
    }

    override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean {
        return showConflicts(conflicts, refUsages.get())
    }

    override fun performRefactoring(usages: Array<out UsageInfo>?) {
        fun moveDeclaration(declaration: JetNamedDeclaration, moveDestination: PsiDirectory): JetNamedDeclaration? {
            val newPackage = moveDestination.getPackage()
            if (newPackage == null) return null

            val file = declaration.getContainingFile() as? JetFile
            if (file == null) return null

            val packageNameInfo = PackageNameInfo(JetPsiUtil.getFQName(file), FqName(newPackage.getQualifiedName()))

            val existingFile = if (moveDestination != file.getContainingDirectory()) moveDestination.findFile(file.getName()) else null
            val newFile = existingFile ?: declaration.getFileNameAfterMove()?.let {
                fileName -> createKotlinFile(fileName, moveDestination)
            }
            assert(newFile != null, "Couldn't create Koltin file for: ${declaration.getClass()}: ${declaration.getText()}")

            declaration.updateInternalReferencesOnPackageNameChange(packageNameInfo, ShorteningMode.NO_SHORTENING)
            val newElement = newFile!!.add(declaration) as JetNamedDeclaration
            declaration.delete()

            newElement.addToShorteningWaitSet()

            return newElement
        }

        try {
            val oldToNewElementsMapping = HashMap<PsiElement, PsiElement>()
            for ((oldDeclaration, oldLightElements) in kotlinToLightElements) {
                val targetDirectory = options.moveDestination.getTargetDirectory(oldDeclaration.getContainingFile())
                if (targetDirectory == null) continue

                val newDeclaration = moveDeclaration(oldDeclaration, targetDirectory)
                if (newDeclaration == null) continue

                getTransaction()!!.getElementListener(oldDeclaration).elementMoved(newDeclaration)
                for ((oldElement, newElement) in oldLightElements.iterator() zip newDeclaration.toLightElements().iterator()) {
                    oldToNewElementsMapping[oldElement] = newElement
                }
            }

            nonCodeUsages = CommonMoveUtil.retargetUsages(usages, oldToNewElementsMapping)
        }
        catch (e: IncorrectOperationException) {
            nonCodeUsages = null
            RefactoringUIUtil.processIncorrectOperation(myProject, e)
        }
    }

    override fun performPsiSpoilingRefactoring() {
        nonCodeUsages?.let { nonCodeUsages -> RenameUtil.renameNonCodeUsages(myProject, nonCodeUsages) }

        options.moveCallback?.let { moveCallback ->
            if (moveCallback is MoveClassesOrPackagesCallback) {
                moveCallback.classesOrPackagesMoved(options.moveDestination)
            }
            moveCallback.refactoringCompleted()
        }
    }

    override fun getCommandName(): String = REFACTORING_NAME
}
