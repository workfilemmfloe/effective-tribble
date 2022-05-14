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

package org.jetbrains.jet.plugin.internal.makeBackup

import com.intellij.history.LocalHistory
import com.intellij.openapi.compiler.CompileTask
import com.intellij.openapi.compiler.CompileContext
import java.util.Random
import com.intellij.openapi.util.Key
import com.intellij.compiler.CompilerWorkspaceConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.ServiceManager
import org.jetbrains.jet.plugin.compiler.configuration.KotlinCompilerWorkspaceSettings

val random = Random()

val HISTORY_LABEL_KEY = Key.create<String>("history label")

public class MakeBackupCompileTask: CompileTask {
    override fun execute(context: CompileContext?): Boolean {
        val project = context!!.getProject()!!

        if (!incrementalCompilationEnabled(project)) return true

        val localHistory = LocalHistory.getInstance()!!
        val label = HISTORY_LABEL_PREFIX + Integer.toHexString(random.nextInt())
        localHistory.putSystemLabel(project, label)

        context.getCompileScope()!!.putUserData(HISTORY_LABEL_KEY, label)

        return true
    }

}

fun incrementalCompilationEnabled(project: Project): Boolean {
    return ServiceManager.getService(project, javaClass<KotlinCompilerWorkspaceSettings>()).incrementalCompilationEnabled
}
