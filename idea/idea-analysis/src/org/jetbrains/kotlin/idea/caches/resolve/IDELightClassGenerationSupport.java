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

package org.jetbrains.kotlin.idea.caches.resolve;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.ClassFileViewProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub;
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.PsiClassHolderFileStub;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.cls.ClsFormatException;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.*;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.idea.decompiler.navigation.JetSourceNavigationHelper;
import org.jetbrains.kotlin.idea.project.ResolveSessionForBodies;
import org.jetbrains.kotlin.idea.stubindex.JetFullClassNameIndex;
import org.jetbrains.kotlin.idea.stubindex.JetTopLevelClassByPackageIndex;
import org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil;
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil;
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer;

import java.io.IOException;
import java.util.*;

import static kotlin.KotlinPackage.*;
import static org.jetbrains.kotlin.idea.stubindex.JetSourceFilterScope.kotlinSourceAndClassFiles;

public class IDELightClassGenerationSupport extends LightClassGenerationSupport {

    private static final Logger LOG = Logger.getInstance(IDELightClassGenerationSupport.class);

    private final Project project;

    private final Comparator<JetFile> jetFileComparator;
    private final PsiManager psiManager;

    public IDELightClassGenerationSupport(@NotNull Project project) {
        this.project = project;
        this.jetFileComparator = byScopeComparator(GlobalSearchScope.allScope(project));
        this.psiManager = PsiManager.getInstance(project);
    }


    @NotNull
    @Override
    public LightClassConstructionContext getContextForPackage(@NotNull Collection<JetFile> files) {
        assert !files.isEmpty() : "No files in package";

        List<JetFile> sortedFiles = new ArrayList<JetFile>(files);
        Collections.sort(sortedFiles, jetFileComparator);

        JetFile file = sortedFiles.get(0);
        ResolveSessionForBodies session = KotlinCacheService.OBJECT$.getInstance(file.getProject()).getLazyResolveSession(file);
        forceResolvePackageDeclarations(files, session);
        return new LightClassConstructionContext(session.getBindingContext(), session.getModuleDescriptor());
    }

    @NotNull
    @Override
    public LightClassConstructionContext getContextForClassOrObject(@NotNull JetClassOrObject classOrObject) {
        ResolveSessionForBodies session =
                KotlinCacheService.OBJECT$.getInstance(classOrObject.getProject()).getLazyResolveSession(classOrObject);

        if (classOrObject.isLocal()) {
            BindingContext bindingContext = session.resolveToElement(classOrObject, BodyResolveMode.FULL);
            ClassDescriptor descriptor = bindingContext.get(BindingContext.CLASS, classOrObject);

            if (descriptor == null) {
                LOG.warn("No class descriptor in context for class: " + JetPsiUtil.getElementTextWithContext(classOrObject));
                return new LightClassConstructionContext(bindingContext, session.getModuleDescriptor());
            }

            ForceResolveUtil.forceResolveAllContents(descriptor);

            return new LightClassConstructionContext(bindingContext, session.getModuleDescriptor());
        }

        ForceResolveUtil.forceResolveAllContents(session.getClassDescriptor(classOrObject));
        return new LightClassConstructionContext(session.getBindingContext(), session.getModuleDescriptor());
    }

    private static void forceResolvePackageDeclarations(@NotNull Collection<JetFile> files, @NotNull KotlinCodeAnalyzer session) {
        for (JetFile file : files) {
            // SCRIPT: not supported
            if (file.isScript()) continue;

            FqName packageFqName = file.getPackageFqName();

            // make sure we create a package descriptor
            PackageViewDescriptor packageDescriptor = session.getModuleDescriptor().getPackage(packageFqName);
            if (packageDescriptor == null) {
                LOG.warn("No descriptor found for package " + packageFqName + " in file " + file.getName() + "\n" + file.getText());
                session.forceResolveAll();
                continue;
            }

            for (JetDeclaration declaration : file.getDeclarations()) {
                if (declaration instanceof JetFunction) {
                    JetFunction jetFunction = (JetFunction) declaration;
                    Name name = jetFunction.getNameAsSafeName();
                    Collection<FunctionDescriptor> functions = packageDescriptor.getMemberScope().getFunctions(name);
                    for (FunctionDescriptor descriptor : functions) {
                        ForceResolveUtil.forceResolveAllContents(descriptor);
                    }
                }
                else if (declaration instanceof JetProperty) {
                    JetProperty jetProperty = (JetProperty) declaration;
                    Name name = jetProperty.getNameAsSafeName();
                    Collection<VariableDescriptor> properties = packageDescriptor.getMemberScope().getProperties(name);
                    for (VariableDescriptor descriptor : properties) {
                        ForceResolveUtil.forceResolveAllContents(descriptor);
                    }
                }
                else if (declaration instanceof JetClassOrObject) {
                    // Do nothing: we are not interested in classes
                }
                else {
                    LOG.error("Unsupported declaration kind: " + declaration + " in file " + file.getName() + "\n" + file.getText());
                }
            }
        }
    }

    @NotNull
    @Override
    public Collection<JetClassOrObject> findClassOrObjectDeclarations(@NotNull FqName fqName, @NotNull GlobalSearchScope searchScope) {
        return JetFullClassNameIndex.getInstance().get(fqName.asString(), project, kotlinSourceAndClassFiles(searchScope, project));
    }

    @NotNull
    @Override
    public Collection<JetFile> findFilesForPackage(@NotNull FqName fqName, @NotNull GlobalSearchScope searchScope) {
        return PackageIndexUtil.findFilesWithExactPackage(fqName, kotlinSourceAndClassFiles(searchScope, project), project);
    }

    @NotNull
    private static Map<IdeaModuleInfo, List<JetFile>> groupByModuleInfo(@NotNull Collection<JetFile> allFiles) {
        return KotlinPackage.groupByTo(
                allFiles,
                new LinkedHashMap<IdeaModuleInfo, List<JetFile>>(),
                new Function1<JetFile, IdeaModuleInfo>() {
                    @Override
                    public IdeaModuleInfo invoke(JetFile file) {
                        return ResolvePackage.getModuleInfo(file);
                    }
                });
    }

    @NotNull
    @Override
    public Collection<JetClassOrObject> findClassOrObjectDeclarationsInPackage(
            @NotNull FqName packageFqName, @NotNull GlobalSearchScope searchScope
    ) {
        return JetTopLevelClassByPackageIndex.getInstance().get(
                packageFqName.asString(), project, kotlinSourceAndClassFiles(searchScope, project)
        );
    }

    @Override
    public boolean packageExists(@NotNull FqName fqName, @NotNull GlobalSearchScope scope) {
        return PackageIndexUtil.packageExists(fqName, kotlinSourceAndClassFiles(scope, project), project);
    }

    @NotNull
    @Override
    public Collection<FqName> getSubPackages(@NotNull FqName fqn, @NotNull GlobalSearchScope scope) {
        return PackageIndexUtil.getSubPackageFqNames(fqn, kotlinSourceAndClassFiles(scope, project), project);
    }

    @Nullable
    @Override
    public PsiClass getPsiClass(@NotNull JetClassOrObject classOrObject) {
        VirtualFile virtualFile = classOrObject.getContainingFile().getVirtualFile();
        if (virtualFile != null && LibraryUtil.findLibraryEntry(virtualFile, classOrObject.getProject()) != null) {
            if (ProjectRootsUtil.isLibraryClassFile(project, virtualFile)) {
                return getLightClassForDecompiledClassOrObject(classOrObject);
            }
            return JetSourceNavigationHelper.getOriginalClass(classOrObject);
        }
        return KotlinLightClassForExplicitDeclaration.create(psiManager, classOrObject);
    }

    @Nullable
    private static PsiClass getLightClassForDecompiledClassOrObject(@NotNull JetClassOrObject decompiledClassOrObject) {
        JetFile containingJetFile = decompiledClassOrObject.getContainingJetFile();
        if (!containingJetFile.isCompiled()) {
            return null;
        }
        PsiClass rootLightClassForDecompiledFile = createLightClassForDecompiledKotlinFile(containingJetFile);
        if (rootLightClassForDecompiledFile == null) return null;

        return findCorrespondingLightClass(decompiledClassOrObject, rootLightClassForDecompiledFile);
    }

    @NotNull
    private static PsiClass findCorrespondingLightClass(
            @NotNull JetClassOrObject decompiledClassOrObject,
            @NotNull PsiClass rootLightClassForDecompiledFile
    ) {
        List<Name> relativeClassNameSegments = getClassRelativeName(decompiledClassOrObject).pathSegments();
        Iterator<Name> iterator = relativeClassNameSegments.iterator();
        Name base = iterator.next();
        assert rootLightClassForDecompiledFile.getName().equals(base.asString())
                : "Light class for file:\n" + decompiledClassOrObject.getContainingJetFile().getVirtualFile().getCanonicalPath()
                  + "\nwas expected to have name: " + base.asString() + "\n Actual: " + rootLightClassForDecompiledFile.getName();
        PsiClass current = rootLightClassForDecompiledFile;
        while (iterator.hasNext()) {
            Name name = iterator.next();
            PsiClass innerClass = current.findInnerClassByName(name.asString(), false);
            assert innerClass != null : "Inner class should be found";
            current = innerClass;
        }
        return current;
    }

    @NotNull
    private static FqName getClassRelativeName(@NotNull JetClassOrObject decompiledClassOrObject) {
        Name name = decompiledClassOrObject.getNameAsName();
        if (name == null) {
            assert decompiledClassOrObject instanceof JetObjectDeclaration &&
                   ((JetObjectDeclaration) decompiledClassOrObject).isClassObject();
            name = Name.identifier(JvmAbi.CLASS_OBJECT_CLASS_NAME);
        }
        JetClassOrObject parent = PsiTreeUtil.getParentOfType(decompiledClassOrObject, JetClassOrObject.class, true);
        if (parent == null) {
            assert decompiledClassOrObject.isTopLevel();
            return FqName.topLevel(name);
        }
        return getClassRelativeName(parent).child(name);
    }

    @NotNull
    @Override
    public Collection<PsiClass> getPackageClasses(@NotNull FqName packageFqName, @NotNull GlobalSearchScope scope) {
        List<PsiClass> result = new ArrayList<PsiClass>();
        List<KotlinLightPackageClassInfo> packageClassesInfos = findPackageClassesInfos(packageFqName, scope);
        for (KotlinLightPackageClassInfo info : packageClassesInfos) {
            Collection<JetFile> files = info.getFiles();
            List<JetFile> filesWithCallables = PackagePartClassUtils.getPackageFilesWithCallables(files);
            if (filesWithCallables.isEmpty()) continue;

            IdeaModuleInfo moduleInfo = info.getModuleInfo();
            if (moduleInfo instanceof ModuleSourceInfo) {
                KotlinLightClassForPackage lightClass =
                        KotlinLightClassForPackage.create(psiManager, packageFqName, moduleInfo.contentScope(), files);
                if (lightClass == null) continue;

                result.add(lightClass);

                if (files.size() > 1) {
                    for (JetFile file : files) {
                        result.add(new FakeLightClassForFileOfPackage(psiManager, lightClass, file));
                    }
                }
            }
            else {
                PsiClass clsClass = getLightClassForDecompiledPackage(packageFqName, filesWithCallables);
                if (clsClass != null) {
                    result.add(clsClass);
                }
            }
        }
        return result;
    }

    @Nullable
    private static PsiClass getLightClassForDecompiledPackage(@NotNull FqName packageFqName, @NotNull List<JetFile> filesWithCallables) {
        JetFile firstFile = filesWithCallables.iterator().next();
        if (firstFile.isCompiled()) {
            if (filesWithCallables.size() > 1) {
                LOG.error("Several files with callables for package: " + packageFqName);
            }
            return createLightClassForDecompiledKotlinFile(firstFile);
        }
        return null;
    }

    @NotNull
    private List<KotlinLightPackageClassInfo> findPackageClassesInfos(
            @NotNull FqName fqName, @NotNull GlobalSearchScope wholeScope
    ) {
        Collection<JetFile> allFiles = findFilesForPackage(fqName, wholeScope);
        Map<IdeaModuleInfo, List<JetFile>> filesByInfo = groupByModuleInfo(allFiles);
        List<KotlinLightPackageClassInfo> result = new ArrayList<KotlinLightPackageClassInfo>();
        for (Map.Entry<IdeaModuleInfo, List<JetFile>> entry : filesByInfo.entrySet()) {
            result.add(new KotlinLightPackageClassInfo(entry.getValue(), entry.getKey()));
        }
        sortByClasspath(wholeScope, result);
        return result;
    }

    @NotNull
    private static Comparator<JetFile> byScopeComparator(@NotNull final GlobalSearchScope searchScope) {
        return new Comparator<JetFile>() {
            @Override
            public int compare(@NotNull JetFile o1, @NotNull JetFile o2) {
                VirtualFile f1 = o1.getVirtualFile();
                VirtualFile f2 = o2.getVirtualFile();
                if (f1 == f2) return 0;
                if (f1 == null) return -1;
                if (f2 == null) return 1;
                return searchScope.compare(f1, f2);
            }
        };
    }

    private static void sortByClasspath(@NotNull GlobalSearchScope wholeScope, @NotNull List<KotlinLightPackageClassInfo> result) {
        final Comparator<JetFile> byScopeComparator = byScopeComparator(wholeScope);
        Collections.sort(result, new Comparator<KotlinLightPackageClassInfo>() {
            @Override
            public int compare(@NotNull KotlinLightPackageClassInfo info1, @NotNull KotlinLightPackageClassInfo info2) {
                JetFile file1 = info1.getFiles().iterator().next();
                JetFile file2 = info2.getFiles().iterator().next();
                //classes earlier that would appear earlier on classpath should go first
                return -byScopeComparator.compare(file1, file2);
            }
        });
    }

    private static final class KotlinLightPackageClassInfo {
        private final Collection<JetFile> files;
        private final IdeaModuleInfo moduleInfo;

        public KotlinLightPackageClassInfo(@NotNull Collection<JetFile> files, @NotNull IdeaModuleInfo moduleInfo) {
            this.files = files;
            this.moduleInfo = moduleInfo;
        }

        @NotNull
        public Collection<JetFile> getFiles() {
            return files;
        }

        @NotNull
        public IdeaModuleInfo getModuleInfo() {
            return moduleInfo;
        }
    }

    @Nullable
    private static KotlinLightClassForDecompiledDeclaration createLightClassForDecompiledKotlinFile(@NotNull JetFile file) {
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }

        ClsClassImpl javaClsClass = createClsJavaClassFromVirtualFile(file, virtualFile);
        if (javaClsClass == null) {
            return null;
        }
        JetClassOrObject declaration = singleOrNull(filterIsInstance(file.getDeclarations(), JetClassOrObject.class));
        return new KotlinLightClassForDecompiledDeclaration(javaClsClass, declaration);
    }

    @Nullable
    private static ClsClassImpl createClsJavaClassFromVirtualFile(
            @NotNull final JetFile decompiledKotlinFile,
            @NotNull VirtualFile virtualFile
    ) {
        final PsiJavaFileStubImpl javaFileStub = getOrCreateJavaFileStub(virtualFile);
        if (javaFileStub == null) {
            return null;
        }
        PsiManager manager = PsiManager.getInstance(decompiledKotlinFile.getProject());
        ClsFileImpl fakeFile = new ClsFileImpl((PsiManagerImpl) manager, new ClassFileViewProvider(manager, virtualFile)) {
            @NotNull
            @Override
            public PsiClassHolderFileStub getStub() {
                return javaFileStub;
            }

            @Override
            public PsiElement getMirror() {
                return decompiledKotlinFile;
            }
        };
        fakeFile.setPhysical(false);
        javaFileStub.setPsi(fakeFile);
        return (ClsClassImpl) single(fakeFile.getClasses());
    }

    private final static Key<CachedJavaStub> cachedJavaStubKey = Key.create("CACHED_JAVA_STUB");

    private static class CachedJavaStub {
        public CachedJavaStub(long modificationStamp, @NotNull PsiJavaFileStubImpl javaFileStub) {
            this.modificationStamp = modificationStamp;
            this.javaFileStub = javaFileStub;
        }

        public long modificationStamp;
        public PsiJavaFileStubImpl javaFileStub;
    }

    @Nullable
    private static PsiJavaFileStubImpl getOrCreateJavaFileStub(@NotNull VirtualFile virtualFile) {
        CachedJavaStub cachedJavaStub = virtualFile.getUserData(cachedJavaStubKey);
        long fileModificationStamp = virtualFile.getModificationStamp();
        if (cachedJavaStub != null && cachedJavaStub.modificationStamp == fileModificationStamp) {
            return cachedJavaStub.javaFileStub;
        }
        PsiJavaFileStubImpl stub = (PsiJavaFileStubImpl) createStub(virtualFile);
        if (stub != null) {
            virtualFile.putUserData(cachedJavaStubKey, new CachedJavaStub(fileModificationStamp, stub));
        }
        return stub;
    }

    @Nullable
    private static PsiJavaFileStub createStub(@NotNull VirtualFile file) {
        try {
            return ClsFileImpl.buildFileStub(file, file.contentsToByteArray());
        }
        catch (ClsFormatException e) {
            LOG.debug(e);
        }
        catch (IOException e) {
            LOG.debug(e);
        }
        LOG.error("Failed to build java cls class for " + file.getCanonicalPath());
        return null;
    }
}
