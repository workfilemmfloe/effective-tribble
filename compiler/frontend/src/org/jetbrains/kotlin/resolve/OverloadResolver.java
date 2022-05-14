/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve;

import com.google.common.collect.Sets;
import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.psi.JetDeclaration;
import org.jetbrains.kotlin.psi.JetObjectDeclaration;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.kotlin.resolve.DescriptorUtils.getFqName;

public class OverloadResolver {
    private BindingTrace trace;

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    public void process(@NotNull BodiesResolveContext c) {
        checkOverloads(c);
    }

    private void checkOverloads(@NotNull BodiesResolveContext c) {
        MultiMap<ClassDescriptor, ConstructorDescriptor> inClasses = MultiMap.create();
        MultiMap<FqNameUnsafe, ConstructorDescriptor> inPackages = MultiMap.create();
        fillGroupedConstructors(c, inClasses, inPackages);

        for (Map.Entry<JetClassOrObject, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            checkOverloadsInAClass(entry.getValue(), entry.getKey(), inClasses.get(entry.getValue()));
        }
        checkOverloadsInPackages(c, inPackages);
    }

    private static void fillGroupedConstructors(
            @NotNull BodiesResolveContext c,
            @NotNull MultiMap<ClassDescriptor, ConstructorDescriptor> inClasses,
            @NotNull MultiMap<FqNameUnsafe, ConstructorDescriptor> inPackages
    ) {
        for (ClassDescriptorWithResolutionScopes klass : c.getDeclaredClasses().values()) {
            if (klass.getKind().isSingleton()) {
                // Constructors of singletons aren't callable from the code, so they shouldn't participate in overload name checking
                continue;
            }
            DeclarationDescriptor containingDeclaration = klass.getContainingDeclaration();
            if (containingDeclaration instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
                inClasses.put(classDescriptor, klass.getConstructors());
            }
            else if (containingDeclaration instanceof PackageFragmentDescriptor) {
                inPackages.put(getFqName(klass), klass.getConstructors());
            }
            else if (containingDeclaration instanceof ScriptDescriptor) {
                // TODO: check overload conflicts of functions with constructors in scripts
            }
            else if (!(containingDeclaration instanceof FunctionDescriptor)) {
                throw new IllegalStateException("Illegal class container: " + containingDeclaration);
            }
        }
    }

    private void checkOverloadsInPackages(
            @NotNull BodiesResolveContext c,
            @NotNull MultiMap<FqNameUnsafe, ConstructorDescriptor> inPackages
    ) {

        MultiMap<FqNameUnsafe, CallableMemberDescriptor> functionsByName = MultiMap.create();

        for (SimpleFunctionDescriptor function : c.getFunctions().values()) {
            if (function.getContainingDeclaration() instanceof PackageFragmentDescriptor) {
                functionsByName.putValue(getFqName(function), function);
            }
        }
        
        for (PropertyDescriptor property : c.getProperties().values()) {
            if (property.getContainingDeclaration() instanceof PackageFragmentDescriptor) {
                functionsByName.putValue(getFqName(property), property);
            }
        }
        
        for (Map.Entry<FqNameUnsafe, Collection<ConstructorDescriptor>> entry : inPackages.entrySet()) {
            functionsByName.putValues(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<FqNameUnsafe, Collection<CallableMemberDescriptor>> e : functionsByName.entrySet()) {
            // TODO: don't render FQ name here, extract this logic to somewhere
            FqNameUnsafe fqName = e.getKey().parent();
            checkOverloadsWithSameName(e.getValue(), fqName.isRoot() ? "root package" : fqName.asString());
        }
    }

    private static String nameForErrorMessage(ClassDescriptor classDescriptor, JetClassOrObject jetClass) {
        String name = jetClass.getName();
        if (name != null) {
            return name;
        }
        if (jetClass instanceof JetObjectDeclaration) {
            // must be class object
            name = classDescriptor.getContainingDeclaration().getName().asString();
            return "class object " + name;
        }
        // safe
        return "<unknown>";
    }

    private void checkOverloadsInAClass(
            ClassDescriptorWithResolutionScopes classDescriptor, JetClassOrObject klass,
            Collection<ConstructorDescriptor> nestedClassConstructors
    ) {
        MultiMap<Name, CallableMemberDescriptor> functionsByName = MultiMap.create();
        
        for (CallableMemberDescriptor function : classDescriptor.getDeclaredCallableMembers()) {
            functionsByName.putValue(function.getName(), function);
        }
        
        for (ConstructorDescriptor nestedClassConstructor : nestedClassConstructors) {
            functionsByName.putValue(nestedClassConstructor.getContainingDeclaration().getName(), nestedClassConstructor);
        }
        
        for (Map.Entry<Name, Collection<CallableMemberDescriptor>> e : functionsByName.entrySet()) {
            checkOverloadsWithSameName(e.getValue(), nameForErrorMessage(classDescriptor, klass));
        }

        // Kotlin has no secondary constructors at this time

    }
    
    private void checkOverloadsWithSameName(
            Collection<CallableMemberDescriptor> functions,
            @NotNull String functionContainer
    ) {
        if (functions.size() == 1) {
            // micro-optimization
            return;
        }
        reportRedeclarations(functionContainer, findRedeclarations(functions));
    }

    @NotNull
    private Set<Pair<JetDeclaration, CallableMemberDescriptor>> findRedeclarations(@NotNull Collection<CallableMemberDescriptor> functions) {
        Set<Pair<JetDeclaration, CallableMemberDescriptor>> redeclarations = Sets.newLinkedHashSet();
        for (CallableMemberDescriptor member : functions) {
            for (CallableMemberDescriptor member2 : functions) {
                if (member == member2) {
                    continue;
                }

                OverloadUtil.OverloadCompatibilityInfo overloadable = OverloadUtil.isOverloadable(member, member2);
                if (!overloadable.isSuccess() && member.getKind() != CallableMemberDescriptor.Kind.SYNTHESIZED) {
                    JetDeclaration jetDeclaration = (JetDeclaration) DescriptorToSourceUtils.descriptorToDeclaration(member);
                    if (jetDeclaration != null) {
                        redeclarations.add(Pair.create(jetDeclaration, member));
                    }
                }
            }
        }
        return redeclarations;
    }

    private void reportRedeclarations(@NotNull String functionContainer,
            @NotNull Set<Pair<JetDeclaration, CallableMemberDescriptor>> redeclarations) {
        for (Pair<JetDeclaration, CallableMemberDescriptor> redeclaration : redeclarations) {
            CallableMemberDescriptor memberDescriptor = redeclaration.getSecond();
            JetDeclaration jetDeclaration = redeclaration.getFirst();
            if (memberDescriptor instanceof PropertyDescriptor) {
                trace.report(Errors.REDECLARATION.on(jetDeclaration, memberDescriptor.getName().asString()));
            }
            else {
                trace.report(Errors.CONFLICTING_OVERLOADS.on(jetDeclaration, memberDescriptor, functionContainer));
            }
        }
    }
}
