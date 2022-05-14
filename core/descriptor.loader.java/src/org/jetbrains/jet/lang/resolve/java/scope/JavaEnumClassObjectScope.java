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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassOrPackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.java.resolver.JavaMemberResolver;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;

import java.util.Collection;
import java.util.Collections;

public class JavaEnumClassObjectScope extends JavaClassMembersScope {
    public JavaEnumClassObjectScope(
            @NotNull ClassOrPackageFragmentDescriptor descriptor,
            @NotNull JavaClass javaClass,
            @NotNull JavaMemberResolver memberResolver
    ) {
        super(descriptor, MembersProvider.forClass(javaClass, true), memberResolver);
    }

    @NotNull
    @Override
    protected Collection<ClassDescriptor> computeInnerClasses() {
        return Collections.emptySet();
    }
}
