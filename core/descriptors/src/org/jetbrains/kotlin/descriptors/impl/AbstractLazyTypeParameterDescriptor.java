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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.SourceElement;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeConstructor;
import org.jetbrains.kotlin.types.Variance;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class AbstractLazyTypeParameterDescriptor extends AbstractTypeParameterDescriptor {
    public AbstractLazyTypeParameterDescriptor(
            @NotNull StorageManager storageManager,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Name name,
            @NotNull Variance variance,
            boolean isReified,
            int index,
            @NotNull SourceElement source
    ) {
        super(storageManager, containingDeclaration, Annotations.EMPTY /* TODO */, name, variance, isReified, index, source);
    }

    @NotNull
    @Override
    protected TypeConstructor createTypeConstructor() {
        return new TypeConstructor() {
            @NotNull
            @Override
            public Collection<JetType> getSupertypes() {
                return AbstractLazyTypeParameterDescriptor.this.getUpperBounds();
            }

            @NotNull
            @Override
            public List<TypeParameterDescriptor> getParameters() {
                return Collections.emptyList();
            }

            @Override
            public boolean isFinal() {
                return false;
            }

            @Override
            public boolean isDenotable() {
                return true;
            }

            @Override
            public ClassifierDescriptor getDeclarationDescriptor() {
                return AbstractLazyTypeParameterDescriptor.this;
            }

            @NotNull
            @Override
            public Annotations getAnnotations() {
                return AbstractLazyTypeParameterDescriptor.this.getAnnotations();
            }

            @NotNull
            @Override
            public KotlinBuiltIns getBuiltIns() {
                return DescriptorUtilsKt.getBuiltIns(AbstractLazyTypeParameterDescriptor.this);
            }

            @Override
            public String toString() {
                return getName().toString();
            }
        };
    }

    @Override
    public String toString() {
        // Not using descriptor renderer to preserve laziness
        return String.format(
                "%s%s%s",
                isReified() ? "reified " : "",
                getVariance() == Variance.INVARIANT ? "" : getVariance() + " ",
                getName()
        );
    }
}
