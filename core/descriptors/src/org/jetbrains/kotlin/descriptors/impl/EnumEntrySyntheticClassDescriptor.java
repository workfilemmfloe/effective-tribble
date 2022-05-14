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

package org.jetbrains.kotlin.descriptors.impl;

import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.incremental.components.LookupLocation;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.DescriptorFactory;
import org.jetbrains.kotlin.resolve.OverridingUtil;
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter;
import org.jetbrains.kotlin.resolve.scopes.KtScope;
import org.jetbrains.kotlin.resolve.scopes.KtScopeImpl;
import org.jetbrains.kotlin.resolve.scopes.StaticScopeForKotlinClass;
import org.jetbrains.kotlin.storage.MemoizedFunctionToNotNull;
import org.jetbrains.kotlin.storage.NotNullLazyValue;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeConstructor;
import org.jetbrains.kotlin.types.TypeConstructorImpl;
import org.jetbrains.kotlin.utils.Printer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class EnumEntrySyntheticClassDescriptor extends ClassDescriptorBase {
    private final TypeConstructor typeConstructor;
    private final ConstructorDescriptor primaryConstructor;
    private final KtScope scope;
    private final KtScope staticScope = new StaticScopeForKotlinClass(this);
    private final NotNullLazyValue<Collection<Name>> enumMemberNames;

    /**
     * Creates and initializes descriptors for enum entry with the given name and its companion object
     * @param enumMemberNames needed for fake overrides resolution
     */
    @NotNull
    public static EnumEntrySyntheticClassDescriptor create(
            @NotNull StorageManager storageManager,
            @NotNull ClassDescriptor enumClass,
            @NotNull Name name,
            @NotNull NotNullLazyValue<Collection<Name>> enumMemberNames,
            @NotNull SourceElement source
    ) {
        KotlinType enumType = enumClass.getDefaultType();

        return new EnumEntrySyntheticClassDescriptor(storageManager, enumClass, enumType, name, enumMemberNames, source);
    }

    private EnumEntrySyntheticClassDescriptor(
            @NotNull StorageManager storageManager,
            @NotNull ClassDescriptor containingClass,
            @NotNull KotlinType supertype,
            @NotNull Name name,
            @NotNull NotNullLazyValue<Collection<Name>> enumMemberNames,
            @NotNull SourceElement source
    ) {
        super(storageManager, containingClass, name, source);
        assert containingClass.getKind() == ClassKind.ENUM_CLASS;

        this.typeConstructor =
                TypeConstructorImpl.createForClass(this, getAnnotations(), true, "enum entry", Collections.<TypeParameterDescriptor>emptyList(),
                                        Collections.singleton(supertype));

        this.scope = new EnumEntryScope(storageManager);
        this.enumMemberNames = enumMemberNames;

        ConstructorDescriptorImpl primaryConstructor = DescriptorFactory.createPrimaryConstructorForObject(this, source);
        primaryConstructor.setReturnType(getDefaultType());
        this.primaryConstructor = primaryConstructor;
    }

    @NotNull
    @Override
    public KtScope getUnsubstitutedMemberScope() {
        return scope;
    }

    @NotNull
    @Override
    public KtScope getStaticScope() {
        return staticScope;
    }

    @NotNull
    @Override
    public Collection<ConstructorDescriptor> getConstructors() {
        return Collections.singleton(primaryConstructor);
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    @Nullable
    @Override
    public ClassDescriptor getCompanionObjectDescriptor() {
        return null;
    }

    @NotNull
    @Override
    public ClassKind getKind() {
        return ClassKind.ENUM_ENTRY;
    }

    @NotNull
    @Override
    public Modality getModality() {
        return Modality.FINAL;
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return Visibilities.PUBLIC;
    }

    @Override
    public boolean isInner() {
        return false;
    }

    @Override
    public boolean isData() {
        return false;
    }

    @Override
    public boolean isCompanionObject() {
        return false;
    }

    @Nullable
    @Override
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return primaryConstructor;
    }

    @NotNull
    @Override
    public Annotations getAnnotations() {
        // TODO
        return Annotations.Companion.getEMPTY();
    }

    @Override
    public String toString() {
        return "enum entry " + getName();
    }

    private class EnumEntryScope extends KtScopeImpl {
        private final MemoizedFunctionToNotNull<Name, Collection<FunctionDescriptor>> functions;
        private final MemoizedFunctionToNotNull<Name, Collection<PropertyDescriptor>> properties;
        private final NotNullLazyValue<Collection<DeclarationDescriptor>> allDescriptors;

        public EnumEntryScope(@NotNull StorageManager storageManager) {
            this.functions = storageManager.createMemoizedFunction(new Function1<Name, Collection<FunctionDescriptor>>() {
                @Override
                public Collection<FunctionDescriptor> invoke(Name name) {
                    return computeFunctions(name);
                }
            });

            this.properties = storageManager.createMemoizedFunction(new Function1<Name, Collection<PropertyDescriptor>>() {
                @Override
                public Collection<PropertyDescriptor> invoke(Name name) {
                    return computeProperties(name);
                }
            });
            this.allDescriptors = storageManager.createLazyValue(new Function0<Collection<DeclarationDescriptor>>() {
                @Override
                public Collection<DeclarationDescriptor> invoke() {
                    return computeAllDeclarations();
                }
            });
        }

        @NotNull
        @Override
        @SuppressWarnings("unchecked")
        public Collection<VariableDescriptor> getProperties(@NotNull Name name, @NotNull LookupLocation location) {
            return (Collection) properties.invoke(name);
        }

        @NotNull
        @SuppressWarnings("unchecked")
        private Collection<PropertyDescriptor> computeProperties(@NotNull Name name) {
            return resolveFakeOverrides(name, (Collection) getSupertypeScope().getProperties(name, NoLookupLocation.UNSORTED));
        }

        @NotNull
        @Override
        public Collection<FunctionDescriptor> getFunctions(@NotNull Name name, @NotNull LookupLocation location) {
            return functions.invoke(name);
        }

        @NotNull
        private Collection<FunctionDescriptor> computeFunctions(@NotNull Name name) {
            return resolveFakeOverrides(name, getSupertypeScope().getFunctions(name, NoLookupLocation.UNSORTED));
        }

        @NotNull
        private KtScope getSupertypeScope() {
            Collection<KotlinType> supertype = getTypeConstructor().getSupertypes();
            assert supertype.size() == 1 : "Enum entry and its companion object both should have exactly one supertype: " + supertype;
            return supertype.iterator().next().getMemberScope();
        }

        @NotNull
        private <D extends CallableMemberDescriptor> Collection<D> resolveFakeOverrides(
                @NotNull Name name,
                @NotNull Collection<D> fromSupertypes
        ) {
            final Set<D> result = new HashSet<D>();

            OverridingUtil.generateOverridesInFunctionGroup(
                    name, fromSupertypes, Collections.<D>emptySet(), EnumEntrySyntheticClassDescriptor.this,
                    new OverridingUtil.DescriptorSink() {
                        @Override
                        @SuppressWarnings("unchecked")
                        public void addFakeOverride(@NotNull CallableMemberDescriptor fakeOverride) {
                            OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, null);
                            result.add((D) fakeOverride);
                        }

                        @Override
                        public void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent) {
                            // Do nothing
                        }
                    }
            );

            return result;
        }

        @NotNull
        @Override
        public DeclarationDescriptor getContainingDeclaration() {
            return EnumEntrySyntheticClassDescriptor.this;
        }

        @NotNull
        @Override
        public Collection<DeclarationDescriptor> getDescriptors(
                @NotNull DescriptorKindFilter kindFilter,
                @NotNull Function1<? super Name, ? extends Boolean> nameFilter
        ) {
            return allDescriptors.invoke();
        }

        @NotNull
        private Collection<DeclarationDescriptor> computeAllDeclarations() {
            Collection<DeclarationDescriptor> result = new HashSet<DeclarationDescriptor>();
            for (Name name : enumMemberNames.invoke()) {
                result.addAll(getFunctions(name, NoLookupLocation.UNSORTED));
                result.addAll(getProperties(name, NoLookupLocation.UNSORTED));
            }
            return result;
        }

        @Override
        public void printScopeStructure(@NotNull Printer p) {
            p.println("enum entry scope for " + EnumEntrySyntheticClassDescriptor.this);
        }
    }
}
