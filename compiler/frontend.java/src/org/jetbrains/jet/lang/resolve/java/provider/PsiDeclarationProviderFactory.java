/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.provider;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

//TODO: remove class
public final class PsiDeclarationProviderFactory {
    private PsiDeclarationProviderFactory() {
    }

    @NotNull
    public static ClassPsiDeclarationProvider createSyntheticClassObjectClassData(@NotNull PsiClass psiClass) {
        return createDeclarationProviderForClassStaticMembers(psiClass);
    }

    @NotNull
    public static ClassPsiDeclarationProvider createBinaryClassData(@NotNull PsiClass psiClass) {
        return new ClassPsiDeclarationProviderImpl(psiClass, false);
    }

    @NotNull
    public static KotlinNamespacePsiDeclarationProvider createDeclarationForKotlinNamespace(
            @NotNull PsiPackage psiPackage,
            @NotNull PsiClass psiClass,
            @NotNull GlobalSearchScope searchScope
    ) {
        return new KotlinNamespacePsiDeclarationProvider(psiPackage, psiClass, searchScope);
    }

    @NotNull
    public static PackagePsiDeclarationProviderImpl createDeclarationProviderForNamespaceWithoutMembers(
            @NotNull PsiPackage psiPackage,
            @NotNull GlobalSearchScope searchScope
    ) {
        return new PackagePsiDeclarationProviderImpl(psiPackage, searchScope);
    }

    @NotNull
    public static ClassPsiDeclarationProvider createDeclarationProviderForClassStaticMembers(@NotNull PsiClass psiClass) {
        return new ClassPsiDeclarationProviderImpl(psiClass, true);
    }
}
