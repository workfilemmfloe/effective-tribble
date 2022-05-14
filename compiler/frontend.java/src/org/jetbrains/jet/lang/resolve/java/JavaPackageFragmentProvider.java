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

package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Lists;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.util.NotNullFunction;
import com.intellij.util.NullableFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.PackageLikeDescriptorBase;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.java.kt.JetPackageClassAnnotation;
import org.jetbrains.jet.lang.resolve.java.provider.PsiDeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.java.scope.JavaClassStaticMembersScope;
import org.jetbrains.jet.lang.resolve.java.scope.JavaPackageScopeWithoutMembers;
import org.jetbrains.jet.lang.resolve.java.scope.JavaScopeForKotlinNamespace;
import org.jetbrains.jet.lang.resolve.lazy.storage.MemoizedFunctionToNullable;
import org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class JavaPackageFragmentProvider implements PackageFragmentProvider {

    public static final PackageFragmentKind JAVA = new PackageFragmentKind() {
        @Override
        public String toString() {
            return "JAVA";
        }
    };

    private final BindingTrace trace;
    private final PsiDeclarationProviderFactory declarationProviderFactory;
    private final PsiClassFinder psiClassFinder;
    private final JavaDescriptorResolver javaDescriptorResolver;
    private final SubModuleDescriptor subModule;
    private final MemoizedFunctionToNullable<FqName, PackageFragmentDescriptor> packageFragments;

    public JavaPackageFragmentProvider(
            @NotNull BindingTrace trace,
            @NotNull StorageManager storageManager,
            @NotNull PsiDeclarationProviderFactory declarationProviderFactory,
            @NotNull JavaDescriptorResolver javaDescriptorResolver,
            @NotNull PsiClassFinder psiClassFinder,
            @NotNull SubModuleDescriptor subModule
    ) {
        this.trace = trace;
        this.javaDescriptorResolver = javaDescriptorResolver;
        this.packageFragments = storageManager.createMemoizedFunctionWithNullableValues(
                new NullableFunction<FqName, PackageFragmentDescriptor>() {
                    @Nullable
                    @Override
                    public PackageFragmentDescriptor fun(FqName fqName) {
                        return createPackageFragment(fqName);
                    }
                },
                StorageManager.ReferenceKind.STRONG
        );
        this.declarationProviderFactory = declarationProviderFactory;
        this.psiClassFinder = psiClassFinder;
        this.subModule = subModule;
    }

    public boolean isResponsibleFor(@NotNull VirtualFile virtualFile) {
        return psiClassFinder.getDefiningSearchScope().contains(virtualFile);
    }

    @Nullable
    public ClassDescriptor getClassDescriptor(@NotNull PsiClass psiClass) {
        assertInMyScope(psiClass);
        return javaDescriptorResolver.resolveClass(psiClass);
    }

    protected void assertInMyScope(@NotNull PsiClass psiClass) {
        VirtualFile file = psiClass.getContainingFile().getVirtualFile();
        assert file != null : "No virtual file for psiClass: " + psiClass.getText();
        assert isResponsibleFor(file) : "Not in scope\n psiClass " + psiClass.getText() + "\nscope: " + psiClassFinder;
    }

    @NotNull
    @Override
    public List<PackageFragmentDescriptor> getPackageFragments(@NotNull FqName fqName) {
        PackageFragmentDescriptor fragment = getPackageFragment(fqName);
        if (fragment == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(fragment);
    }

    @NotNull
    @Override
    public Collection<FqName> getSubPackagesOf(@NotNull FqName fqName) {
        PsiPackage psiPackage = psiClassFinder.findPsiPackage(fqName);
        if (psiPackage == null) {
            return Collections.emptyList();
        }
        PsiPackage[] subPackages = psiPackage.getSubPackages(psiClassFinder.getDefiningSearchScope());
        Collection<FqName> result = Lists.newArrayList();
        for (PsiPackage subPackage : subPackages) {
            result.add(new FqName(subPackage.getQualifiedName()));
        }
        return result;
    }

    // This particular provider creates no more than one fragment per package
    @Nullable
    public PackageFragmentDescriptor getPackageFragment(@NotNull FqName fqName) {
        return packageFragments.fun(fqName);
    }

    @Nullable
    private PackageFragmentDescriptor createPackageFragment(@NotNull FqName fqName) {
        final PsiClass staticClass = psiClassFinder.findPsiClass(fqName);
        if (staticClass != null) {
            if (staticClass.isEnum()) {
                // TODO: this is a bug. but reproduces the existing behavior
                // http://youtrack.jetbrains.com/issue/KT-3377

                // old comment:
                // NOTE: we don't want to create namespace for enum classes because we put
                // static members of enum class into class object descriptor
                return null;
            }

            return createPackageFragmentForStaticClass(fqName, staticClass);
        }

        final PsiPackage psiPackage = psiClassFinder.findPsiPackage(fqName);
        if (psiPackage == null) return null;

        final PsiClass packageClass = psiClassFinder.findPsiClass(PackageClassUtils.getPackageClassFqName(fqName));

        if (packageClass == null) {
            return createPackageFragmentForPackageWithoutMembers(fqName, psiPackage);
        }

        AbiVersionUtil.checkAbiVersion(packageClass, JetPackageClassAnnotation.get(packageClass), trace);
        return createPackageFragmentForPackageWithMembers(fqName, psiPackage, packageClass);
    }

    private PackageFragmentDescriptor createPackageFragmentForStaticClass(final FqName fqName, final PsiClass staticClass) {
        JavaPackageFragment fragment = new JavaPackageFragment(
                "static class",
                subModule, fqName,
                new NotNullFunction<PackageFragmentDescriptor, JetScope>() {
                   @NotNull
                   @Override
                   public JetScope fun(PackageFragmentDescriptor fragment) {
                       return new JavaClassStaticMembersScope(
                               fragment,
                               declarationProviderFactory
                                       .createDeclarationProviderForClassStaticMembers(
                                               staticClass),
                               fqName,
                               javaDescriptorResolver);
                   }
               });
        trace.record(JavaBindingContext.JAVA_STATIC_CLASS_FOR_PACKAGE, fragment, staticClass);
        return fragment;
    }

    private PackageFragmentDescriptor createPackageFragmentForPackageWithoutMembers(
            final FqName fqName,
            final PsiPackage psiPackage
    ) {
        return new JavaPackageFragment(
                "simple package",
                subModule, fqName,
                new NotNullFunction<PackageFragmentDescriptor, JetScope>() {
                   @NotNull
                   @Override
                   public JetScope fun(PackageFragmentDescriptor fragment) {
                       return new JavaPackageScopeWithoutMembers(
                                           fragment,
                                           declarationProviderFactory.createDeclarationProviderForNamespaceWithoutMembers(
                                                   psiPackage),
                                           fqName, javaDescriptorResolver);
                   }
               });
    }

    private PackageFragmentDescriptor createPackageFragmentForPackageWithMembers(
            final FqName fqName,
            final PsiPackage psiPackage,
            final PsiClass packageClass
    ) {
        return new JavaPackageFragment(
                "package class (from Kotlin)",
                subModule, fqName,
                new NotNullFunction<PackageFragmentDescriptor, JetScope>() {
                   @NotNull
                   @Override
                   public JetScope fun(PackageFragmentDescriptor fragment) {
                       return new JavaScopeForKotlinNamespace(
                               fragment,
                               declarationProviderFactory.createDeclarationForKotlinNamespace(
                                       psiPackage, packageClass),
                               fqName, javaDescriptorResolver);
                   }
               });
    }

    private static class JavaPackageFragment extends PackageLikeDescriptorBase implements PackageFragmentDescriptor {

        private final String debugName;
        private final SubModuleDescriptor subModule;
        private final JetScope memberScope;

        public JavaPackageFragment(
                @NotNull String debugName,
                @NotNull SubModuleDescriptor subModule,
                @NotNull FqName fqName,
                @NotNull NotNullFunction<PackageFragmentDescriptor, JetScope> memberScope
        ) {
            super(fqName);
            this.debugName = debugName;
            this.subModule = subModule;
            this.memberScope = memberScope.fun(this);
        }

        @NotNull
        @Override
        public PackageFragmentKind getKind() {
            return JAVA;
        }

        @NotNull
        @Override
        public SubModuleDescriptor getContainingDeclaration() {
            return subModule;
        }

        @NotNull
        @Override
        public JetScope getMemberScope() {
            return memberScope;
        }

        @Override
        public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
            return visitor.visitPackageFragmentDescriptor(this, data);
        }

        @Override
        public String toString() {
            return "[" + debugName + "]" + super.toString();
        }
    }
}
