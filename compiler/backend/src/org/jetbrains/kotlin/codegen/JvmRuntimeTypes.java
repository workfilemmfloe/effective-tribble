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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.kotlin.descriptors.impl.MutablePackageFragmentDescriptor;
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap;
import org.jetbrains.kotlin.resolve.ImportPath;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils;
import org.jetbrains.kotlin.types.reflect.ReflectionTypes;

import java.util.*;

public class JvmRuntimeTypes {
    private final ReflectionTypes reflectionTypes;

    private final ClassDescriptor functionImpl;
    private final ClassDescriptor extensionFunctionImpl;
    private final ClassDescriptor kFunctionImpl;
    private final ClassDescriptor kMemberFunctionImpl;
    private final ClassDescriptor kExtensionFunctionImpl;

    public JvmRuntimeTypes(@NotNull ReflectionTypes reflectionTypes) {
        this.reflectionTypes = reflectionTypes;

        ModuleDescriptor fakeModule = new ModuleDescriptorImpl(Name.special("<fake module for functions impl>"),
                                                               Collections.<ImportPath>emptyList(), JavaToKotlinClassMap.INSTANCE);

        PackageFragmentDescriptor kotlinJvmInternal =
                new MutablePackageFragmentDescriptor(fakeModule, new FqName("kotlin.jvm.internal"));
        PackageFragmentDescriptor kotlinReflectJvmInternal =
                new MutablePackageFragmentDescriptor(fakeModule, new FqName("kotlin.reflect.jvm.internal"));

        this.functionImpl = createClass(kotlinJvmInternal, "FunctionImpl", "out R");
        this.extensionFunctionImpl = createClass(kotlinJvmInternal, "ExtensionFunctionImpl", "in T", "out R");
        this.kFunctionImpl = createClass(kotlinReflectJvmInternal, "KFunctionImpl", "out R");
        this.kExtensionFunctionImpl = createClass(kotlinReflectJvmInternal, "KExtensionFunctionImpl", "in T", "out R");
        this.kMemberFunctionImpl = createClass(kotlinReflectJvmInternal, "KMemberFunctionImpl", "in T", "out R");
    }

    @NotNull
    private static ClassDescriptor createClass(
            @NotNull PackageFragmentDescriptor packageFragment,
            @NotNull String name,
            @NotNull String... typeParameters
    ) {
        MutableClassDescriptor descriptor = new MutableClassDescriptor(packageFragment, packageFragment.getMemberScope(),
                                                                       ClassKind.CLASS, false, Name.identifier(name), SourceElement.NO_SOURCE);
        List<TypeParameterDescriptor> typeParameterDescriptors = new ArrayList<TypeParameterDescriptor>(typeParameters.length);
        for (int i = 0; i < typeParameters.length; i++) {
            String[] s = typeParameters[i].split(" ");
            Variance variance = Variance.valueOf(s[0].toUpperCase() + "_VARIANCE");
            String typeParameterName = s[1];
            TypeParameterDescriptorImpl typeParameter = TypeParameterDescriptorImpl.createForFurtherModification(
                    descriptor, Annotations.EMPTY, false, variance, Name.identifier(typeParameterName), i, SourceElement.NO_SOURCE
            );
            typeParameter.setInitialized();
            typeParameterDescriptors.add(typeParameter);
        }

        descriptor.setModality(Modality.FINAL);
        descriptor.setVisibility(Visibilities.PUBLIC);
        descriptor.setTypeParameterDescriptors(typeParameterDescriptors);
        descriptor.createTypeConstructor();

        return descriptor;
    }

    @NotNull
    public Collection<JetType> getSupertypesForClosure(@NotNull FunctionDescriptor descriptor) {
        ReceiverParameterDescriptor receiverParameter = descriptor.getExtensionReceiverParameter();

        List<TypeProjection> typeArguments = new ArrayList<TypeProjection>(2);

        ClassDescriptor classDescriptor;
        if (receiverParameter != null) {
            classDescriptor = extensionFunctionImpl;
            typeArguments.add(new TypeProjectionImpl(receiverParameter.getType()));
        }
        else {
            classDescriptor = functionImpl;
        }

        //noinspection ConstantConditions
        typeArguments.add(new TypeProjectionImpl(descriptor.getReturnType()));

        JetType functionImplType = new JetTypeImpl(
                classDescriptor.getDefaultType().getAnnotations(),
                classDescriptor.getTypeConstructor(),
                false,
                typeArguments,
                classDescriptor.getMemberScope(typeArguments)
        );

        JetType functionType = KotlinBuiltIns.getInstance().getFunctionType(
                Annotations.EMPTY,
                receiverParameter == null ? null : receiverParameter.getType(),
                ExpressionTypingUtils.getValueParametersTypes(descriptor.getValueParameters()),
                descriptor.getReturnType()
        );

        return Arrays.asList(functionImplType, functionType);
    }

    @NotNull
    public Collection<JetType> getSupertypesForFunctionReference(@NotNull FunctionDescriptor descriptor) {
        ReceiverParameterDescriptor extensionReceiver = descriptor.getExtensionReceiverParameter();
        ReceiverParameterDescriptor dispatchReceiver = descriptor.getDispatchReceiverParameter();

        List<TypeProjection> typeArguments = new ArrayList<TypeProjection>(2);

        ClassDescriptor classDescriptor;
        JetType receiverType;
        if (extensionReceiver != null) {
            classDescriptor = kExtensionFunctionImpl;
            receiverType = extensionReceiver.getType();
            typeArguments.add(new TypeProjectionImpl(receiverType));
        }
        else if (dispatchReceiver != null) {
            classDescriptor = kMemberFunctionImpl;
            receiverType = dispatchReceiver.getType();
            typeArguments.add(new TypeProjectionImpl(receiverType));
        }
        else {
            classDescriptor = kFunctionImpl;
            receiverType = null;
        }

        //noinspection ConstantConditions
        typeArguments.add(new TypeProjectionImpl(descriptor.getReturnType()));

        JetType kFunctionImplType = new JetTypeImpl(
                classDescriptor.getDefaultType().getAnnotations(),
                classDescriptor.getTypeConstructor(),
                false,
                typeArguments,
                classDescriptor.getMemberScope(typeArguments)
        );

        JetType kFunctionType = reflectionTypes.getKFunctionType(
                Annotations.EMPTY,
                receiverType,
                ExpressionTypingUtils.getValueParametersTypes(descriptor.getValueParameters()),
                descriptor.getReturnType(),
                extensionReceiver != null
        );

        return Arrays.asList(kFunctionImplType, kFunctionType);
    }
}
