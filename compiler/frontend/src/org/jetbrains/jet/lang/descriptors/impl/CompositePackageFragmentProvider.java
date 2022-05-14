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

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CompositePackageFragmentProvider implements PackageFragmentProvider {

    private final Collection<PackageFragmentProvider> children;

    public CompositePackageFragmentProvider(@NotNull PackageFragmentProvider... children) {
        this(Arrays.asList(children));
    }

    public CompositePackageFragmentProvider(@NotNull Collection<PackageFragmentProvider> children) {
        this.children = children;
    }

    @NotNull
    @Override
    public List<PackageFragmentDescriptor> getPackageFragments(@NotNull FqName fqName) {
        List<PackageFragmentDescriptor> result = Lists.newArrayList();
        for (PackageFragmentProvider child : children) {
            result.addAll(child.getPackageFragments(fqName));
        }
        return result;
    }

    @NotNull
    @Override
    public Collection<FqName> getSubPackagesOf(@NotNull FqName fqName) {
        List<FqName> result = Lists.newArrayList();
        for (PackageFragmentProvider child : children) {
            result.addAll(child.getSubPackagesOf(fqName));
        }
        return result;
    }
}
