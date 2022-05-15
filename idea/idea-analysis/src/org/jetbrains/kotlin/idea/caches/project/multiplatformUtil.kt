/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.project

import com.intellij.facet.FacetTypeRegistry
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.facet.KotlinFacetType
import org.jetbrains.kotlin.resolve.TargetPlatform

val Module.implementingModules: List<Module>
    get() = cached(CachedValueProvider {
        CachedValueProvider.Result(
            findImplementingModules(IdeModifiableModelsProviderImpl(project)),
            ProjectRootModificationTracker.getInstance(project)
        )
    })

val Module.implementedModules: List<Module>
    get() = this.cached(
        CachedValueProvider {
            val modelsProvider = IdeModifiableModelsProviderImpl(project)
            CachedValueProvider.Result(
                findImplementedModuleNames(modelsProvider).mapNotNull { modelsProvider.findIdeModule(it) },
                ProjectRootModificationTracker.getInstance(project)
            )
        }
    )

private fun Module.findImplementingModules(modelsProvider: IdeModifiableModelsProvider) =
    modelsProvider.modules.filter { name in it.findImplementedModuleNames(modelsProvider) }

private fun Module.findImplementedModuleNames(modelsProvider: IdeModifiableModelsProvider): List<String> {
    val facetModel = modelsProvider.getModifiableFacetModel(this)
    val facet = facetModel.findFacet(
        KotlinFacetType.TYPE_ID,
        FacetTypeRegistry.getInstance().findFacetType(KotlinFacetType.ID)!!.defaultFacetName
    )
    return facet?.configuration?.settings?.implementedModuleNames ?: emptyList()
}


private fun Module.toInfo(isTests: Boolean): ModuleSourceInfo? =
    if (isTests) testSourceInfo() else productionSourceInfo()

val ModuleDescriptor.implementingDescriptors: List<ModuleDescriptor>
    get() {
        val moduleInfo = getCapability(ModuleInfo.Capability)
        if (moduleInfo is PlatformModuleInfo) {
            return listOf(this)
        }
        val moduleSourceInfo = moduleInfo as? ModuleSourceInfo ?: return emptyList()
        val implementingModuleInfos = moduleSourceInfo.module.implementingModules.mapNotNull { it.toInfo(moduleSourceInfo.isTests()) }
        return implementingModuleInfos.mapNotNull { it.toDescriptor() }
    }

val ModuleDescriptor.implementedDescriptors: List<ModuleDescriptor>
    get() {
        val moduleInfo = getCapability(ModuleInfo.Capability)
        if (moduleInfo is PlatformModuleInfo) return listOf(this)

        val moduleSourceInfo = moduleInfo as? ModuleSourceInfo ?: return emptyList()

        return moduleSourceInfo.expectedBy.mapNotNull { it.toDescriptor() }
    }

private fun ModuleSourceInfo.toDescriptor() = KotlinCacheService.getInstance(module.project)
    .getResolutionFacadeByModuleInfo(this, platform)?.moduleDescriptor

fun PsiElement.getPlatformModuleInfo(desiredPlatform: TargetPlatform): PlatformModuleInfo? {
    assert(desiredPlatform != TargetPlatform.Common)
    val moduleInfo = getModuleInfo() as? ModuleSourceInfo ?: return null
    return when (moduleInfo.platform) {
        TargetPlatform.Common -> {
            val correspondingImplementingModule = moduleInfo.module.implementingModules.map { it.toInfo(moduleInfo.isTests()) }
                .firstOrNull { it?.platform == desiredPlatform } ?: return null
            PlatformModuleInfo(correspondingImplementingModule, correspondingImplementingModule.expectedBy)
        }
        desiredPlatform -> {
            val expectedBy = moduleInfo.expectedBy.takeIf { it.isNotEmpty() } ?: return null
            PlatformModuleInfo(moduleInfo, expectedBy)
        }
        else -> null
    }
}
