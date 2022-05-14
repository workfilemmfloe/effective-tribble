/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.inspections.gradle

import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.plugins.gradle.codeInspection.GradleBaseInspection
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.groovy.codeInspection.BaseInspectionVisitor
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrCallExpression

class DifferentStdlibGradleVersionInspection : GradleBaseInspection() {
    override fun buildVisitor(): BaseInspectionVisitor = MyVisitor()

    override fun buildErrorString(vararg args: Any) =
            "Plugin version (${args[0]}) is not the same as library version (${args[1]})"

    private inner class MyVisitor : KotlinGradleInspectionVisitor() {
        override fun visitClosure(closure: GrClosableBlock) {
            super.visitClosure(closure)

            val dependenciesCall = closure.getStrictParentOfType<GrMethodCall>() ?: return
            if (dependenciesCall.invokedExpression.text != "dependencies") return

            if (dependenciesCall.parent !is PsiFile) return

            val stdlibStatement = findLibraryStatement(closure, "org.jetbrains.kotlin", "kotlin-stdlib") ?: return
            val stdlibVersion = getResolvedKotlinStdlibVersion(closure.containingFile, "org.jetbrains.kotlin:kotlin-stdlib:") ?: return

            val gradlePluginVersion = getResolvedKotlinGradleVersion(closure.containingFile)

            if (stdlibVersion != gradlePluginVersion) {
                registerError(stdlibStatement, gradlePluginVersion, stdlibVersion)
            }
        }
    }

    companion object {
        val COMPILE_DEPENDENCY_STATEMENTS = listOf("classpath", "compile")

        private fun findLibraryStatement(closure: GrClosableBlock, libraryGroup: String, libraryId: String): GrCallExpression? {
            val applicationStatements = closure.getChildrenOfType<GrCallExpression>()

            for (statement in applicationStatements) {
                val startExpression = statement.getChildrenOfType<GrReferenceExpression>().firstOrNull() ?: continue
                if (startExpression.text in COMPILE_DEPENDENCY_STATEMENTS) {
                    if (statement.text.contains(libraryId) && statement.text.contains(libraryGroup)) {
                        return statement
                    }
                }
            }

            return null
        }

        private fun getResolvedKotlinStdlibVersion(file: PsiFile, libraryNameMarker: String): String? {
            val projectStructureNode = findGradleProjectStructure(file) ?: return null
            val module = ProjectRootManager.getInstance(file.project).fileIndex.getModuleForFile(file.virtualFile) ?: return null

            for (moduleData in projectStructureNode.findAll(ProjectKeys.MODULE).filter { it.data.internalName == module.name }) {
                for (sourceSetData in moduleData.node.findAll(GradleSourceSetData.KEY).filter { it.data.internalName.endsWith("main") }) {
                    for (libraryDependencyData in sourceSetData.node.findAll(ProjectKeys.LIBRARY_DEPENDENCY)) {
                        if (libraryDependencyData.data.externalName.startsWith(libraryNameMarker)) {
                            return libraryDependencyData.data.externalName.substringAfter(libraryNameMarker)
                        }
                    }
                }
            }

            return null
        }
    }
}