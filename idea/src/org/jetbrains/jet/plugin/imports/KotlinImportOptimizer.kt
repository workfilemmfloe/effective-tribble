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

package org.jetbrains.jet.plugin.imports

import com.intellij.lang.ImportOptimizer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.resolve.ImportPath
import org.jetbrains.jet.lang.resolve.name.*
import org.jetbrains.jet.plugin.references.JetReference
import java.util.HashSet
import org.jetbrains.jet.plugin.quickfix.ImportInsertHelper.needImport
import java.util.ArrayList

public class KotlinImportOptimizer() : ImportOptimizer {

    override fun supports(file: PsiFile?) = file is JetFile

    override fun processFile(file: PsiFile?) = Runnable() {
        val jetFile = file as JetFile
        val usedQualifiedNames = extractUsedQualifiedNames(jetFile)

        val directives = jetFile.getImportDirectives()

        val directivesBeforeCurrent = ArrayList<JetImportDirective>()
        val directivesAfterCurrent = ArrayList(jetFile.getImportDirectives())

        ApplicationManager.getApplication()!!.runWriteAction(Runnable {
            // Remove only unnecessary imports
            for (anImport in directives) {
                directivesAfterCurrent.remove(anImport)

                val importPath = anImport.getImportPath()
                if (importPath == null) {
                    continue
                }

                if (isUseful(importPath, usedQualifiedNames)
                    && needImport(importPath, jetFile, directivesBeforeCurrent)
                    && needImport(importPath, jetFile, directivesAfterCurrent)
                ) {
                    directivesBeforeCurrent.add(anImport)
                }
                else {
                    anImport.delete()
                }
            }
        })
    }

    private fun isUseful(importPath: ImportPath, usedNames: Collection<FqName>): Boolean {
        // TODO: Add better analysis for aliases
        return importPath.hasAlias() || usedNames.any { it.isImported(importPath) }
    }

    private fun extractUsedQualifiedNames(jetFile: JetFile): Set<FqName> {
        val usedQualifiedNames = HashSet<FqName>()
        jetFile.accept(object : JetVisitorVoid() {
            override fun visitElement(element: PsiElement) {
                ProgressIndicatorProvider.checkCanceled()
                element?.acceptChildren(this)
            }

            override fun visitJetElement(element: JetElement) {
                if (PsiTreeUtil.getParentOfType(element, javaClass<JetImportDirective>()) != null ||
                    PsiTreeUtil.getParentOfType(element, javaClass<JetPackageDirective>()) != null) {
                    return
                }
                val reference = element.getReference()
                if (reference is JetReference) {
                    val referencedDescriptors = reference.resolveToDescriptors()
                    val importableDescriptors = referencedDescriptors.filter {
                        it.canBeReferencedViaImport() && !isInReceiverScope(element, it)
                    }
                    usedQualifiedNames.addAll(importableDescriptors.map { it.importableFqName }.filterNotNull())
                }
                super.visitJetElement(element)
            }
        })

        return usedQualifiedNames
    }
}