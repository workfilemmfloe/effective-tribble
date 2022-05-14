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

package org.jetbrains.jet.lang.resolve.java.scope;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.provider.PackagePsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public final class JavaPackageScopeWithoutMembers extends JavaPackageScope {
    public JavaPackageScopeWithoutMembers(
            @NotNull NamespaceDescriptor descriptor,
            @NotNull PackagePsiDeclarationProvider declarationProvider,
            @NotNull FqName packageFQN,
            @NotNull ModuleDescriptor parentModule,
            @NotNull JavaDescriptorResolver descriptorResolver
    ) {
        super(descriptor, declarationProvider, packageFQN, parentModule, descriptorResolver);
    }

    @Override
    @NotNull
    protected Set<FunctionDescriptor> computeFunctionDescriptor(@NotNull Name name) {
            return Collections.emptySet();
    }

    @NotNull
    @Override
    protected Collection<ClassDescriptor> computeInnerClasses() {
        return Collections.emptyList();
    }
}
