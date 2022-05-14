/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.project;

import com.google.common.base.Predicates;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.containers.SLRUCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.context.ContextPackage;
import org.jetbrains.jet.context.GlobalContext;
import org.jetbrains.jet.descriptors.serialization.descriptors.MemberFilter;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.plugin.caches.resolve.*;
import org.jetbrains.jet.plugin.util.ApplicationUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public final class AnalyzerFacadeWithCache {

    private static final Logger LOG = Logger.getInstance("org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache");

    private final static Key<CachedValue<SLRUCache<JetFile, AnalyzeExhaust>>> ANALYZE_EXHAUST_FULL = Key.create("ANALYZE_EXHAUST_FULL");

    private static final Object lock = new Object();

    private AnalyzerFacadeWithCache() {
    }

    /**
     * Analyze project with string cache for given file. Given file will be fully analyzed.
     */
    // TODO: Also need to pass several files when user have multi-file environment
    @NotNull
    public static AnalyzeExhaust analyzeFileWithCache(@NotNull JetFile file) {
        // Need lock, because parallel threads can start evaluation of compute() simultaneously
        synchronized (lock) {
            Project project = file.getProject();
            return CachedValuesManager.getManager(project).getCachedValue(
                    project,
                    ANALYZE_EXHAUST_FULL,
                    new SLRUCachedAnalyzeExhaustProvider(),
                    false
            ).get(file);
        }
    }

    @NotNull
    public static BindingContext getContextForElement(@NotNull JetElement jetElement) {
        ResolveSessionForBodies resolveSessionForBodies = getLazyResolveSessionForFile((JetFile) jetElement.getContainingFile());
        return resolveSessionForBodies.resolveToElement(jetElement);
    }

    @NotNull
    public static ResolveSessionForBodies getLazyResolveSessionForFile(@NotNull JetFile file) {
        Project project = file.getProject();
        DeclarationsCacheProvider provider = KotlinCacheManager.getInstance(project).getRegisteredProvider(TargetPlatformDetector.getPlatform(file));

        if (!provider.areDeclarationsAvailable(file)) {
            // There can be request for temp files (in completion) or non-source (in library) files. Create temp sessions for them.
            CachedValue<ResolveSessionForBodies> cachedValue;

            synchronized (PER_FILE_SESSION_CACHE) {
                cachedValue = PER_FILE_SESSION_CACHE.get(file);
            }

            return cachedValue.getValue();
        }

        return provider.getLazyResolveSession();
    }

    @NotNull
    private static AnalyzeExhaust emptyExhaustWithDiagnosticOnFile(JetFile file, Throwable e) {
        BindingTraceContext bindingTraceContext = new BindingTraceContext();
        bindingTraceContext.report(Errors.EXCEPTION_WHILE_ANALYZING.on(file, e));
        return AnalyzeExhaust.error(bindingTraceContext.getBindingContext(), e);
    }

    private static final SLRUCache<JetFile, CachedValue<ResolveSessionForBodies>> PER_FILE_SESSION_CACHE = new SLRUCache<JetFile, CachedValue<ResolveSessionForBodies>>(2, 3) {
        @NotNull
        @Override
        public CachedValue<ResolveSessionForBodies> createValue(final JetFile file) {
            final Project fileProject = file.getProject();
            return CachedValuesManager.getManager(fileProject).createCachedValue(
                    // Each value monitors OUT_OF_CODE_BLOCK_MODIFICATION_COUNT and modification tracker of the stored value
                    new CachedValueProvider<ResolveSessionForBodies>() {
                        @Nullable
                        @Override
                        public Result<ResolveSessionForBodies> compute() {
                            Project project = file.getProject();


                            Collection<JetFile> files = new HashSet<JetFile>(JetFilesProvider.getInstance(project).allInScope(GlobalSearchScope.allScope(project)));

                            // Add requested file to the list of files for searching declarations
                            files.add(file);

                            if (file != file.getOriginalFile()) {
                                // Given file can be a non-physical copy of the file in list (completion case). Remove the prototype file.

                                //noinspection SuspiciousMethodCalls
                                files.remove(file.getOriginalFile());
                            }

                            ResolveSession resolveSession = AnalyzerFacadeProvider.getAnalyzerFacadeForFile(file).getLazyResolveSession(fileProject, files);
                            return Result.create(new ResolveSessionForBodies(file, resolveSession), PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT);
                        }
                    },
                    true);
        }
    };

    private static class SLRUCachedAnalyzeExhaustProvider implements CachedValueProvider<SLRUCache<JetFile, AnalyzeExhaust>> {
        @Nullable
        @Override
        public Result<SLRUCache<JetFile, AnalyzeExhaust>> compute() {
            final GlobalContext globalContext = ContextPackage.GlobalContext();
            SLRUCache<JetFile, AnalyzeExhaust> cache = new SLRUCache<JetFile, AnalyzeExhaust>(3, 8) {

                @NotNull
                @Override
                public AnalyzeExhaust createValue(JetFile file) {
                    try {
                        if (DumbService.isDumb(file.getProject())) {
                            return AnalyzeExhaust.EMPTY;
                        }

                        ApplicationUtils.warnTimeConsuming(LOG);

                        AnalyzeExhaust analyzeExhaustHeaders = analyzeHeadersWithCacheOnFile(file, globalContext);
                        return analyzeBodies(analyzeExhaustHeaders, file);
                    }
                    catch (ProcessCanceledException e) {
                        throw e;
                    }
                    catch (Throwable e) {
                        handleError(e);

                        // Exception during body resolve analyze can harm internal caches in declarations cache
                        KotlinCacheManager.getInstance(file.getProject()).invalidateCache();

                        return emptyExhaustWithDiagnosticOnFile(file, e);
                    }
                }
            };
            return Result.create(cache, PsiModificationTracker.MODIFICATION_COUNT, globalContext.getExceptionTracker());
        }

        private static AnalyzeExhaust analyzeHeadersWithCacheOnFile(@NotNull JetFile fileToCache, @NotNull GlobalContext globalContext) {
            VirtualFile virtualFile = fileToCache.getVirtualFile();
            if (LightClassUtil.belongsToKotlinBuiltIns(fileToCache) ||
                virtualFile != null && LibraryUtil.findLibraryEntry(virtualFile, fileToCache.getProject()) != null) {
                // Library sources:
                // Mark file to skip
                fileToCache.putUserData(LibrarySourceHacks.SKIP_TOP_LEVEL_MEMBERS, true);
                // Resolve this file, not only project files (as KotlinCacheManager do)

                return AnalyzerFacadeForJVM
                        .analyzeFilesWithJavaIntegrationInGlobalContext(
                                fileToCache.getProject(),
                                Collections.singleton(fileToCache),
                                new BindingTraceContext(),
                                Predicates.<PsiFile>alwaysFalse(),
                                true,
                                AnalyzerFacadeForJVM.createJavaModule("<module>"),
                                globalContext,
                                MemberFilter.ALWAYS_TRUE
                        );
            }

            KotlinDeclarationsCache cache = KotlinCacheManagerUtil.getDeclarationsFromProject(fileToCache);
            return ((KotlinDeclarationsCacheImpl) cache).getAnalyzeExhaust();
        }

        private static AnalyzeExhaust analyzeBodies(AnalyzeExhaust analyzeExhaustHeaders, JetFile file) {
            BodiesResolveContext context = analyzeExhaustHeaders.getBodiesResolveContext();
            assert context != null : "Headers resolver should prepare and stored information for bodies resolve";

            // Need to resolve bodies in given file and all in the same package
            return AnalyzerFacadeProvider.getAnalyzerFacadeForFile(file).analyzeBodiesInFiles(
                    file.getProject(),
                    new JetFilesProvider.SameJetFilePredicate(file),
                    new DelegatingBindingTrace(analyzeExhaustHeaders.getBindingContext(),
                                               "trace to resolve bodies in file", file.getName()),
                    context,
                    analyzeExhaustHeaders.getModuleDescriptor());
        }

        private static void handleError(@NotNull Throwable e) {
            DiagnosticUtils.throwIfRunningOnServer(e);
            LOG.error(e);
        }
    }
}
