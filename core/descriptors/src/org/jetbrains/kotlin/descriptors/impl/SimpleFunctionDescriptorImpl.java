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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeSubstitutor;

import java.util.List;

public class SimpleFunctionDescriptorImpl extends FunctionDescriptorImpl implements SimpleFunctionDescriptor {
    protected SimpleFunctionDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @Nullable SimpleFunctionDescriptor original,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull Kind kind,
            @NotNull SourceElement source
    ) {
        super(containingDeclaration, original, annotations, name, kind, source);
    }

    @NotNull
    public static SimpleFunctionDescriptorImpl create(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull Kind kind,
            @NotNull SourceElement source
    ) {
        return new SimpleFunctionDescriptorImpl(containingDeclaration, null, annotations, name, kind, source);
    }

    @NotNull
    @Override
    public SimpleFunctionDescriptorImpl initialize(
            @Nullable JetType receiverParameterType,
            @Nullable ReceiverParameterDescriptor dispatchReceiverParameter,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @Nullable JetType unsubstitutedReturnType,
            @Nullable Modality modality,
            @NotNull Visibility visibility,
            boolean isOperator
    ) {
        super.initialize(receiverParameterType, dispatchReceiverParameter, typeParameters, unsubstitutedValueParameters,
                         unsubstitutedReturnType, modality, visibility, isOperator);
        return this;
    }

    @NotNull
    @Override
    public SimpleFunctionDescriptor getOriginal() {
        return (SimpleFunctionDescriptor) super.getOriginal();
    }

    @NotNull
    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy(
            @NotNull DeclarationDescriptor newOwner,
            @Nullable FunctionDescriptor original,
            @NotNull Kind kind
    ) {
        return new SimpleFunctionDescriptorImpl(
                newOwner,
                (SimpleFunctionDescriptor) original,
                // TODO : safeSubstitute
                getAnnotations(),
                getName(),
                kind,
                SourceElement.NO_SOURCE
        );
    }

    @NotNull
    @Override
    public SimpleFunctionDescriptor copy(
            DeclarationDescriptor newOwner,
            Modality modality,
            Visibility visibility,
            Kind kind,
            boolean copyOverrides
    ) {
        return (SimpleFunctionDescriptorImpl) doSubstitute(
                TypeSubstitutor.EMPTY, newOwner, modality, visibility, isOperator(), null, copyOverrides, kind
        );
    }
}
