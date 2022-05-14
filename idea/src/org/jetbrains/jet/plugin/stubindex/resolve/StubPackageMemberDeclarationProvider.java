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

package org.jetbrains.jet.plugin.stubindex.resolve;

import com.intellij.openapi.project.Project;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.lazy.declarations.PackageMemberDeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.plugin.stubindex.JetAllPackagesIndex;
import org.jetbrains.jet.plugin.stubindex.JetTopLevelFunctionsFqnNameIndex;
import org.jetbrains.jet.plugin.stubindex.JetTopLevelPropertiesFqnNameIndex;
import org.jetbrains.jet.lang.resolve.name.NamePackage;

import java.util.Collection;
import java.util.Collections;

public class StubPackageMemberDeclarationProvider extends AbstractStubDeclarationProvider implements PackageMemberDeclarationProvider {
    private final FqName fqName;
    private final Project project;
    private final GlobalSearchScope searchScope;

    public StubPackageMemberDeclarationProvider(@NotNull FqName fqName, @NotNull Project project, @NotNull GlobalSearchScope searchScope) {
        this.fqName = fqName;
        this.project = project;
        this.searchScope = searchScope;
    }

    @NotNull
    @Override
    public Collection<JetNamedFunction> getFunctionDeclarations(@NotNull Name name) {
        return JetTopLevelFunctionsFqnNameIndex.getInstance().get(fqName.child(name).toString(), project, searchScope);
    }

    @NotNull
    @Override
    public Collection<JetProperty> getPropertyDeclarations(@NotNull Name name) {
        return JetTopLevelPropertiesFqnNameIndex.getInstance().get(fqName.child(name).toString(), project, searchScope);
    }

    @Override
    public Collection<FqName> getAllDeclaredPackages() {
        // get all packages in this project
        Collection<String> allPackagesInProject = JetAllPackagesIndex.getInstance().getAllKeys(project);
        return ContainerUtil.mapNotNull(allPackagesInProject, new Function<String, FqName>() {
            @Override
            public FqName fun(String packageFqName) {
                // filter by the search scope
                Collection<JetFile> files = JetAllPackagesIndex.getInstance().get(packageFqName, project, searchScope);
                if (files.isEmpty()) return null;
                return new FqName(packageFqName);
            }
        });
    }

    @NotNull
    @Override
    public Collection<NavigatablePsiElement> getPackageDeclarations(final FqName fqName) {
        if (fqName.isRoot()) {
            return Collections.emptyList();
        }

        Collection<JetFile> files = JetAllPackagesIndex.getInstance().get(fqName.asString(), project, searchScope);
        return ContainerUtil.map(files, new Function<JetFile, NavigatablePsiElement>() {
            @Override
            public NavigatablePsiElement fun(JetFile file) {
                return JetPsiUtil.getPackageReference(file, NamePackage.numberOfSegments(fqName) - 1);
            }
        });
    }

    @NotNull
    @Override
    public Collection<JetFile> getPackageFiles() {
        return JetAllPackagesIndex.getInstance().get(fqName.asString(), project, searchScope);
    }
}
