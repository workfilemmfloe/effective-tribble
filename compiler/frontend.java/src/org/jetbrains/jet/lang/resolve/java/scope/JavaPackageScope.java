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

package org.jetbrains.jet.lang.resolve.java.scope;

import com.google.common.collect.Sets;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.provider.PackagePsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;

public abstract class JavaPackageScope extends JavaBaseScope {

    @NotNull
    private final PackagePsiDeclarationProvider declarationProvider;
    @NotNull
    private final FqName packageFQN;

    protected JavaPackageScope(
            @NotNull PackageFragmentDescriptor descriptor,
            @NotNull PackagePsiDeclarationProvider declarationProvider,
            @NotNull FqName packageFQN,
            @NotNull JavaDescriptorResolver javaDescriptorResolver
    ) {
        super(descriptor, javaDescriptorResolver, declarationProvider);
        this.declarationProvider = declarationProvider;
        this.packageFQN = packageFQN;
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        PsiClass psiClass = declarationProvider.getPsiClass(name);
        if (psiClass == null) return null;

        ClassDescriptor classDescriptor = javaDescriptorResolver.resolveClass(psiClass, DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
        if (classDescriptor == null || classDescriptor.getKind().isObject()) {
            return null;
        }
        return classDescriptor;
    }

    @Override
    public ClassDescriptor getObjectDescriptor(@NotNull Name name) {
        PsiClass psiClass = declarationProvider.getPsiClass(name);
        if (psiClass == null) return null;

        ClassDescriptor classDescriptor = javaDescriptorResolver.resolveClass(psiClass, DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
        if (classDescriptor != null && classDescriptor.getKind().isObject()) {
            return classDescriptor;
        }
        return null;
    }

    @Override
    public PackageViewDescriptor getPackage(@NotNull Name name) {
        return null;
    }

    @NotNull
    @Override
    protected Collection<DeclarationDescriptor> computeAllDescriptors() {
        Collection<DeclarationDescriptor> result = Sets.newLinkedHashSet(super.computeAllDescriptors());

        for (PsiClass psiClass : declarationProvider.getAllPsiClasses()) {
            if (PackageClassUtils.isPackageClass(psiClass)) {
                continue;
            }

            if (DescriptorResolverUtils.isKotlinLightClass(psiClass)) {
                continue;
            }

            if (psiClass.hasModifierProperty(PsiModifier.PUBLIC)) {
                ProgressIndicatorProvider.checkCanceled();
                ClassDescriptor classDescriptor = javaDescriptorResolver.getClassDescriptor(psiClass);
                if (classDescriptor != null) {
                    result.add(classDescriptor);
                }
            }
        }

        return result;
    }
}
