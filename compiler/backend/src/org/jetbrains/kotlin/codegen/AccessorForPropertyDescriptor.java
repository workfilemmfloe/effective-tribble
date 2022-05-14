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

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.PropertySetterDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.JetSuperExpression;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.types.JetType;

import java.util.Collections;

public class AccessorForPropertyDescriptor extends PropertyDescriptorImpl implements AccessorForCallableDescriptor<PropertyDescriptor> {
    private final PropertyDescriptor calleeDescriptor;
    private final int accessorIndex;
    private final JetSuperExpression superCallExpression;

    public AccessorForPropertyDescriptor(
            @NotNull PropertyDescriptor property,
            @NotNull DeclarationDescriptor containingDeclaration,
            int index,
            @Nullable JetSuperExpression superCallExpression
    ) {
        this(property, property.getType(), DescriptorUtils.getReceiverParameterType(property.getExtensionReceiverParameter()),
             property.getDispatchReceiverParameter(), containingDeclaration, index, superCallExpression);
    }

    protected AccessorForPropertyDescriptor(
            @NotNull PropertyDescriptor original,
            @NotNull JetType propertyType,
            @Nullable JetType receiverType,
            @Nullable ReceiverParameterDescriptor dispatchReceiverParameter,
            @NotNull DeclarationDescriptor containingDeclaration,
            int index,
            @Nullable JetSuperExpression superCallExpression
    ) {
        super(containingDeclaration, null, Annotations.EMPTY, Modality.FINAL, Visibilities.LOCAL,
              original.isVar(), Name.identifier("access$" + getIndexedAccessorSuffix(original, index)),
              Kind.DECLARATION, SourceElement.NO_SOURCE, false);

        this.calleeDescriptor = original;
        this.accessorIndex = index;
        this.superCallExpression = superCallExpression;
        setType(propertyType, Collections.<TypeParameterDescriptorImpl>emptyList(), dispatchReceiverParameter, receiverType);
        initialize(new Getter(this), new Setter(this));
    }

    public static class Getter extends PropertyGetterDescriptorImpl implements AccessorForCallableDescriptor<PropertyGetterDescriptor> {
        public Getter(AccessorForPropertyDescriptor property) {
            super(property, Annotations.EMPTY, Modality.FINAL, Visibilities.LOCAL,
                  false, false, Kind.DECLARATION, null, SourceElement.NO_SOURCE);
            initialize(property.getType());
        }

        @NotNull
        @Override
        public PropertyGetterDescriptor getCalleeDescriptor() {
            //noinspection ConstantConditions
            return ((AccessorForPropertyDescriptor) getCorrespondingProperty()).getCalleeDescriptor().getGetter();
        }

        @Nullable
        @Override
        public JetSuperExpression getSuperCallExpression() {
            return ((AccessorForPropertyDescriptor) getCorrespondingProperty()).getSuperCallExpression();
        }
    }

    public static class Setter extends PropertySetterDescriptorImpl implements AccessorForCallableDescriptor<PropertySetterDescriptor>{
        public Setter(AccessorForPropertyDescriptor property) {
            super(property, Annotations.EMPTY, Modality.FINAL, Visibilities.LOCAL,
                  false, false, Kind.DECLARATION, null, SourceElement.NO_SOURCE);
            initializeDefault();
        }

        @NotNull
        @Override
        public PropertySetterDescriptor getCalleeDescriptor() {
            //noinspection ConstantConditions
            return ((AccessorForPropertyDescriptor) getCorrespondingProperty()).getCalleeDescriptor().getSetter();
        }

        @Nullable
        @Override
        public JetSuperExpression getSuperCallExpression() {
            return ((AccessorForPropertyDescriptor) getCorrespondingProperty()).getSuperCallExpression();
        }
    }

    @NotNull
    @Override
    public PropertyDescriptor getCalleeDescriptor() {
        return calleeDescriptor;
    }

    @Override
    public JetSuperExpression getSuperCallExpression() {
        return superCallExpression;
    }

    @NotNull
    public String getIndexedAccessorSuffix() {
        return getIndexedAccessorSuffix(calleeDescriptor, accessorIndex);
    }

    @NotNull
    private static String getIndexedAccessorSuffix(@NotNull PropertyDescriptor original, int index) {
        return original.getName() + "$" + index;
    }
}
