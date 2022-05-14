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

package org.jetbrains.jet.plugin.caches.resolve

import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.openapi.project.Project
import org.jetbrains.jet.analyzer.AnalyzeExhaust
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.containers.SLRUCache
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.CachedValueProvider
import org.jetbrains.jet.lang.resolve.BindingContext
import com.intellij.openapi.components.ServiceManager
import org.jetbrains.jet.plugin.project.AnalyzerFacadeProvider
import org.jetbrains.jet.plugin.project.TargetPlatform
import org.jetbrains.jet.plugin.project.TargetPlatform.*
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import org.jetbrains.jet.plugin.project.TargetPlatformDetector
import org.jetbrains.jet.lang.psi.JetCodeFragment
import org.jetbrains.jet.utils.keysToMap
import com.intellij.openapi.roots.ProjectRootModificationTracker
import org.jetbrains.jet.plugin.util.ProjectRootsUtil

private val LOG = Logger.getInstance(javaClass<KotlinCacheService>())

public fun JetElement.getLazyResolveSession(): ResolveSessionForBodies {
    return KotlinCacheService.getInstance(getProject()).getLazyResolveSession(this)
}

public fun JetElement.getAnalysisResults(): AnalyzeExhaust {
    return KotlinCacheService.getInstance(getProject()).getAnalysisResults(listOf(this))
}

public fun JetElement.getBindingContext(): BindingContext {
    return getAnalysisResults().getBindingContext()
}

public fun getAnalysisResultsForElements(elements: Collection<JetElement>): AnalyzeExhaust {
    if (elements.isEmpty()) return AnalyzeExhaust.EMPTY
    val element = elements.first()
    return KotlinCacheService.getInstance(element.getProject()).getAnalysisResults(elements)
}

public class KotlinCacheService(val project: Project) {
    class object {
        public fun getInstance(project: Project): KotlinCacheService = ServiceManager.getService(project, javaClass<KotlinCacheService>())!!
    }

    fun globalResolveSessionProvider(
            platform: TargetPlatform,
            dependencies: Collection<Any>,
            moduleFilter: (IdeaModuleInfo) -> Boolean,
            reuseDataFromCache: KotlinResolveCache? = null,
            syntheticFiles: Collection<JetFile> = listOf(),
            logProcessCanceled: Boolean = false
    ): () -> CachedValueProvider.Result<ModuleResolverProvider> = {
        val analyzerFacade = AnalyzerFacadeProvider.getAnalyzerFacade(platform)
        val delegateResolverProvider = reuseDataFromCache?.moduleResolverProvider ?: EmptyModuleResolverProvider
        val globalContext = (delegateResolverProvider as? ModuleResolverProviderImpl)?.globalContext
                                    ?.withCompositeExceptionTrackerUnderSameLock()
                            ?: GlobalContext(logProcessCanceled)

        val moduleResolverProvider = createModuleResolverProvider(
                project, globalContext, analyzerFacade, syntheticFiles, delegateResolverProvider, moduleFilter
        )
        val allDependencies = dependencies + listOf(moduleResolverProvider.exceptionTracker)
        CachedValueProvider.Result.create(moduleResolverProvider, allDependencies)
    }

    private val globalCachesPerPlatform = listOf(JVM, JS).keysToMap { platform -> GlobalCache(platform) }

    private inner class GlobalCache(platform: TargetPlatform) {
        val librariesCache = KotlinResolveCache(
                project, globalResolveSessionProvider(platform,
                                                      logProcessCanceled = true,
                                                      moduleFilter = { it.isLibraryClasses() },
                                                      dependencies = listOf(
                                                              LibraryModificationTracker.getInstance(project),
                                                              ProjectRootModificationTracker.getInstance(project)))
        )

        val modulesCache = KotlinResolveCache(
                project, globalResolveSessionProvider(platform,
                                                      reuseDataFromCache = librariesCache,
                                                      moduleFilter = { !it.isLibraryClasses() },
                                                      dependencies = listOf(PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT))
        )
    }

    private fun getGlobalCache(platform: TargetPlatform) = globalCachesPerPlatform[platform]!!.modulesCache
    private fun getGlobalLibrariesCache(platform: TargetPlatform) = globalCachesPerPlatform[platform]!!.librariesCache

    private val syntheticFileCaches = object : SLRUCache<JetFile, KotlinResolveCache>(2, 3) {
        override fun createValue(file: JetFile?): KotlinResolveCache {
            val targetPlatform = TargetPlatformDetector.getPlatform(file!!)
            val syntheticFileModule = file.getModuleInfo()
            return when {
                syntheticFileModule is ModuleSourceInfo -> {
                    val dependentModules = syntheticFileModule.getDependentModules()
                    KotlinResolveCache(
                            project,
                            globalResolveSessionProvider(
                                    targetPlatform,
                                    syntheticFiles = listOf(file),
                                    reuseDataFromCache = getGlobalCache(targetPlatform),
                                    moduleFilter = { it in dependentModules },
                                    dependencies = listOf(PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)
                            )
                    )
                }

                syntheticFileModule is LibrarySourceInfo || syntheticFileModule is NotUnderContentRootModuleInfo -> {
                    KotlinResolveCache(
                            project,
                            globalResolveSessionProvider(
                                    targetPlatform,
                                    syntheticFiles = listOf(file),
                                    reuseDataFromCache = getGlobalLibrariesCache(targetPlatform),
                                    moduleFilter = { it == syntheticFileModule },
                                    dependencies = listOf(PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)
                            )
                    )
                }

                syntheticFileModule.isLibraryClasses() -> {
                    //NOTE: this code should not be called for sdk or library classes
                    // currently the only known scenario is when we cannot determine that file is a library source
                    // (file under both classes and sources root)
                    LOG.warn("Creating cache with synthetic file ($file) in classes of library $syntheticFileModule")
                    KotlinResolveCache(
                            project,
                            globalResolveSessionProvider(
                                    targetPlatform,
                                    syntheticFiles = listOf(file),
                                    moduleFilter = { true },
                                    dependencies = listOf(PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)
                            )
                    )
                }

                else -> throw IllegalStateException("Unknown IdeaModuleInfo ${syntheticFileModule.javaClass}")
            }
        }
    }

    private fun getCacheForSyntheticFile(file: JetFile): KotlinResolveCache {
        return synchronized(syntheticFileCaches) {
            syntheticFileCaches[file]
        }
    }

    public fun getGlobalLazyResolveSession(file: JetFile, platform: TargetPlatform): ResolveSessionForBodies {
        return getGlobalCache(platform).getLazyResolveSession(file)
    }

    public fun getLazyResolveSession(element: JetElement): ResolveSessionForBodies {
        val file = element.getContainingJetFile()
        if (!ProjectRootsUtil.isInProjectSource(file)) {
            return getCacheForSyntheticFile(file).getLazyResolveSession(file)
        }

        return getGlobalLazyResolveSession(file, TargetPlatformDetector.getPlatform(file))
    }

    public fun getAnalysisResults(elements: Collection<JetElement>): AnalyzeExhaust {
        if (elements.isEmpty()) return AnalyzeExhaust.EMPTY

        val firstFile = elements.first().getContainingJetFile()
        if (elements.size == 1 && (!ProjectRootsUtil.isInProjectSource(firstFile) && firstFile !is JetCodeFragment)) {
            return getCacheForSyntheticFile(firstFile).getAnalysisResultsForElements(elements)
        }

        return getGlobalCache(TargetPlatformDetector.getPlatform(firstFile)).getAnalysisResultsForElements(elements)
    }

    public fun <T> get(extension: CacheExtension<T>): T {
        return getGlobalCache(extension.platform)[extension]
    }
}
