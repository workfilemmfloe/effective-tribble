/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.project

import com.intellij.facet.FacetManager
import com.intellij.facet.FacetTypeRegistry
import com.intellij.openapi.externalSystem.service.project.IdeModelsProviderImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.psi.util.CachedValueProvider
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.caches.project.SourceType.PRODUCTION
import org.jetbrains.kotlin.idea.caches.project.SourceType.TEST
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.facet.KotlinFacetType
import org.jetbrains.kotlin.idea.facet.KotlinFacetType.Companion.ID
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.rootManager
import org.jetbrains.kotlin.platform.impl.CommonIdePlatformKind
import org.jetbrains.kotlin.resolve.isCommon

val Module.isNewMPPModule: Boolean
    get() = facetSettings?.kind?.isNewMPP ?: false

val Module.externalProjectId: String
    get() = facetSettings?.externalProjectId ?: ""

val Module.sourceType: SourceType?
    get() = facetSettings?.isTestModule?.let { isTest -> if (isTest) SourceType.TEST else PRODUCTION }

val Module.isMPPModule: Boolean
    get() {
        val settings = facetSettings ?: return false
        return settings.platform.isCommon() ||
                settings.implementedModuleNames.isNotEmpty() ||
                settings.kind.isNewMPP
    }

private val Module.facetSettings get() = KotlinFacet.get(this)?.configuration?.settings

val Module.implementingModules: List<Module>
    get() = cached(CachedValueProvider {
        val moduleManager = ModuleManager.getInstance(project)
        CachedValueProvider.Result(
            if (isNewMPPModule) {
                moduleManager.getModuleDependentModules(this).filter {
                    it.isNewMPPModule && it.externalProjectId == externalProjectId
                }
            } else {
                moduleManager.modules.filter { name in it.findOldFashionedImplementedModuleNames() }
            },
            ProjectRootModificationTracker.getInstance(project)
        )
    })

val Module.implementedModules: List<Module>
    get() = cached<List<Module>>(
        CachedValueProvider {
            CachedValueProvider.Result(
                if (isNewMPPModule) {
                    rootManager.dependencies.filter {
                        it.isNewMPPModule && it.platform.isCommon() && it.externalProjectId == externalProjectId
                    }
                } else {
                    val modelsProvider = IdeModelsProviderImpl(project)
                    findOldFashionedImplementedModuleNames().mapNotNull { modelsProvider.findIdeModule(it) }
                },
                ProjectRootModificationTracker.getInstance(project)
            )
        }
    )

private fun Module.findOldFashionedImplementedModuleNames(): List<String> {
    val facet = FacetManager.getInstance(this).findFacet(
        KotlinFacetType.TYPE_ID,
        FacetTypeRegistry.getInstance().findFacetType(ID)!!.defaultFacetName
    )
    return facet?.configuration?.settings?.implementedModuleNames ?: emptyList()
}


val ModuleDescriptor.implementingDescriptors: List<ModuleDescriptor>
    get() {
        val moduleInfo = getCapability(ModuleInfo.Capability)
        /** Same */
//        if (moduleInfo is PlatformModuleInfo) {
//            return listOf(this)
//        }
        val moduleSourceInfo = moduleInfo as? ModuleSourceInfo ?: return emptyList()
        val implementingModuleInfos = moduleSourceInfo.module.implementingModules.mapNotNull { it.toInfo(moduleSourceInfo.sourceType) }
        return implementingModuleInfos.mapNotNull { it.toDescriptor() }
    }

private fun Module.toInfo(type: SourceType): ModuleSourceInfo? = when (type) {
    PRODUCTION -> productionSourceInfo()
    TEST -> testSourceInfo()
}


/**
 * This function returns immediate parents in dependsOn graph
 */
val ModuleDescriptor.implementedDescriptors: List<ModuleDescriptor>
    get() {
        val moduleInfo = getCapability(ModuleInfo.Capability)
        /** This is weird, but strangely makes sense: combined module implements itself */
//        if (moduleInfo is PlatformModuleInfo) return listOf(this)

        val moduleSourceInfo = moduleInfo as? ModuleSourceInfo ?: return emptyList()

        return moduleSourceInfo.expectedBy.mapNotNull { it.toDescriptor() }
    }

private fun ModuleSourceInfo.toDescriptor() = KotlinCacheService.getInstance(module.project)
    .getResolutionFacadeByModuleInfo(this)?.moduleDescriptor

/** This function used to emulate a behaviour somewhat similar to projection (on module-level):
 *      Given a module, get a platforms-view of that module
 *
 *  This was implemented by following algo:
 *  - if it is a common-module, they find along its children a module with required platforms.
 *    Then, build a PlatformModuleInfo for that children (it will include a given module, but will provide a semantic
 *    of platforms-view of that given module)
 *  - otherwise, build a PlatformModuleInfo straightforwardly. This function returns null if requested module hadn't childs,
 *    seems like it is just for consistency.
 *
 *  In a new world, operation of projection of module shouldn't be really necessary.
 *  Each client should be dealt with on case-by-case basis
 */
//fun PsiElement.getPlatformModuleInfo(desiredPlatform: TargetPlatform): PlatformModuleInfo? {
//    assert(desiredPlatform !is CommonPlatform) { "Platform module cannot have Common platforms" }
//    val moduleInfo = getNullableModuleInfo() as? ModuleSourceInfo ?: return null
//    return when (moduleInfo.platforms) {
//        is CommonPlatform -> {
//            val correspondingImplementingModule = moduleInfo.module.implementingModules.map { it.toInfo(moduleInfo.sourceType) }
//                .firstOrNull { it?.platforms == desiredPlatform } ?: return null
//            PlatformModuleInfo(correspondingImplementingModule, correspondingImplementingModule.expectedBy)
//        }
//        desiredPlatform -> {
//            val expectedBy = moduleInfo.expectedBy.takeIf { it.isNotEmpty() } ?: return null
//            PlatformModuleInfo(moduleInfo, expectedBy)
//        }
//        else -> null
//    }
//}
