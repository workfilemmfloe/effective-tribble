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

import jet.Function0;
import jet.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.EnumEntrySyntheticClassDescriptor;
import org.jetbrains.jet.lang.resolve.java.resolver.JavaMemberResolver;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaField;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.storage.LockBasedStorageManager;
import org.jetbrains.jet.storage.NotNullLazyValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isEnumClass;
import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.IGNORE_KOTLIN_SOURCES;

public final class JavaClassNonStaticMembersScope extends JavaClassMembersScope {
    @NotNull
    private final ClassDescriptor descriptor;
    @NotNull
    private final JavaClass javaClass;

    public JavaClassNonStaticMembersScope(
            @NotNull ClassDescriptor descriptor,
            @NotNull JavaClass javaClass,
            @NotNull JavaMemberResolver memberResolver
    ) {
        super(descriptor, MembersProvider.forClass(javaClass, false), memberResolver);
        this.descriptor = descriptor;
        this.javaClass = javaClass;
    }

    @NotNull
    public Collection<ConstructorDescriptor> getConstructors() {
        return memberResolver.resolveConstructors(javaClass, descriptor);
    }

    @NotNull
    @Override
    protected Collection<ClassDescriptor> computeInnerClasses() {
        Collection<JavaClass> innerClasses = javaClass.getInnerClasses();
        List<ClassDescriptor> result = new ArrayList<ClassDescriptor>(innerClasses.size());
        for (JavaClass innerClass : innerClasses) {
            result.add(resolveInnerClass(innerClass));
        }

        if (isEnumClass(descriptor)) {
            result.addAll(computeEnumEntries());
        }

        return result;
    }

    private Collection<ClassDescriptor> computeEnumEntries() {
        List<ClassDescriptor> result = new ArrayList<ClassDescriptor>();

        final Collection<NamedMembers> enumNonStaticMembers = MembersProvider.forClass(javaClass, true).allMembers();

        NotNullLazyValue<Collection<Name>> enumMemberNames =
                LockBasedStorageManager.NO_LOCKS.createLazyValue(new Function0<Collection<Name>>() {
                    @Override
                    public Collection<Name> invoke() {
                        return KotlinPackage.map(enumNonStaticMembers, new Function1<NamedMembers, Name>() {
                            @Override
                            public Name invoke(@NotNull NamedMembers members) {
                                return members.getName();
                            }
                        });
                    }
                });

        for (NamedMembers members : enumNonStaticMembers) {
            for (JavaField field : members.getFields()) {
                if (field.isEnumEntry()) {
                    EnumEntrySyntheticClassDescriptor enumEntry = EnumEntrySyntheticClassDescriptor
                            .create(LockBasedStorageManager.NO_LOCKS, descriptor, members.getName(), enumMemberNames);
                    result.add(enumEntry);
                    break;
                }
            }
        }

        return result;
    }

    @NotNull
    private ClassDescriptor resolveInnerClass(@NotNull JavaClass innerClass) {
        FqName fqName = innerClass.getFqName();
        assert fqName != null : "Inner class has no qualified name: " + innerClass;
        ClassDescriptor classDescriptor = memberResolver.resolveClass(fqName, IGNORE_KOTLIN_SOURCES);
        assert classDescriptor != null : "Couldn't resolve inner class " + fqName;
        return classDescriptor;
    }
}
