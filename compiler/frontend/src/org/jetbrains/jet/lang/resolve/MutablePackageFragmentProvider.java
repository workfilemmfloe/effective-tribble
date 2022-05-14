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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Maps;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider;
import org.jetbrains.jet.lang.descriptors.impl.MutablePackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MutablePackageFragmentProvider implements PackageFragmentProvider {
    private final ModuleDescriptor module;

    private final Map<FqName, MutablePackageFragmentDescriptor> fqNameToPackage = Maps.newHashMap();
    private final MultiMap<FqName, FqName> subPackages = MultiMap.create();

    public MutablePackageFragmentProvider(@NotNull ModuleDescriptor module) {
        this.module = module;
        fqNameToPackage.put(FqName.ROOT, new MutablePackageFragmentDescriptor(module, FqName.ROOT));
    }

    @NotNull
    @Override
    public List<PackageFragmentDescriptor> getPackageFragments(@NotNull FqName fqName) {
        return ContainerUtil.<PackageFragmentDescriptor>createMaybeSingletonList(fqNameToPackage.get(fqName));
    }

    @NotNull
    @Override
    public Collection<FqName> getSubPackagesOf(@NotNull FqName fqName) {
        return subPackages.get(fqName);
    }

    @NotNull
    public MutablePackageFragmentDescriptor getOrCreateFragment(@NotNull FqName fqName) {
        if (!fqNameToPackage.containsKey(fqName)) {
            FqName parent = fqName.parent();
            getOrCreateFragment(parent); // assure that parent exists

            fqNameToPackage.put(fqName, new MutablePackageFragmentDescriptor(module, fqName));
            subPackages.putValue(parent, fqName);
        }

        return fqNameToPackage.get(fqName);
    }

    @NotNull
    public ModuleDescriptor getModule() {
        return module;
    }

    @NotNull
    public Collection<MutablePackageFragmentDescriptor> getAllFragments() {
        return fqNameToPackage.values();
    }
}
