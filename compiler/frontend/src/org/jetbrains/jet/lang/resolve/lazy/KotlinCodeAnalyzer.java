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

package org.jetbrains.jet.lang.resolve.lazy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.ScriptDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetScript;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.lazy.descriptors.LazyPackageDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;

public interface KotlinCodeAnalyzer {
    @Nullable
    LazyPackageDescriptor getPackageFragment(@NotNull FqName fqName);

    @NotNull
    ModuleDescriptor getModuleDescriptor();

    @NotNull
    @ReadOnly
    Collection<ClassDescriptor> getTopLevelClassDescriptors(@NotNull FqName fqName);

    @NotNull
    ClassDescriptor getClassDescriptor(@NotNull JetClassOrObject classOrObject);

    @NotNull
    ScriptDescriptor getScriptDescriptor(@NotNull JetScript script);

    @NotNull
    BindingContext getBindingContext();

    @NotNull
    DeclarationDescriptor resolveToDescriptor(@NotNull JetDeclaration declaration);

    @NotNull
    ScopeProvider getScopeProvider();

    /**
     * Forces all descriptors to be resolved.
     *
     * Use this method when laziness plays against you, e.g. when lazy descriptors may be accessed in a multi-threaded setting
     */
    void forceResolveAll();
}
