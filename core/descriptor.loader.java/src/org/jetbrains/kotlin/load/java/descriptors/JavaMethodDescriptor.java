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

package org.jetbrains.kotlin.load.java.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeSubstitutor;
import org.jetbrains.kotlin.util.OperatorNameConventions;

import java.util.List;

public class JavaMethodDescriptor extends SimpleFunctionDescriptorImpl implements JavaCallableMemberDescriptor {
    private enum ParameterNamesStatus {
        NON_STABLE_DECLARED(false, false),
        STABLE_DECLARED(true, false),
        NON_STABLE_SYNTHESIZED(false, true),
        STABLE_SYNTHESIZED(true, true), // TODO: this makes no sense
        ;

        public final boolean isStable;
        public final boolean isSynthesized;

        ParameterNamesStatus(boolean isStable, boolean isSynthesized) {
            this.isStable = isStable;
            this.isSynthesized = isSynthesized;
        }

        @NotNull
        public static ParameterNamesStatus get(boolean stable, boolean synthesized) {
            return stable ? (synthesized ? STABLE_SYNTHESIZED : STABLE_DECLARED) :
                   (synthesized ? NON_STABLE_SYNTHESIZED : NON_STABLE_DECLARED);
        }
    }

    private ParameterNamesStatus parameterNamesStatus = null;

    protected JavaMethodDescriptor(
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
    public static JavaMethodDescriptor createJavaMethod(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull SourceElement source
    ) {
        return new JavaMethodDescriptor(containingDeclaration, null, annotations, name, Kind.DECLARATION, source);
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
            @NotNull Visibility visibility
    ) {
        SimpleFunctionDescriptorImpl descriptor = super.initialize(
                receiverParameterType, dispatchReceiverParameter, typeParameters, unsubstitutedValueParameters,
                unsubstitutedReturnType, modality, visibility);
        setOperator(OperatorNameConventions.INSTANCE$.canBeOperator(descriptor));
        return descriptor;
    }

    @Override
    public boolean hasStableParameterNames() {
        assert parameterNamesStatus != null : "Parameter names status was not set: " + this;
        return parameterNamesStatus.isStable;
    }

    @Override
    public boolean hasSynthesizedParameterNames() {
        assert parameterNamesStatus != null : "Parameter names status was not set: " + this;
        return parameterNamesStatus.isSynthesized;
    }

    public void setParameterNamesStatus(boolean hasStableParameterNames, boolean hasSynthesizedParameterNames) {
        this.parameterNamesStatus = ParameterNamesStatus.get(hasStableParameterNames, hasSynthesizedParameterNames);
    }

    @NotNull
    @Override
    protected JavaMethodDescriptor createSubstitutedCopy(
            @NotNull DeclarationDescriptor newOwner,
            @Nullable FunctionDescriptor original,
            @NotNull Kind kind
    ) {
        JavaMethodDescriptor result = new JavaMethodDescriptor(
                newOwner,
                (SimpleFunctionDescriptor) original,
                getAnnotations(),
                getName(),
                kind,
                original != null ? original.getSource() : SourceElement.NO_SOURCE
        );
        result.setParameterNamesStatus(hasStableParameterNames(), hasSynthesizedParameterNames());
        return result;
    }

    @Override
    @NotNull
    public JavaMethodDescriptor enhance(
            @Nullable JetType enhancedReceiverType,
            @NotNull List<JetType> enhancedValueParametersTypes,
            @NotNull JetType enhancedReturnType
    ) {
        List<ValueParameterDescriptor> enhancedValueParameters =
                DescriptorsPackage.createEnhancedValueParameters(enhancedValueParametersTypes, getValueParameters(), this);

        // We use `doSubstitute` here because it does exactly what we need:
        // 1. creates full copy of descriptor
        // 2. copies method's type parameters (with new containing declaration) and properly substitute to them in value parameters, return type and etc.
        JavaMethodDescriptor enhancedMethod = (JavaMethodDescriptor) doSubstitute(
                TypeSubstitutor.EMPTY, getContainingDeclaration(), getModality(), getVisibility(), isOperator(), isInfix(), getOriginal(),
                /* copyOverrides = */ true, getKind(),
                enhancedValueParameters, enhancedReceiverType, enhancedReturnType
        );

        assert enhancedMethod != null : "null after substitution while enhancing " + toString();
        return enhancedMethod;
    }
}
