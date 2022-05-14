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

package org.jetbrains.jet.plugin.actions.internal

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import java.util.ArrayList
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.ui.Messages
import javax.swing.SwingUtilities
import org.jetbrains.jet.plugin.caches.resolve.getResolutionFacade
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils
import com.intellij.usages.UsageViewManager
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.usageView.UsageInfo
import com.intellij.usages.UsageTarget
import com.intellij.usages.UsageViewPresentation
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.diagnostic.Logger

public class FindImplicitNothingAction : AnAction() {
    private val LOG = Logger.getInstance("#org.jetbrains.jet.plugin.actions.internal.FindImplicitNothingAction")

    override fun actionPerformed(e: AnActionEvent) {
        val selectedFiles = selectedKotlinFiles(e).toList()
        val project = CommonDataKeys.PROJECT.getData(e.getDataContext())!!

        ProgressManager.getInstance().runProcessWithProgressSynchronously(
                Runnable { find(selectedFiles, project) },
                "Finding Implicit Nothing's",
                true,
                project)
    }

    private fun find(files: Collection<JetFile>, project: Project) {
        val progressIndicator = ProgressManager.getInstance().getProgressIndicator()
        val found = ArrayList<JetCallExpression>()
        for ((i, file) in files.withIndices()) {
            progressIndicator?.setText("Scanning files: $i of ${files.size} file. ${found.size} occurences found")
            progressIndicator?.setText2(file.getVirtualFile().getPath())

            val resolutionFacade = file.getResolutionFacade()
            file.acceptChildren(object : JetVisitorVoid() {
                override fun visitJetElement(element: JetElement) {
                    ProgressManager.checkCanceled()
                    element.acceptChildren(this)
                }

                override fun visitCallExpression(expression: JetCallExpression) {
                    expression.acceptChildren(this)

                    try {
                        val bindingContext = resolutionFacade.analyze(expression)
                        val type = bindingContext[BindingContext.EXPRESSION_TYPE, expression] ?: return
                        if (KotlinBuiltIns.getInstance().isNothing(type) && !expression.hasExplicitNothing(bindingContext)) { //TODO: what about nullable Nothing?
                            found.add(expression)
                        }
                    }
                    catch(e: ProcessCanceledException) {
                        throw e
                    }
                    catch(t: Throwable) { // do not stop on internal error
                        LOG.error(t)
                    }
                }
            })

            progressIndicator?.setFraction((i + 1) / files.size.toDouble())
        }

        SwingUtilities.invokeLater {
            if (found.isNotEmpty()) {
                val usages = found.map { UsageInfo2UsageAdapter(UsageInfo(it)) }.copyToArray()
                val presentation = UsageViewPresentation()
                presentation.setTabName("Implicit Nothing's")
                UsageViewManager.getInstance(project).showUsages(array<UsageTarget>(), usages, presentation)
            }
            else {
                Messages.showInfoMessage(project, "Not found in ${files.size} file(s)", "Not Found")
            }
        }
    }

    private fun JetExpression.hasExplicitNothing(bindingContext: BindingContext): Boolean {
        val callee = getCalleeExpressionIfAny() ?: return false
        when (callee) {
            is JetSimpleNameExpression -> {
                val target = bindingContext[BindingContext.REFERENCE_TARGET, callee] ?: return false
                val callableDescriptor = (target as? CallableDescriptor ?: return false).getOriginal()
                val declaration = DescriptorToSourceUtils.descriptorToDeclaration(callableDescriptor) as? JetCallableDeclaration
                if (declaration != null && declaration.getTypeReference() == null) return false // implicit type
                val type = callableDescriptor.getReturnType() ?: return false
                return type.isNothingOrNothingFunctionType()
            }

            else -> {
                return callee.hasExplicitNothing(bindingContext)
            }
        }
    }

    private fun JetType.isNothingOrNothingFunctionType(): Boolean {
        val builtIns = KotlinBuiltIns.getInstance()
        return when {
            builtIns.isNothing(this) -> true

            builtIns.isExactFunctionOrExtensionFunctionType(this) -> builtIns.getReturnTypeFromFunctionType(this).isNothingOrNothingFunctionType()

            else -> false
        }
    }

    override fun update(e: AnActionEvent) {
        if (!KotlinInternalMode.enabled) {
            e.getPresentation().setVisible(false)
            e.getPresentation().setEnabled(false)
        }
        e.getPresentation().setVisible(true)
        e.getPresentation().setEnabled(selectedKotlinFiles(e).any())
    }

    private fun selectedKotlinFiles(e: AnActionEvent): Stream<JetFile> {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return streamOf()
        val project = CommonDataKeys.PROJECT.getData(e.getDataContext()) ?: return streamOf()
        return allKotlinFiles(virtualFiles, project)
    }

    private fun allKotlinFiles(filesOrDirs: Array<VirtualFile>, project: Project): Stream<JetFile> {
        val manager = PsiManager.getInstance(project)
        return allFiles(filesOrDirs)
                .stream()
                .map { manager.findFile(it) as? JetFile }
                .filterNotNull()
    }

    private fun allFiles(filesOrDirs: Array<VirtualFile>): Collection<VirtualFile> {
        val result = ArrayList<VirtualFile>()
        for (file in filesOrDirs) {
            VfsUtilCore.visitChildrenRecursively(file, object : VirtualFileVisitor<Unit>() {
                override fun visitFile(file: VirtualFile): Boolean {
                    result.add(file)
                    return true
                }
            })
        }
        return result
    }
}
