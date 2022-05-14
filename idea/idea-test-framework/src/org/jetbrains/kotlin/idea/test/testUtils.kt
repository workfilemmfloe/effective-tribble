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

package org.jetbrains.kotlin.idea.test

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootModificationUtil.updateModel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.impl.file.impl.FileManagerImpl
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.Consumer
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.caches.resolve.LibraryModificationTracker
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.decompiler.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFile
import org.jetbrains.kotlin.psi.KtFile
import java.util.*

enum class ModuleKind {
    KOTLIN_JVM_WITH_STDLIB_SOURCES,
    KOTLIN_JAVASCRIPT
}

fun Module.configureAs(descriptor: KotlinLightProjectDescriptor) {
    val module = this
    updateModel(module, object : Consumer<ModifiableRootModel> {
        override fun consume(model: ModifiableRootModel) {
            if (descriptor.sdk != null) {
                model.sdk = descriptor.sdk
            }
            val entries = model.contentEntries
            if (entries.isEmpty()) {
                descriptor.configureModule(module, model)
            }
            else {
                descriptor.configureModule(module, model, entries[0])
            }
        }
    })
}

fun Module.configureAs(kind: ModuleKind) {
    when(kind) {
        ModuleKind.KOTLIN_JVM_WITH_STDLIB_SOURCES ->
            this.configureAs(ProjectDescriptorWithStdlibSources.INSTANCE)
        ModuleKind.KOTLIN_JAVASCRIPT -> {
            this.configureAs(KotlinStdJSProjectDescriptor.instance)
        }

        else -> throw IllegalArgumentException("Unknown kind=$kind")
    }
}

fun KtFile.dumpTextWithErrors(): String {
    return diagnosticsHeader() + getText()
}

fun KtFile.diagnosticsHeader(): String {
    val diagnostics = analyzeFullyAndGetResult().bindingContext.getDiagnostics()
    val errors = diagnostics.filter { it.getSeverity() == Severity.ERROR }
    if (errors.isEmpty()) return ""
    return errors.map { "// ERROR: " + DefaultErrorMessages.render(it).replace('\n', ' ') }.joinToString("\n", postfix = "\n")
}

fun closeAndDeleteProject(): Unit =
    ApplicationManager.getApplication().runWriteAction() { LightPlatformTestCase.closeAndDeleteProject() }

fun unInvalidateBuiltinsAndStdLib(project: Project, runnable: RunnableWithException) {
    val fileManager = (PsiManager.getInstance(project) as PsiManagerEx).getFileManager()

    val stdLibViewProviders = HashSet<KotlinDecompiledFileViewProvider>()
    val vFileToViewProviderMap = ((PsiManager.getInstance(project) as PsiManagerEx).fileManager as FileManagerImpl).vFileToViewProviderMap
    for ((file, viewProvider) in vFileToViewProviderMap) {
        if (file.isStdLibFile && viewProvider is KotlinDecompiledFileViewProvider) {
            stdLibViewProviders.add(viewProvider)
        }
    }

    runnable.run()

    // Restore mapping between PsiFiles and VirtualFiles dropped in FileManager.cleanupForNextTest(),
    // otherwise built-ins psi elements will become invalid in next test.
    fun unInvalidateFile(file: PsiFileImpl) {
        val provider = file.getViewProvider();
        fileManager.setViewProvider(provider.getVirtualFile(), provider);
    }

    stdLibViewProviders.forEach {
        it.allFiles.forEach { unInvalidateFile(it as KtDecompiledFile) }
        vFileToViewProviderMap.set(it.virtualFile, it)
    }
}

private val VirtualFile.isStdLibFile: Boolean get() = presentableUrl.contains("kotlin-runtime.jar")

fun unInvalidateBuiltinsAndStdLib(project: Project, runnable: () -> Unit) {
    unInvalidateBuiltinsAndStdLib(project, RunnableWithException { runnable() })
}

fun invalidateLibraryCache(project: Project) {
    LibraryModificationTracker.getInstance(project).incModificationCount()
}