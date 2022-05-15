/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.definitions

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.valueOr

fun PsiFile.findScriptCompilationConfiguration(): ScriptCompilationConfiguration? {
    // Do not use psiFile.script, see comments in findScriptDefinition
    if (this !is KtFile/* || this.script == null*/) return null
    val file = virtualFile ?: originalFile.virtualFile ?: return null
    if (file.isNonScript()) return null

    val provider = ScriptDependenciesProvider.getInstance(project)
    return provider?.getScriptConfigurationResult(this)?.valueOrError()?.configuration
        ?: findScriptDefinition()?.compilationConfiguration
}

fun VirtualFile.findScriptCompilationConfiguration(project: Project): ScriptCompilationConfiguration? {
    if (!isValid || isNonScript()) return null
    // see comments in findScriptDefinition
    // we do not need expensive check as in findScriptDefinition here, since it is assumed that this function is called only for known scripts

    val provider = ScriptDependenciesProvider.getInstance(project)
    return provider?.getScriptConfigurationResult(this)?.valueOrError()?.configuration
        ?: findScriptDefinition(project)?.compilationConfiguration
}

private fun ScriptCompilationConfigurationResult.valueOrError() = valueOr { failure ->
    throw IllegalArgumentException(
        "Error retrieving script compilation configuration: ${failure.reports.joinToString { it.message }}",
        failure.reports.find { it.exception != null }?.exception
    )
}
