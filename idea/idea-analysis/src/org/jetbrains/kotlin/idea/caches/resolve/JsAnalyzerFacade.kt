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

package org.jetbrains.kotlin.idea.caches.resolve

import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.analyzer.ResolverForModule
import org.jetbrains.kotlin.analyzer.PlatformAnalysisParameters
import org.jetbrains.kotlin.analyzer.AnalyzerFacade
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.context.GlobalContext
import org.jetbrains.kotlin.analyzer.ResolverForProject
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.di.InjectorForLazyResolve
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.ModuleContent
import org.jetbrains.kotlin.js.resolve.KotlinJsCheckerProvider
import org.jetbrains.kotlin.types.DynamicTypesAllowed

public class JsResolverForModule(
        override val lazyResolveSession: ResolveSession
) : ResolverForModule


public object JsAnalyzerFacade : AnalyzerFacade<JsResolverForModule, PlatformAnalysisParameters> {

    override fun <M : ModuleInfo> createResolverForModule(
            moduleInfo: M,
            project: Project,
            globalContext: GlobalContext,
            moduleDescriptor: ModuleDescriptorImpl,
            moduleContent: ModuleContent,
            platformParameters: PlatformAnalysisParameters,
            resolverForProject: ResolverForProject<M, JsResolverForModule>
    ): JsResolverForModule {
        val (syntheticFiles, moduleContentScope) = moduleContent
        val declarationProviderFactory = DeclarationProviderFactoryService.createDeclarationProviderFactory(
                project, globalContext.storageManager, syntheticFiles, moduleContentScope
        )

        val injector = InjectorForLazyResolve(project, globalContext, moduleDescriptor, declarationProviderFactory, BindingTraceContext(),
                                              KotlinJsCheckerProvider, DynamicTypesAllowed())
        val resolveSession = injector.getResolveSession()!!
        moduleDescriptor.initialize(resolveSession.getPackageFragmentProvider())
        return JsResolverForModule(resolveSession)
    }

    override val defaultImports = TopDownAnalyzerFacadeForJS.DEFAULT_IMPORTS
    override val platformToKotlinClassMap = PlatformToKotlinClassMap.EMPTY

}
