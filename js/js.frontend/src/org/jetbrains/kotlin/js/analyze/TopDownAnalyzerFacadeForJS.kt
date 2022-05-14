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

package org.jetbrains.kotlin.js.analyze

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.context.ContextForNewModule
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.frontend.js.di.createTopDownAnalyzerForJs
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory

object TopDownAnalyzerFacadeForJS {
    @JvmStatic
    fun analyzeFiles(files: Collection<KtFile>, config: JsConfig): JsAnalysisResult {
        val context =
                ContextForNewModule(ProjectContext(config.project), Name.special("<${config.moduleId}>"), JsPlatform, JsPlatform.builtIns)
        context.setDependencies(
                listOf(context.module) +
                config.moduleDescriptors.map { it.data } +
                listOf(JsPlatform.builtIns.builtInsModule)
        )
        return analyzeFilesWithGivenTrace(files, BindingTraceContext(), context, config)
    }

    @JvmStatic
    fun analyzeFilesWithGivenTrace(
            files: Collection<KtFile>,
            trace: BindingTrace,
            moduleContext: ModuleContext,
            config: JsConfig
    ): JsAnalysisResult {
        val allFiles = JsConfig.withJsLibAdded(files, config)
        val analyzerForJs = createTopDownAnalyzerForJs(
                moduleContext, trace,
                FileBasedDeclarationProviderFactory(moduleContext.storageManager, allFiles),
                config.configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, LanguageVersionSettingsImpl.DEFAULT)
        )
        analyzerForJs.analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)
        return JsAnalysisResult.success(trace, moduleContext.module)
    }

    @JvmStatic
    fun checkForErrors(allFiles: Collection<KtFile>, bindingContext: BindingContext) {
        AnalyzingUtils.throwExceptionOnErrors(bindingContext)
        for (file in allFiles) {
            AnalyzingUtils.checkForSyntacticErrors(file)
        }
    }
}
