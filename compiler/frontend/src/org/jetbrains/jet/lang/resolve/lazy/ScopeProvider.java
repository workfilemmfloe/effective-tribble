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

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.NotNullFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.descriptors.SubModuleDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.TemporaryBindingTrace;
import org.jetbrains.jet.lang.resolve.lazy.descriptors.LazyClassDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.storage.MemoizedFunctionToNotNull;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.ChainedScope;
import org.jetbrains.jet.lang.resolve.scopes.InnerClassesScopeWrapper;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager.ReferenceKind.STRONG;
import static org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager.ReferenceKind.WEAK;

public class ScopeProvider {
    private final LazyCodeAnalyzer analyzer;

    private final MemoizedFunctionToNotNull<JetFile, JetScope> fileScopes;

    private final MemoizedFunctionToNotNull<SubModuleDescriptor, JetScope> defaultImportsScope;

    public ScopeProvider(@NotNull LazyCodeAnalyzer analyzer) {
        this.analyzer = analyzer;

        this.fileScopes = analyzer.getStorageManager().createMemoizedFunction(new Function<JetFile, JetScope>() {
            @Override
            public JetScope fun(@NotNull JetFile file) {
                return createFileScope(file);
            }
        }, WEAK);

        this.defaultImportsScope = analyzer.getStorageManager().createMemoizedFunction(new NotNullFunction<SubModuleDescriptor, JetScope>() {
            @NotNull
            @Override
            public JetScope fun(SubModuleDescriptor subModuleDescriptor) {
                return createScopeWithDefaultImports(subModuleDescriptor);
            }
        }, STRONG);
    }

    @NotNull
    public JetScope getFileScope(final JetFile file) {
        return fileScopes.fun(file);
    }

    private JetScope createFileScope(JetFile file) {
        SubModuleDescriptor subModule = analyzer.getModuleSourcesManager().getSubModuleForFile(file);

        PackageViewDescriptor rootPackageDescriptor = DescriptorUtils.getRootPackage(subModule);

        PackageViewDescriptor packageDescriptor = getFilePackageDescriptor(file);

        JetScope importsScope = LazyImportScope.createImportScopeForFile(
                analyzer,
                subModule,
                packageDescriptor,
                file,
                analyzer.getTrace(),
                "Lazy Imports Scope for file " + file.getName());

        return new ChainedScope(analyzer.getPackageFragment(packageDescriptor.getFqName()),
                                "File scope: " + file.getName(),
                                rootPackageDescriptor.getMemberScope(),
                                packageDescriptor.getMemberScope(),
                                importsScope,
                                defaultImportsScope.fun(subModule));
    }

    private JetScope createScopeWithDefaultImports(@NotNull SubModuleDescriptor subModuleDescriptor) {
        PackageViewDescriptor rootPackageDescriptor = DescriptorUtils.getRootPackage(subModuleDescriptor);

        JetImportsFactory importsFactory = analyzer.getInjector().getJetImportsFactory();
        List<ImportPath> defaultImports = subModuleDescriptor.getDefaultImports();

        Collection<JetImportDirective> defaultImportDirectives = importsFactory.createImportDirectives(defaultImports);

        return LazyImportScope.createImportScope(
                analyzer,
                subModuleDescriptor,
                rootPackageDescriptor,
                Lists.newArrayList(defaultImportDirectives),
                TemporaryBindingTrace.create(analyzer.getTrace(), "Transient trace for default imports lazy resolve"),
                "Lazy default imports scope");
    }

    @NotNull
    private PackageViewDescriptor getFilePackageDescriptor(@NotNull JetFile file) {
        JetNamespaceHeader header = file.getNamespaceHeader();
        if (header == null) {
            throw new IllegalArgumentException("Scripts are not supported: " + file.getName());
        }

        FqName fqName = new FqName(header.getQualifiedName());

        PackageViewDescriptor packageDescriptor = analyzer.getModuleSourcesManager().getSubModuleForFile(file).getPackageView(fqName);

        if (packageDescriptor == null) {
            throw new IllegalStateException("Package not found: " + fqName + " maybe the file is not in scope of this resolve session: " + file.getName());
        }

        return packageDescriptor;
    }

    @NotNull
    public JetScope getResolutionScopeForDeclaration(@NotNull PsiElement elementOfDeclaration) {
        JetDeclaration jetDeclaration = PsiTreeUtil.getParentOfType(elementOfDeclaration, JetDeclaration.class, false);

        assert !(elementOfDeclaration instanceof JetDeclaration) || jetDeclaration == elementOfDeclaration :
                "For JetDeclaration element getParentOfType() should return itself.";

        JetDeclaration parentDeclaration = PsiTreeUtil.getParentOfType(jetDeclaration, JetDeclaration.class);
        if (parentDeclaration == null) {
            return getFileScope((JetFile) elementOfDeclaration.getContainingFile());
        }

        assert jetDeclaration != null : "Can't happen because of getParentOfType(null, ?) == null";

        if (parentDeclaration instanceof JetClassOrObject) {
            JetClassOrObject classOrObject = (JetClassOrObject) parentDeclaration;
            LazyClassDescriptor classDescriptor = (LazyClassDescriptor) analyzer.getDescriptor(classOrObject);
            if (jetDeclaration instanceof JetClassInitializer || jetDeclaration instanceof JetProperty) {
                return classDescriptor.getScopeForPropertyInitializerResolution();
            }
            if (jetDeclaration instanceof JetEnumEntry) {
                return ((LazyClassDescriptor) classDescriptor.getClassObjectDescriptor()).getScopeForMemberDeclarationResolution();
            }
            return classDescriptor.getScopeForMemberDeclarationResolution();
        }

        if (parentDeclaration instanceof JetClassObject) {
            assert jetDeclaration instanceof JetObjectDeclaration : "Should be situation for getting scope for object in class [object {...}]";

            JetClassObject classObject = (JetClassObject) parentDeclaration;
            LazyClassDescriptor classObjectDescriptor =
                    (LazyClassDescriptor) analyzer.getClassObjectDescriptor(classObject).getContainingDeclaration();

            // During class object header resolve there should be no resolution for parent class generic params
            return new InnerClassesScopeWrapper(classObjectDescriptor.getScopeForMemberDeclarationResolution());
        }

        throw new IllegalStateException("Don't call this method for local declarations: " + jetDeclaration + " " + jetDeclaration.getText());
    }
}