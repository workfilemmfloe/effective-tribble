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

package org.jetbrains.jet.lang.resolve.lazy.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.Visibilities;
import org.jetbrains.jet.lang.descriptors.Visibility;
import org.jetbrains.jet.lang.resolve.TemporaryBindingTrace;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.data.JetScriptInfo;
import org.jetbrains.jet.lang.resolve.lazy.declarations.ClassMemberDeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.Name;

public class LazyScriptClassDescriptor extends LazyClassDescriptor {
    public LazyScriptClassDescriptor(
            @NotNull ResolveSession resolveSession,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Name name,
            @NotNull JetScriptInfo scriptInfo
    ) {
        super(resolveSession, containingDeclaration, name, scriptInfo);
    }

    @NotNull
    @Override
    protected LazyScriptClassMemberScope createMemberScope(
            @NotNull ResolveSession resolveSession, @NotNull ClassMemberDeclarationProvider declarationProvider
    ) {
        return new LazyScriptClassMemberScope(
                resolveSession,
                declarationProvider,
                this,
                TemporaryBindingTrace.create(resolveSession.getTrace(), "A trace for script class, needed to avoid rewrites on members")
        );
    }

    @NotNull
    @Override
    public LazyScriptClassMemberScope getScopeForMemberLookup() {
        return (LazyScriptClassMemberScope) super.getScopeForMemberLookup();
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return Visibilities.PUBLIC;
    }
}
