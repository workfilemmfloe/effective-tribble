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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.*;
import com.intellij.compiler.ModuleCompilerUtil;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.impl.scopes.LibraryScope;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.impl.LibraryScopeCache;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.Chunk;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.graph.CachingSemiGraph;
import com.intellij.util.graph.Graph;
import com.intellij.util.graph.GraphGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.TraceBasedLightClassResolver;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolver;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentKind;
import org.jetbrains.jet.lang.descriptors.SubModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.MutableSubModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.provider.PsiDeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.lazy.storage.LockBasedStorageManager;
import org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.plugin.JetFileType;

import java.util.*;

public class IdeModuleManager implements KotlinModuleManager {

    public static final Predicate<Object> MUTABLE_SUB_MODULE_DESCRIPTOR = Predicates.instanceOf(MutableSubModuleDescriptor.class);

    @NotNull
    public static IdeModuleManager getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, IdeModuleManager.class);
    }

    private final Project project;

    public IdeModuleManager(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public Collection<ModuleDescriptor> getModules() {
        return getData().getModuleDescriptors();
    }

    @NotNull
    @Override
    public ModuleSourcesManager getSourcesManager() {
        return getData().moduleSourcesManager;
    }

    private KotlinModules getData() {
        return CachedValuesManager.getManager(project).getCachedValue(project, new CachedValueProvider<KotlinModules>() {
            @Nullable
            @Override
            public Result<KotlinModules> compute() {
                return Result.create(
                        new ModuleBuilder().computeModules(),
                        ProjectRootManager.getInstance(project),
                        // Since our modules are eager, we have to invalidate them on every out-of-code-block modification
                        PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT
                );
            }
        });
    }

    private class ModuleBuilder {
        private final StorageManager storageManager = new LockBasedStorageManager();

        private final MutableModuleSourcesManager moduleSourcesManager;
        private final JavaClassResolutionFacadeImpl classResolutionFacade;

        private final BiMap<Module, ModuleDescriptor> kotlinModules = HashBiMap.create();
        private final BiMap<Module, MutableSubModuleDescriptor> srcSubModules = HashBiMap.create();
        private final BiMap<Module, MutableSubModuleDescriptor> testSubModules = HashBiMap.create();

        private final Map<Library, MutableSubModuleDescriptor> librarySubModules = Maps.newHashMap();
        private final Map<Sdk, MutableSubModuleDescriptor> sdkSubModules = Maps.newHashMap();

        private final BindingTrace trace;


        private ModuleBuilder() {
            this.trace = new BindingTraceContext();
            this.moduleSourcesManager = new MutableModuleSourcesManager(project);
            this.classResolutionFacade = new JavaClassResolutionFacadeImpl(
                    new TraceBasedLightClassResolver(trace.getBindingContext())
            );
        }

        private void registerJavaPackageFragmentProvider(MutableSubModuleDescriptor subModule, JavaPackageFragmentProvider provider) {
            classResolutionFacade.addPackageFragmentProvider(provider);
            subModule.addPackageFragmentProvider(provider);
        }

        @NotNull
        public KotlinModules computeModules() {
            Module[] ideaModules = ModuleManager.getInstance(project).getModules();
            for (Module ideaModule : ideaModules) {
                createModuleAndSubModules(ideaModule);
            }

            for (Module ideaModule : ideaModules) {
                createDependencies(ideaModule);
            }

            List<Chunk<MutableSubModuleDescriptor>> dependencyGraphSCCs = ModuleCompilerUtil.getSortedChunks(getSubModuleGraph());
            for (Chunk<MutableSubModuleDescriptor> scc : dependencyGraphSCCs) {
                resolveKotlinInStronglyConnectedSubModules(scc.getNodes(), trace);
            }

            return new KotlinModules(kotlinModules, moduleSourcesManager, trace.getBindingContext());
        }

        private Graph<MutableSubModuleDescriptor> getSubModuleGraph() {
            return GraphGenerator.create(CachingSemiGraph.create(
                    new GraphGenerator.SemiGraph<MutableSubModuleDescriptor>() {
                        @Override
                        public Collection<MutableSubModuleDescriptor> getNodes() {
                            //noinspection unchecked
                            return Lists.newArrayList(ContainerUtil.concat(srcSubModules.values(), testSubModules.values()));
                        }

                        @Override
                        public Iterator<MutableSubModuleDescriptor> getIn(MutableSubModuleDescriptor m) {
                            //noinspection unchecked
                            return (Iterator) Collections2.filter(m.getDependencies(), MUTABLE_SUB_MODULE_DESCRIPTOR).iterator();
                        }
                    }));
        }

        // This method creates package fragments for all mentioned packages in all submodules
        // See usages of MutableSubModuleDescriptor.getPackageFragmentProviderForKotlinSources()
        private void resolveKotlinInStronglyConnectedSubModules(Set<MutableSubModuleDescriptor> scc, BindingTrace trace) {
            Collection<JetFile> sourceFiles = Lists.newArrayList();
            for (MutableSubModuleDescriptor subModule : scc) {
                collectKotlinSourceFiles(subModule, moduleSourcesManager, sourceFiles);
            }

            TopDownAnalysisParameters parameters = new TopDownAnalysisParameters(
                    Predicates.<PsiFile>alwaysFalse(), false, false, Collections.<AnalyzerScriptParameter>emptyList()
            );
            TopDownAnalyzer topDownAnalyzer = new InjectorForTopDownAnalyzerForJvm(
                    project, parameters, trace, moduleSourcesManager
            ).getTopDownAnalyzer();
            topDownAnalyzer.analyzeFiles(sourceFiles, Collections.<AnalyzerScriptParameter>emptyList());
        }

        private void collectKotlinSourceFiles(
                final MutableSubModuleDescriptor subModule,
                final MutableModuleSourcesManager sourcesManager,
                final Collection<JetFile> result
        ) {
            Module module = srcSubModules.inverse().get(subModule);
            final boolean tests = module == null;
            if (tests) {
                module = testSubModules.inverse().get(subModule);
                assert module != null : "SubModule not present in either src or test map: " + subModule;
            }

            final ModuleFileIndex index = ModuleRootManager.getInstance(module).getFileIndex();
            index.iterateContent(new ContentIterator() {
                @Override
                public boolean processFile(VirtualFile file) {
                    if (file.isDirectory()) return true;
                    if (!index.isInSourceContent(file) && tests) return true;
                    if (!index.isInTestSourceContent(file) && !tests) return true;

                    FileType fileType = FileTypeManager.getInstance().getFileTypeByFile(file);
                    if (fileType != JetFileType.INSTANCE) return true;
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                    if (psiFile instanceof JetFile) {
                        sourcesManager.registerRoot(subModule, PackageFragmentKind.SOURCE, file);
                        result.add((JetFile) psiFile);
                    }
                    return true;
                }
            });
        }

        private void createModuleAndSubModules(Module ideaModule) {
            MutableModuleDescriptor moduleDescriptor = new MutableModuleDescriptor(
                    stringToSpecialName(ideaModule.getName()),
                    getClassMap(ideaModule)
            );
            kotlinModules.put(ideaModule, moduleDescriptor);

            Collection<SourceFolder> testFolders = Lists.newArrayList();
            Collection<SourceFolder> srcFolders = Lists.newArrayList();
            splitSourceFolders(ideaModule, testFolders, srcFolders);

            if (!srcFolders.isEmpty()) {
                createSrcSubModule(ideaModule, moduleDescriptor);
            }

            if (!testFolders.isEmpty()) {
                createTestSubModule(ideaModule, moduleDescriptor);
            }

        }

        private void createSrcSubModule(Module ideaModule, MutableModuleDescriptor moduleDescriptor) {
            MutableSubModuleDescriptor srcSubModule =
                    new MutableSubModuleDescriptor(moduleDescriptor, Name.special("<production sources>"));
            addPlatformSpecificProviders(ideaModule, srcSubModule, false);
            moduleDescriptor.addSubModule(srcSubModule);

            srcSubModules.put(ideaModule, srcSubModule);
        }

        private void createTestSubModule(Module ideaModule, MutableModuleDescriptor moduleDescriptor) {
            MutableSubModuleDescriptor testSubModule = new MutableSubModuleDescriptor(moduleDescriptor, Name.special("<test sources>"));
            MutableSubModuleDescriptor srcSubModule = srcSubModules.get(ideaModule);
            addPlatformSpecificProviders(ideaModule, testSubModule, true);
            if (srcSubModule != null) {
                testSubModule.addDependency(srcSubModule);
            }

            moduleDescriptor.addSubModule(testSubModule);

            testSubModules.put(ideaModule, testSubModule);
        }

        private void splitSourceFolders(Module ideaModule, Collection<SourceFolder> testFolders, Collection<SourceFolder> srcFolders) {
            ModuleRootManager rootManager = ModuleRootManager.getInstance(ideaModule);
            for (ContentEntry contentEntry : rootManager.getContentEntries()) {
                for (SourceFolder sourceFolder : contentEntry.getSourceFolders()) {
                    if (sourceFolder.isTestSource()) {
                        testFolders.add(sourceFolder);
                    }
                    else {
                        srcFolders.add(sourceFolder);
                    }
                }
            }
        }

        private void createDependencies(Module ideaModule) {
            final List<OrderEntry> sourceDependencies = Lists.newArrayList();
            final List<OrderEntry> testDependencies = Lists.newArrayList();

            OrderEnumerator.orderEntries(ideaModule).recursively().exportedOnly().forEach(
                    new Processor<OrderEntry>() {
                        @Override
                        public boolean process(OrderEntry entry) {
                            if (entry instanceof ExportableOrderEntry) {
                                DependencyScope scope = ((ExportableOrderEntry) entry).getScope();
                                if (scope == DependencyScope.RUNTIME) return true;
                                if (scope != DependencyScope.TEST) {
                                    sourceDependencies.add(entry);
                                }
                                testDependencies.add(entry);

                            }
                            else if (entry instanceof ModuleSourceOrderEntry) {
                                sourceDependencies.add(entry);
                                testDependencies.add(entry);
                            }
                            return true;
                        }
                    }
            );

            createDependenciesForSubModule(srcSubModules.get(ideaModule), sourceDependencies, false);
            createDependenciesForSubModule(testSubModules.get(ideaModule), testDependencies, true);
        }

        private void createDependenciesForSubModule(
                @Nullable MutableSubModuleDescriptor subModule,
                @NotNull List<OrderEntry> dependencies,
                boolean isTestSources
        ) {
            if (subModule == null) return;

            for (OrderEntry dependency : dependencies) {
                if (dependency instanceof ModuleOrderEntry) {
                    ModuleOrderEntry entry = (ModuleOrderEntry) dependency;
                    subModule.addDependency(srcSubModules.get(entry.getModule()));
                    if (isTestSources) {
                        subModule.addDependency(testSubModules.get(entry.getModule()));
                    }
                }
                else if (dependency instanceof ModuleSourceOrderEntry) {
                    subModule.addDependency(SubModuleDescriptor.MY_SOURCE);
                }
                else {
                    if (dependency instanceof LibraryOrderEntry) {
                        LibraryOrderEntry entry = (LibraryOrderEntry) dependency;

                        Library library = entry.getLibrary();
                        if (library == null) continue;

                        subModule.addDependency(
                                getCachedSubModule(
                                        librarySubModules,
                                        library,
                                        stringToSpecialName(library.getName()),
                                        new LibraryScope(project, library)));
                    }
                    else if (dependency instanceof JdkOrderEntry) {
                        JdkOrderEntry entry = (JdkOrderEntry) dependency;

                        Sdk jdk = entry.getJdk();
                        if (jdk == null) continue;

                        subModule.addDependency(
                                getCachedSubModule(
                                        sdkSubModules,
                                        jdk,
                                        stringToSpecialName(jdk.getName()),
                                        LibraryScopeCache.getInstance(project).getScopeForSdk(entry)));
                    }
                }
            }
        }

        @NotNull
        private <K> SubModuleDescriptor getCachedSubModule(
                @NotNull Map<K, MutableSubModuleDescriptor> cache,
                @NotNull K key,
                @NotNull Name name,
                @NotNull GlobalSearchScope searchScope
        ) {
            MutableSubModuleDescriptor subModule = cache.get(key);
            if (subModule == null) {
                MutableModuleDescriptor moduleDescriptor = new MutableModuleDescriptor(name, JavaToKotlinClassMap.getInstance());
                subModule = new MutableSubModuleDescriptor(moduleDescriptor, name);
                createJavaPackageFragmentProvider(subModule, searchScope);
                cache.put(key, subModule);
            }
            return subModule;
        }

        private JavaPackageFragmentProvider createJavaPackageFragmentProvider(
                MutableSubModuleDescriptor subModule,
                GlobalSearchScope allLibrariesScope
        ) {
            InjectorForJavaDescriptorResolver injector = new InjectorForJavaDescriptorResolver(
                    project, trace, classResolutionFacade, storageManager, subModule, allLibrariesScope
            );
            PsiClassFinderImpl psiClassFinder = injector.getPsiClassFinder();
            JavaPackageFragmentProvider provider = new JavaPackageFragmentProvider(
                    classResolutionFacade,
                    trace,
                    storageManager,
                    new PsiDeclarationProviderFactory(psiClassFinder),
                    injector.getJavaClassResolver(),
                    psiClassFinder,
                    subModule
            );
            registerJavaPackageFragmentProvider(subModule, provider);
            return provider;
        }

        private void addPlatformSpecificProviders(Module ideaModule, MutableSubModuleDescriptor srcSubModule, boolean isTestSubModule) {
            if (!JsModuleDetector.isJsModule(ideaModule)) {
                GlobalSearchScope searchScope = getSourcesScope(ideaModule, isTestSubModule);
                registerJavaPackageFragmentProvider(srcSubModule, createJavaPackageFragmentProvider(srcSubModule, searchScope));
            }
        }

        private GlobalSearchScope getSourcesScope(Module ideaModule, boolean isTestSubModule) {
            GlobalSearchScope onlyProductionSources = ideaModule.getModuleScope(false);
            if (isTestSubModule) {
                return ideaModule.getModuleScope(true).intersectWith(GlobalSearchScope.notScope(onlyProductionSources));
            }
            else {
                return onlyProductionSources;
            }
        }
    }

    @NotNull
    private static PlatformToKotlinClassMap getClassMap(@NotNull Module ideaModule) {
        if (JsModuleDetector.isJsModule(ideaModule)) {
            return PlatformToKotlinClassMap.EMPTY;
        }
        else {
            return JavaToKotlinClassMap.getInstance();
        }
    }

    @NotNull
    private static Name stringToSpecialName(@Nullable String name) {
        return Name.special("<" + name + ">");
    }

    private static class KotlinModules {
        private final ImmutableMap<Module, ModuleDescriptor> moduleDescriptors;
        private final MutableModuleSourcesManager moduleSourcesManager;
        private final BindingContext bindingContext;

        private KotlinModules(
                Map<Module, ModuleDescriptor> descriptors,
                MutableModuleSourcesManager moduleSourcesManager,
                BindingContext bindingContext
        ) {
            this.moduleDescriptors = ImmutableMap.copyOf(descriptors);
            this.moduleSourcesManager = moduleSourcesManager;
            this.bindingContext = bindingContext;
        }

        @NotNull
        public Collection<ModuleDescriptor> getModuleDescriptors() {
            //noinspection unchecked
            return (Collection) moduleDescriptors.values();
        }

    }
}
