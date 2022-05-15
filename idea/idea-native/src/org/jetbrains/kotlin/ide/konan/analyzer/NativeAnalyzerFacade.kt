/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan.analyzer

import org.jetbrains.konan.createDeclarationProviderFactory
import org.jetbrains.konan.createResolvedModuleDescriptors
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.TargetPlatformVersion
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.createContainerForLazyResolve
import org.jetbrains.kotlin.idea.caches.project.ModuleProductionSourceInfo
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.konan.platform.KonanPlatform
import org.jetbrains.kotlin.resolve.lazy.ResolveSession

/**
 * @author Alefas
 */
object NativeAnalyzerFacade : ResolverForModuleFactory() {
    override val targetPlatform: TargetPlatform
        get() = KonanPlatform

    override fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        platformParameters: PlatformAnalysisParameters,
        targetEnvironment: TargetEnvironment,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
        targetPlatformVersion: TargetPlatformVersion
    ): ResolverForModule {

        val declarationProviderFactory = createDeclarationProviderFactory(
            moduleContext.project,
            moduleContext,
            moduleContent.syntheticFiles,
            moduleContent.moduleInfo,
            moduleContent.moduleContentScope
        )

        val container = createContainerForLazyResolve(
            moduleContext,
            declarationProviderFactory,
            BindingTraceContext(),
            targetPlatform,
            TargetPlatformVersion.NoVersion,
            targetEnvironment,
            languageVersionSettings
        )

        val moduleDescriptors = moduleContent.moduleInfo.createResolvedModuleDescriptors(
            moduleContext.project,
            moduleContext.storageManager,
            moduleContext.module.builtIns,
            languageVersionSettings
        )

        val packageFragmentProvider = container.get<ResolveSession>().packageFragmentProvider
        val fragmentProviders = mutableListOf(packageFragmentProvider)
        moduleDescriptors.resolvedDescriptors.mapTo(fragmentProviders) { it.packageFragmentProvider }
        fragmentProviders.add(moduleDescriptors.forwardDeclarationsModule.packageFragmentProvider)

        return ResolverForModule(CompositePackageFragmentProvider(fragmentProviders), container)
    }
}
