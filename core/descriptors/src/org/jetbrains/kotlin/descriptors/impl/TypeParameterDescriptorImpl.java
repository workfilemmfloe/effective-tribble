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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.SourceElement;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeConstructor;
import org.jetbrains.kotlin.types.TypeConstructorImpl;
import org.jetbrains.kotlin.types.Variance;
import org.jetbrains.kotlin.utils.SmartSet;

import java.util.Collections;
import java.util.Set;

import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilPackage.getBuiltIns;

public class TypeParameterDescriptorImpl extends AbstractTypeParameterDescriptor {
    public static TypeParameterDescriptor createWithDefaultBound(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            boolean reified,
            @NotNull Variance variance,
            @NotNull Name name,
            int index
    ) {
        TypeParameterDescriptorImpl typeParameterDescriptor =
                createForFurtherModification(containingDeclaration, annotations, reified, variance, name, index, SourceElement.NO_SOURCE);
        typeParameterDescriptor.addUpperBound(getBuiltIns(containingDeclaration).getDefaultBound());
        typeParameterDescriptor.setInitialized();
        return typeParameterDescriptor;
    }

    public static TypeParameterDescriptorImpl createForFurtherModification(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            boolean reified,
            @NotNull Variance variance,
            @NotNull Name name,
            int index,
            @NotNull SourceElement source
    ) {
        return new TypeParameterDescriptorImpl(containingDeclaration, annotations, reified, variance, name, index, source);
    }

    private final Set<JetType> upperBounds = SmartSet.create();
    private boolean initialized = false;

    private TypeParameterDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            boolean reified,
            @NotNull Variance variance,
            @NotNull Name name,
            int index,
            @NotNull SourceElement source
    ) {
        super(LockBasedStorageManager.NO_LOCKS, containingDeclaration, annotations, name, variance, reified, index, source);
    }

    @NotNull
    @Override
    protected TypeConstructor createTypeConstructor() {
        // TODO: Should we actually pass the annotations on to the type constructor?
        return TypeConstructorImpl.createForTypeParameter(
                this,
                getAnnotations(),
                false,
                getName().asString(),
                Collections.<TypeParameterDescriptor>emptyList(),
                upperBounds
        );
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Type parameter descriptor is not initialized: " + nameForAssertions());
        }
    }

    private void checkUninitialized() {
        if (initialized) {
            throw new IllegalStateException("Type parameter descriptor is already initialized: " + nameForAssertions());
        }
    }

    private String nameForAssertions() {
        return getName() + " declared in " + DescriptorUtils.getFqName(getContainingDeclaration());
    }

    public void setInitialized() {
        checkUninitialized();
        initialized = true;
    }

    public void addUpperBound(@NotNull JetType bound) {
        checkUninitialized();
        doAddUpperBound(bound);
    }

    private void doAddUpperBound(JetType bound) {
        upperBounds.add(bound); // TODO : Duplicates?
    }

    public void addDefaultUpperBound() {
        checkUninitialized();

        if (upperBounds.isEmpty()) {
            doAddUpperBound(getBuiltIns(getContainingDeclaration()).getDefaultBound());
        }
    }

    @NotNull
    @Override
    protected Set<JetType> resolveUpperBounds() {
        checkInitialized();
        return upperBounds;
    }
}
