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

package org.jetbrains.jet.lang.resolve.java.resolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.java.structure.*;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.INCLUDE_KOTLIN_SOURCES;
import static org.jetbrains.jet.lang.resolve.java.resolver.TypeUsage.*;
import static org.jetbrains.jet.lang.types.Variance.*;

public class JavaTypeTransformer {
    private JavaClassResolver classResolver;

    @Inject
    public void setClassResolver(JavaClassResolver classResolver) {
        this.classResolver = classResolver;
    }

    @NotNull
    private TypeProjection transformToTypeProjection(
            @NotNull JavaType type,
            @NotNull TypeParameterDescriptor typeParameterDescriptor,
            @NotNull TypeVariableResolver typeVariableResolver,
            @NotNull TypeUsage howThisTypeIsUsed
    ) {
        if (!(type instanceof JavaWildcardType)) {
            return new TypeProjectionImpl(transformToType(type, howThisTypeIsUsed, typeVariableResolver));
        }

        JavaWildcardType wildcardType = (JavaWildcardType) type;
        JavaType bound = wildcardType.getBound();
        if (bound == null) {
            return SubstitutionUtils.makeStarProjection(typeParameterDescriptor);
        }

        Variance variance = wildcardType.isExtends() ? OUT_VARIANCE : IN_VARIANCE;

        return new TypeProjectionImpl(variance, transformToType(bound, UPPER_BOUND, typeVariableResolver));
    }

    @NotNull
    public JetType transformToType(@NotNull JavaType type, @NotNull TypeVariableResolver typeVariableResolver) {
        return transformToType(type, TypeUsage.MEMBER_SIGNATURE_INVARIANT, typeVariableResolver);
    }

    @NotNull
    public JetType transformToType(
            @NotNull JavaType type,
            @NotNull TypeUsage howThisTypeIsUsed,
            @NotNull TypeVariableResolver typeVariableResolver
    ) {
        if (type instanceof JavaClassifierType) {
            JavaClassifierType classifierType = (JavaClassifierType) type;
            return transformClassifierType(classifierType, howThisTypeIsUsed, typeVariableResolver);
        }
        else if (type instanceof JavaPrimitiveType) {
            String canonicalText = ((JavaPrimitiveType) type).getCanonicalText();
            JetType jetType = JavaToKotlinClassMap.getInstance().mapPrimitiveKotlinClass(canonicalText);
            assert jetType != null : "Primitive type is not found: " + canonicalText;
            return jetType;
        }
        else if (type instanceof JavaArrayType) {
            return transformArrayType((JavaArrayType) type, howThisTypeIsUsed, typeVariableResolver, false);
        }
        else {
            throw new UnsupportedOperationException("Unsupported type: " + type); // TODO
        }
    }

    @NotNull
    private JetType transformClassifierType(
            @NotNull JavaClassifierType classifierType,
            @NotNull TypeUsage howThisTypeIsUsed,
            @NotNull TypeVariableResolver typeVariableResolver
    ) {
        JavaClassifier javaClassifier = classifierType.getClassifier();
        if (javaClassifier instanceof JavaTypeParameter) {
            return transformTypeParameter((JavaTypeParameter) javaClassifier, classifierType, howThisTypeIsUsed, typeVariableResolver);
        }
        else {
            return transformClassType(javaClassifier, classifierType, howThisTypeIsUsed, typeVariableResolver);
        }
    }

    @NotNull
    private JetType transformTypeParameter(
            @NotNull JavaTypeParameter typeParameter,
            @NotNull JavaClassifierType classifierType,
            @NotNull TypeUsage howThisTypeIsUsed,
            @NotNull TypeVariableResolver typeVariableResolver
    ) {
        // In Java: ArrayList<T>
        // In Kotlin: ArrayList<T>, not ArrayList<T?>
        // nullability will be taken care of in individual member signatures
        boolean nullable = !EnumSet.of(TYPE_ARGUMENT, UPPER_BOUND, SUPERTYPE_ARGUMENT).contains(howThisTypeIsUsed);

        JavaTypeParameterListOwner owner = typeParameter.getOwner();
        if (owner instanceof JavaMethod && ((JavaMethod) owner).isConstructor()) {
            Set<JetType> supertypesJet = new HashSet<JetType>();
            for (JavaClassifierType supertype : typeParameter.getUpperBounds()) {
                supertypesJet.add(transformToType(supertype, UPPER_BOUND, typeVariableResolver));
            }
            JetType intersection = TypeUtils.intersect(JetTypeChecker.INSTANCE, supertypesJet);
            if (intersection == null) {
                return createErrorClassifierType(classifierType, nullable);
            }
            return intersection;
        }

        TypeParameterDescriptor descriptor = typeVariableResolver.getTypeVariable(typeParameter.getName());
        if (descriptor == null) return createErrorClassifierType(classifierType, nullable);

        return TypeUtils.makeNullableIfNeeded(descriptor.getDefaultType(), nullable);
    }

    @NotNull
    private JetType transformClassType(
            @Nullable JavaClassifier javaClassifier,
            @NotNull JavaClassifierType classifierType,
            @NotNull TypeUsage howThisTypeIsUsed,
            @NotNull TypeVariableResolver typeVariableResolver
    ) {
        // 'L extends List<T>' in Java is a List<T> in Kotlin, not a List<T?>
        boolean nullable = !EnumSet.of(TYPE_ARGUMENT, SUPERTYPE_ARGUMENT, SUPERTYPE).contains(howThisTypeIsUsed);

        if (javaClassifier == null) return createErrorClassifierType(classifierType, nullable);

        FqName fqName = ((JavaClass) javaClassifier).getFqName();
        assert fqName != null : "Class type should have a FQ name: " + javaClassifier;

        ClassDescriptor classData = JavaToKotlinClassMap.getInstance().mapKotlinClass(fqName, howThisTypeIsUsed);

        if (classData == null) {
            classData = classResolver.resolveClass(fqName, INCLUDE_KOTLIN_SOURCES);
        }
        if (classData == null) {
            return createErrorClassifierType(classifierType, nullable);
        }

        List<TypeProjection> arguments = new ArrayList<TypeProjection>();
        List<TypeParameterDescriptor> parameters = classData.getTypeConstructor().getParameters();
        if (isRaw(classifierType, !parameters.isEmpty())) {
            for (TypeParameterDescriptor parameter : parameters) {
                // not making a star projection because of this case:
                // Java:
                // class C<T extends C> {}
                // The upper bound is raw here, and we can't compute the projection: it would be infinite:
                // C<*> = C<out C<out C<...>>>
                // this way we loose some type information, even when the case is not so bad, but it doesn't seem to matter

                // projections are not allowed in immediate arguments of supertypes
                Variance projectionKind = parameter.getVariance() == OUT_VARIANCE || howThisTypeIsUsed == SUPERTYPE
                                          ? INVARIANT
                                          : OUT_VARIANCE;
                arguments.add(new TypeProjectionImpl(projectionKind, KotlinBuiltIns.getInstance().getNullableAnyType()));
            }
        }
        else {
            List<JavaType> javaTypeArguments = classifierType.getTypeArguments();

            if (parameters.size() != javaTypeArguments.size()) {
                // Most of the time this means there is an error in the Java code
                for (TypeParameterDescriptor parameter : parameters) {
                    arguments.add(new TypeProjectionImpl(ErrorUtils.createErrorType(parameter.getName().asString())));
                }
            }
            else {
                for (int i = 0, size = javaTypeArguments.size(); i < size; i++) {
                    JavaType typeArgument = javaTypeArguments.get(i);
                    TypeParameterDescriptor typeParameterDescriptor = parameters.get(i);

                    TypeUsage howTheProjectionIsUsed = howThisTypeIsUsed == SUPERTYPE ? SUPERTYPE_ARGUMENT : TYPE_ARGUMENT;
                    TypeProjection typeProjection =
                            transformToTypeProjection(typeArgument, typeParameterDescriptor, typeVariableResolver, howTheProjectionIsUsed);

                    if (typeProjection.getProjectionKind() == typeParameterDescriptor.getVariance()) {
                        // remove redundant 'out' and 'in'
                        arguments.add(new TypeProjectionImpl(INVARIANT, typeProjection.getType()));
                    }
                    else {
                        arguments.add(typeProjection);
                    }
                }
            }
        }

        return new JetTypeImpl(
                Annotations.EMPTY,
                classData.getTypeConstructor(),
                nullable,
                arguments,
                classData.getMemberScope(arguments));
    }

    @NotNull
    private static JetType createErrorClassifierType(JavaClassifierType classifierType, boolean nullable) {
        return TypeUtils.makeNullableAsSpecified(
                ErrorUtils.createErrorType("Unresolved java classifier: " + classifierType.getPresentableText()),
                nullable
        );
    }

    @NotNull
    private JetType transformArrayType(
            @NotNull JavaArrayType arrayType,
            @NotNull TypeUsage howThisTypeIsUsed,
            @NotNull TypeVariableResolver typeVariableResolver,
            boolean vararg
    ) {
        JavaType componentType = arrayType.getComponentType();
        if (componentType instanceof JavaPrimitiveType) {
            JetType jetType = JavaToKotlinClassMap.getInstance().mapPrimitiveKotlinClass(
                    "[" + ((JavaPrimitiveType) componentType).getCanonicalText());
            if (jetType != null) {
                return TypeUtils.makeNullable(jetType);
            }
        }

        Variance projectionKind = arrayElementTypeProjectionKind(howThisTypeIsUsed, vararg);
        TypeUsage howArgumentTypeIsUsed = vararg ? MEMBER_SIGNATURE_CONTRAVARIANT : TYPE_ARGUMENT;

        JetType type = transformToType(componentType, howArgumentTypeIsUsed, typeVariableResolver);
        return TypeUtils.makeNullable(KotlinBuiltIns.getInstance().getArrayType(projectionKind, type));
    }

    @NotNull
    private static Variance arrayElementTypeProjectionKind(@NotNull TypeUsage howThisTypeIsUsed, boolean vararg) {
        if (howThisTypeIsUsed == MEMBER_SIGNATURE_CONTRAVARIANT && !vararg) {
            return OUT_VARIANCE;
        }
        else {
            return INVARIANT;
        }
    }

    @NotNull
    public JetType transformVarargType(
            @NotNull JavaArrayType type,
            @NotNull TypeUsage howThisTypeIsUsed,
            @NotNull TypeVariableResolver typeVariableResolver
    ) {
        return transformArrayType(type, howThisTypeIsUsed, typeVariableResolver, true);
    }

    private static boolean isRaw(@NotNull JavaClassifierType classifierType, boolean argumentsExpected) {
        // The second option is needed because sometimes we get weird versions of JDK classes in the class path,
        // such as collections with no generics, so the Java types are not raw, formally, but they don't match with
        // their Kotlin analogs, so we treat them as raw to avoid exceptions
        return classifierType.isRaw() || argumentsExpected && classifierType.getTypeArguments().isEmpty();
    }
}
