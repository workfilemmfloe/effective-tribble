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

package org.jetbrains.jet.lang.resolve.lazy.declarations;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import kotlin.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPackageDirective;
import org.jetbrains.jet.lang.resolve.lazy.data.JetClassLikeInfo;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.storage.NotNullLazyValue;
import org.jetbrains.jet.storage.StorageManager;

import java.util.Collection;
import java.util.Set;

public class FileBasedDeclarationProviderFactory extends AbstractDeclarationProviderFactory  {

    private static class Index {
        private final Multimap<FqName, JetFile> filesByPackage = HashMultimap.create();
        private final Set<FqName> declaredPackages = Sets.newHashSet();
    }

    private final StorageManager storageManager;
    private final NotNullLazyValue<Index> index;

    public FileBasedDeclarationProviderFactory(@NotNull StorageManager storageManager, @NotNull final Collection<JetFile> files) {
        super(storageManager);
        this.storageManager = storageManager;
        this.index = storageManager.createLazyValue(new Function0<Index>() {
            @Override
            public Index invoke() {
                return computeFilesByPackage(files);
            }
        });
    }

    @NotNull
    private static Index computeFilesByPackage(@NotNull Collection<JetFile> files) {
        Index index = new Index();
        for (JetFile file : files) {
            FqName packageFqName = file.getPackageFqName();
            addMeAndParentPackages(index, packageFqName);
            index.filesByPackage.put(packageFqName, file);
        }
        return index;
    }

    private static void addMeAndParentPackages(@NotNull Index index, @NotNull FqName name) {
        index.declaredPackages.add(name);
        if (!name.isRoot()) {
            addMeAndParentPackages(index, name.parent());
        }
    }

    /*package*/ boolean isPackageDeclaredExplicitly(@NotNull FqName packageFqName) {
        return index.invoke().declaredPackages.contains(packageFqName);
    }

    /*package*/ Collection<FqName> getAllDeclaredSubPackagesOf(@NotNull final FqName parent) {
        return Collections2.filter(index.invoke().declaredPackages, new Predicate<FqName>() {
            @Override
            public boolean apply(FqName fqName) {
                return !fqName.isRoot() && fqName.parent().equals(parent);
            }
        });
    }

    @Nullable
    @Override
    protected PackageMemberDeclarationProvider createPackageMemberDeclarationProvider(@NotNull FqName packageFqName) {
        if (isPackageDeclaredExplicitly(packageFqName)) {
            return new FileBasedPackageMemberDeclarationProvider(
                    storageManager, packageFqName, this, index.invoke().filesByPackage.get(packageFqName));
        }

        return null;
    }

    @NotNull
    @Override
    public ClassMemberDeclarationProvider getClassMemberDeclarationProvider(@NotNull JetClassLikeInfo classLikeInfo) {
        if (!index.invoke().filesByPackage.containsKey(classLikeInfo.getContainingPackageFqName())) {
            throw new IllegalStateException("This factory doesn't know about this class: " + classLikeInfo);
        }

        return new PsiBasedClassMemberDeclarationProvider(storageManager, classLikeInfo);
    }
}