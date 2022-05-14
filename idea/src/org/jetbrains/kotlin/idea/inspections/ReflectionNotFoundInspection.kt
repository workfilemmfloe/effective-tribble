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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinInProjectUtils
import org.jetbrains.kotlin.idea.configuration.KotlinJavaModuleConfigurator
import org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator
import org.jetbrains.kotlin.idea.framework.JavaRuntimePresentationProvider
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.versions.KotlinRuntimeLibraryUtil
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.JetDoubleColonExpression
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetVisitorVoid
import org.jetbrains.kotlin.serialization.deserialization.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.singletonOrEmptyList
import java.io.File

public class ReflectionNotFoundInspection : AbstractKotlinInspection() {
    override fun runForWholeFile() = true

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val file = holder.getFile()
        val reportProblem =
                file is JetFile &&
                ProjectRootsUtil.isInProjectSource(file) &&
                file.findModuleDescriptor().findClassAcrossModuleDependencies(JvmAbi.REFLECTION_FACTORY_IMPL) == null

        return object : JetVisitorVoid() {
            private fun createQuickFix(): LocalQuickFix? {
                val pluginReflectJar = PathUtil.getKotlinPathsForIdeaPlugin().getReflectPath()
                if (pluginReflectJar.exists()) {
                    val configurator =
                            Extensions.getExtensions(KotlinProjectConfigurator.EP_NAME).firstIsInstanceOrNull<KotlinJavaModuleConfigurator>()

                    if (configurator != null) {
                        return AddReflectJarQuickFix(configurator, pluginReflectJar)
                    }
                }

                return null
            }

            override fun visitDoubleColonExpression(expression: JetDoubleColonExpression) {
                if (reportProblem) {
                    holder.registerProblem(
                            expression.getDoubleColonTokenReference(),
                            JetBundle.message("reflection.not.found"),
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                            *(createQuickFix().singletonOrEmptyList().copyToArray())
                    )
                }
            }
        }
    }

    class AddReflectJarQuickFix(
            val configurator: KotlinJavaModuleConfigurator,
            val pluginReflectJar: File
    ) : LocalQuickFix {
        override fun getName() = JetBundle.message("add.reflection.to.classpath")

        override fun getFamilyName() = getName()

        override fun applyFix(project: Project, descriptor: ProblemDescriptor?) {
            for (library in KotlinRuntimeLibraryUtil.findKotlinLibraries(project)) {
                val runtimeJar = JavaRuntimePresentationProvider.getRuntimeJar(library) ?: continue
                if (JavaRuntimePresentationProvider.getReflectJar(library) != null) continue

                val model = library.getModifiableModel()

                val libFilesDir = VfsUtilCore.virtualToIoFile(runtimeJar).getParent()
                val reflectIoFile = File(libFilesDir, PathUtil.KOTLIN_JAVA_REFLECT_JAR)
                if (reflectIoFile.exists()) {
                    model.addRoot(VfsUtil.getUrlForLibraryRoot(reflectIoFile), OrderRootType.CLASSES)
                }
                else {
                    val copied = configurator.copyFileToDir(pluginReflectJar, libFilesDir)!!
                    model.addRoot(VfsUtil.getUrlForLibraryRoot(copied), OrderRootType.CLASSES)
                }

                model.commit()

                ConfigureKotlinInProjectUtils.showInfoNotification(
                        "${PathUtil.KOTLIN_JAVA_REFLECT_JAR} was added to the library ${library.getName()}"
                )
            }
        }
    }
}
