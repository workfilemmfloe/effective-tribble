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

package org.jetbrains.jet.lang.descriptors.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MutablePackageFragmentProvider implements PackageFragmentProvider {
    private final ListMultimap<FqName, PackageFragmentDescriptor> packageFragments = ArrayListMultimap.create();

    private final SubModuleDescriptor subModule;
    private final PackageIndex.Builder packageIndexBuilder = new PackageIndex.Builder();

    public MutablePackageFragmentProvider(@NotNull SubModuleDescriptor subModule) {
        this.subModule = subModule;
    }

    @NotNull
    @Override
    public List<PackageFragmentDescriptor> getPackageFragments(@NotNull FqName fqName) {
        List<PackageFragmentDescriptor> descriptors = packageFragments.get(fqName);
        if (descriptors.isEmpty()) {
            if (packageIndexBuilder.getAllPackages().contains(fqName)) {
                EmptyPackageFragment emptyFragment = new EmptyPackageFragment(subModule, PackageFragmentKind.SOURCE, fqName);
                packageFragments.put(fqName, emptyFragment);
                return Collections.<PackageFragmentDescriptor>singletonList(emptyFragment);
            }
        }
        return descriptors;
    }

    @NotNull
    @Override
    public Collection<FqName> getSubPackagesOf(@NotNull FqName fqName) {
        return packageIndexBuilder.getSubPackagesOf(fqName);
    }

    @NotNull
    public MutablePackageFragmentDescriptor addPackageFragment(@NotNull PackageFragmentKind kind, @NotNull FqName fqName) {
        // We use one fragment per kind
        Collection<PackageFragmentDescriptor> fragments = packageFragments.get(fqName);
        for (PackageFragmentDescriptor fragment : fragments) {
            if (kind.equals(fragment.getKind())) return (MutablePackageFragmentDescriptor) fragment;
        }

        MutablePackageFragmentDescriptor result = new MutablePackageFragmentDescriptor(subModule, kind, fqName);
        packageFragments.put(fqName, result);
        packageIndexBuilder.addPackage(fqName);

        return result;
    }

    @Override
    public String toString() {
        return "for " + subModule;
    }
}
