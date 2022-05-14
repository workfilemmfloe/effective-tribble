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

package org.jetbrains.kotlin.types;

import kotlin.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;
import org.jetbrains.kotlin.utils.DFS;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.util.*;

public class TypeUtils {
    public static final JetType DONT_CARE = ErrorUtils.createErrorTypeWithCustomDebugName("DONT_CARE");
    public static final JetType PLACEHOLDER_FUNCTION_TYPE = ErrorUtils.createErrorTypeWithCustomDebugName("PLACEHOLDER_FUNCTION_TYPE");

    public static final JetType CANT_INFER_FUNCTION_PARAM_TYPE = ErrorUtils.createErrorType("Cannot be inferred");

    public static class SpecialType implements JetType {
        private final String name;

        public SpecialType(String name) {
            this.name = name;
        }

        @NotNull
        @Override
        public TypeConstructor getConstructor() {
            throw new IllegalStateException(name);
        }

        @NotNull
        @Override
        public List<TypeProjection> getArguments() {
            throw new IllegalStateException(name);
        }

        @NotNull
        @Override
        public TypeSubstitution getSubstitution() {
            throw new IllegalStateException(name);
        }

        @Override
        public boolean isMarkedNullable() {
            throw new IllegalStateException(name);
        }

        @NotNull
        @Override
        public JetScope getMemberScope() {
            throw new IllegalStateException(name);
        }

        @Override
        public boolean isError() {
            return false;
        }

        @NotNull
        @Override
        public Annotations getAnnotations() {
            throw new IllegalStateException(name);
        }

        @Nullable
        @Override
        public <T extends TypeCapability> T getCapability(@NotNull Class<T> capabilityClass) {
            return null;
        }

        @NotNull
        @Override
        public TypeCapabilities getCapabilities() {
            return TypeCapabilities.NONE.INSTANCE$;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @NotNull
    public static final JetType NO_EXPECTED_TYPE = new SpecialType("NO_EXPECTED_TYPE");

    public static final JetType UNIT_EXPECTED_TYPE = new SpecialType("UNIT_EXPECTED_TYPE");

    public static boolean noExpectedType(@NotNull JetType type) {
        return type == NO_EXPECTED_TYPE || type == UNIT_EXPECTED_TYPE;
    }

    public static boolean isDontCarePlaceholder(@Nullable JetType type) {
        return type != null && type.getConstructor() == DONT_CARE.getConstructor();
    }

    @NotNull
    public static JetType makeNullable(@NotNull JetType type) {
        return makeNullableAsSpecified(type, true);
    }

    @NotNull
    public static JetType makeNotNullable(@NotNull JetType type) {
        return makeNullableAsSpecified(type, false);
    }

    @NotNull
    public static JetType makeNullableAsSpecified(@NotNull JetType type, boolean nullable) {
        NullAwareness nullAwareness = type.getCapability(NullAwareness.class);
        if (nullAwareness != null) {
            return nullAwareness.makeNullableAsSpecified(nullable);
        }

        // Wrapping serves two purposes here
        // 1. It's requires less memory than copying with a changed nullability flag: a copy has many fields, while a wrapper has only one
        // 2. It preserves laziness of types

        // Unwrap to avoid long delegation call chains
        if (type instanceof AbstractTypeWithKnownNullability) {
            return makeNullableAsSpecified(((AbstractTypeWithKnownNullability) type).delegate, nullable);
        }

        // checking to preserve laziness
        if (!(type instanceof LazyType) && type.isMarkedNullable() == nullable) {
            return type;
        }

        return nullable ? new NullableType(type) : new NotNullType(type);
    }

    @NotNull
    public static JetType makeNullableIfNeeded(@NotNull JetType type, boolean nullable) {
        if (nullable) {
            return makeNullable(type);
        }
        return type;
    }

    public static boolean canHaveSubtypes(JetTypeChecker typeChecker, @NotNull JetType type) {
        if (type.isMarkedNullable()) {
            return true;
        }
        if (!type.getConstructor().isFinal()) {
            return true;
        }

        List<TypeParameterDescriptor> parameters = type.getConstructor().getParameters();
        List<TypeProjection> arguments = type.getArguments();
        for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
            TypeParameterDescriptor parameterDescriptor = parameters.get(i);
            TypeProjection typeProjection = arguments.get(i);
            Variance projectionKind = typeProjection.getProjectionKind();
            JetType argument = typeProjection.getType();

            switch (parameterDescriptor.getVariance()) {
                case INVARIANT:
                    switch (projectionKind) {
                        case INVARIANT:
                            if (lowerThanBound(typeChecker, argument, parameterDescriptor) || canHaveSubtypes(typeChecker, argument)) {
                                return true;
                            }
                            break;
                        case IN_VARIANCE:
                            if (lowerThanBound(typeChecker, argument, parameterDescriptor)) {
                                return true;
                            }
                            break;
                        case OUT_VARIANCE:
                            if (canHaveSubtypes(typeChecker, argument)) {
                                return true;
                            }
                            break;
                    }
                    break;
                case IN_VARIANCE:
                    if (projectionKind != Variance.OUT_VARIANCE) {
                        if (lowerThanBound(typeChecker, argument, parameterDescriptor)) {
                            return true;
                        }
                    }
                    else {
                        if (canHaveSubtypes(typeChecker, argument)) {
                            return true;
                        }
                    }
                    break;
                case OUT_VARIANCE:
                    if (projectionKind != Variance.IN_VARIANCE) {
                        if (canHaveSubtypes(typeChecker, argument)) {
                            return true;
                        }
                    }
                    else {
                        if (lowerThanBound(typeChecker, argument, parameterDescriptor)) {
                            return true;
                        }
                    }
                    break;
            }
        }
        return false;
    }

    private static boolean lowerThanBound(JetTypeChecker typeChecker, JetType argument, TypeParameterDescriptor parameterDescriptor) {
        for (JetType bound : parameterDescriptor.getUpperBounds()) {
            if (typeChecker.isSubtypeOf(argument, bound)) {
                if (!argument.getConstructor().equals(bound.getConstructor())) {
                    return true;
                }
            }
        }
        return false;
    }

    @NotNull
    public static JetType makeUnsubstitutedType(ClassDescriptor classDescriptor, JetScope unsubstitutedMemberScope) {
        if (ErrorUtils.isError(classDescriptor)) {
            return ErrorUtils.createErrorType("Unsubstituted type for " + classDescriptor);
        }
        TypeConstructor typeConstructor = classDescriptor.getTypeConstructor();
        List<TypeProjection> arguments = getDefaultTypeProjections(typeConstructor.getParameters());
        return JetTypeImpl.create(
                Annotations.EMPTY,
                typeConstructor,
                false,
                arguments,
                unsubstitutedMemberScope
        );
    }

    @NotNull
    public static List<TypeProjection> getDefaultTypeProjections(@NotNull List<TypeParameterDescriptor> parameters) {
        List<TypeProjection> result = new ArrayList<TypeProjection>(parameters.size());
        for (TypeParameterDescriptor parameterDescriptor : parameters) {
            result.add(new TypeProjectionImpl(parameterDescriptor.getDefaultType()));
        }
        return UtilsPackage.toReadOnlyList(result);
    }

    @NotNull
    public static List<JetType> getImmediateSupertypes(@NotNull JetType type) {
        boolean isNullable = type.isMarkedNullable();
        TypeSubstitutor substitutor = TypeSubstitutor.create(type);
        Collection<JetType> originalSupertypes = type.getConstructor().getSupertypes();
        List<JetType> result = new ArrayList<JetType>(originalSupertypes.size());
        for (JetType supertype : originalSupertypes) {
            JetType substitutedType = substitutor.substitute(supertype, Variance.INVARIANT);
            if (substitutedType != null) {
                result.add(makeNullableIfNeeded(substitutedType, isNullable));
            }
        }
        return result;
    }

    private static void collectAllSupertypes(@NotNull JetType type, @NotNull Set<JetType> result) {
        List<JetType> immediateSupertypes = getImmediateSupertypes(type);
        result.addAll(immediateSupertypes);
        for (JetType supertype : immediateSupertypes) {
            collectAllSupertypes(supertype, result);
        }
    }


    @NotNull
    public static Set<JetType> getAllSupertypes(@NotNull JetType type) {
        // 15 is obtained by experimentation: JDK classes like ArrayList tend to have so many supertypes,
        // the average number is lower
        Set<JetType> result = new LinkedHashSet<JetType>(15);
        collectAllSupertypes(type, result);
        return result;
    }

    public static boolean hasNullableLowerBound(@NotNull TypeParameterDescriptor typeParameterDescriptor) {
        for (JetType bound : typeParameterDescriptor.getLowerBounds()) {
            if (bound.isMarkedNullable()) {
                return true;
            }
        }
        return false;
    }

    /**
     * A work-around of the generic nullability problem in the type checker
     * Semantics should be the same as `!isSubtype(T, Any)`
     * @return true if a value of this type can be null
     */
    public static boolean isNullableType(@NotNull JetType type) {
        if (type.isMarkedNullable()) {
            return true;
        }
        if (TypesPackage.isFlexible(type) && isNullableType(TypesPackage.flexibility(type).getUpperBound())) {
            return true;
        }
        if (isTypeParameter(type)) {
            return hasNullableSuperType(type);
        }
        return false;
    }

    /**
     * Differs from `isNullableType` only by treating type parameters: acceptsNullable(T) <=> T has nullable lower bound
     * Semantics should be the same as `isSubtype(Nothing?, T)`
     * @return true if `null` can be assigned to storage of this type
     */
    public static boolean acceptsNullable(@NotNull JetType type) {
        if (type.isMarkedNullable()) {
            return true;
        }
        if (TypesPackage.isFlexible(type) && acceptsNullable(TypesPackage.flexibility(type).getUpperBound())) {
            return true;
        }
        if (isTypeParameter(type)) {
            return hasNullableLowerBound((TypeParameterDescriptor) type.getConstructor().getDeclarationDescriptor());
        }
        return false;
    }

    public static boolean hasNullableSuperType(@NotNull JetType type) {
        if (type.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor) {
            // A class/trait cannot have a nullable supertype
            return false;
        }

        for (JetType supertype : getImmediateSupertypes(type)) {
            if (supertype.isMarkedNullable()) return true;
            if (hasNullableSuperType(supertype)) return true;
        }

        return false;
    }

    @Nullable
    public static ClassDescriptor getClassDescriptor(@NotNull JetType type) {
        DeclarationDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();
        if (declarationDescriptor instanceof ClassDescriptor) {
            return (ClassDescriptor) declarationDescriptor;
        }
        return null;
    }

    @NotNull
    public static JetType substituteParameters(@NotNull ClassDescriptor clazz, @NotNull List<JetType> typeArguments) {
        List<TypeProjection> projections = CollectionsKt.map(typeArguments, new Function1<JetType, TypeProjection>() {
            @Override
            public TypeProjection invoke(JetType type) {
                return new TypeProjectionImpl(type);
            }
        });

        return substituteProjectionsForParameters(clazz, projections);
    }

    @NotNull
    public static JetType substituteProjectionsForParameters(@NotNull ClassDescriptor clazz, @NotNull List<TypeProjection> projections) {
        List<TypeParameterDescriptor> clazzTypeParameters = clazz.getTypeConstructor().getParameters();
        if (clazzTypeParameters.size() != projections.size()) {
            throw new IllegalArgumentException("type parameter counts do not match: " + clazz + ", " + projections);
        }

        Map<TypeConstructor, TypeProjection> substitutions = UtilsPackage.newHashMapWithExpectedSize(clazzTypeParameters.size());

        for (int i = 0; i < clazzTypeParameters.size(); ++i) {
            TypeConstructor typeConstructor = clazzTypeParameters.get(i).getTypeConstructor();
            substitutions.put(typeConstructor, projections.get(i));
        }

        return TypeSubstitutor.create(substitutions).substitute(clazz.getDefaultType(), Variance.INVARIANT);
    }

    public static boolean equalTypes(@NotNull JetType a, @NotNull JetType b) {
        return JetTypeChecker.DEFAULT.isSubtypeOf(a, b) && JetTypeChecker.DEFAULT.isSubtypeOf(b, a);
    }

    public static boolean dependsOnTypeParameters(@NotNull JetType type, @NotNull Collection<TypeParameterDescriptor> typeParameters) {
        return dependsOnTypeConstructors(type, CollectionsKt.map(
                typeParameters,
                new Function1<TypeParameterDescriptor, TypeConstructor>() {
                    @Override
                    public TypeConstructor invoke(@NotNull TypeParameterDescriptor typeParameterDescriptor) {
                        return typeParameterDescriptor.getTypeConstructor();
                    }
                }
        ));
    }

    public static boolean dependsOnTypeConstructors(@NotNull JetType type, @NotNull Collection<TypeConstructor> typeParameterConstructors) {
        if (typeParameterConstructors.contains(type.getConstructor())) return true;
        for (TypeProjection typeProjection : type.getArguments()) {
            if (!typeProjection.isStarProjection() && dependsOnTypeConstructors(typeProjection.getType(), typeParameterConstructors)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsSpecialType(@Nullable JetType type, @NotNull final JetType specialType) {
        return containsSpecialType(type, new Function1<JetType, Boolean>() {
            @Override
            public Boolean invoke(JetType type) {
                return specialType.equals(type);
            }
        });
    }

    public static boolean containsSpecialType(
            @Nullable JetType type,
            @NotNull Function1<JetType, Boolean> isSpecialType
    ) {
        if (type == null) return false;
        if (isSpecialType.invoke(type)) return true;
        Flexibility flexibility = type.getCapability(Flexibility.class);
        if (flexibility != null
                && (containsSpecialType(flexibility.getLowerBound(), isSpecialType) || containsSpecialType(flexibility.getUpperBound(), isSpecialType))) {
            return true;
        }
        for (TypeProjection projection : type.getArguments()) {
            if (!projection.isStarProjection() && containsSpecialType(projection.getType(), isSpecialType)) return true;
        }
        return false;
    }

    @NotNull
    public static TypeProjection makeStarProjection(@NotNull TypeParameterDescriptor parameterDescriptor) {
        return new StarProjectionImpl(parameterDescriptor);
    }

    @Nullable
    public static JetType commonSupertypeForNumberTypes(@NotNull Collection<JetType> numberLowerBounds) {
        if (numberLowerBounds.isEmpty()) return null;
        Set<JetType> intersectionOfSupertypes = getIntersectionOfSupertypes(numberLowerBounds);
        JetType primitiveNumberType = getDefaultPrimitiveNumberType(intersectionOfSupertypes);
        if (primitiveNumberType != null) {
            return primitiveNumberType;
        }
        return CommonSupertypes.commonSupertype(numberLowerBounds);
    }

    @NotNull
    private static Set<JetType> getIntersectionOfSupertypes(@NotNull Collection<JetType> types) {
        Set<JetType> upperBounds = new HashSet<JetType>();
        for (JetType type : types) {
            Collection<JetType> supertypes = type.getConstructor().getSupertypes();
            if (upperBounds.isEmpty()) {
                upperBounds.addAll(supertypes);
            }
            else {
                upperBounds.retainAll(supertypes);
            }
        }
        return upperBounds;
    }

    @NotNull
    public static JetType getDefaultPrimitiveNumberType(@NotNull IntegerValueTypeConstructor numberValueTypeConstructor) {
        JetType type = getDefaultPrimitiveNumberType(numberValueTypeConstructor.getSupertypes());
        assert type != null : "Strange number value type constructor: " + numberValueTypeConstructor + ". " +
                              "Super types doesn't contain double, int or long: " + numberValueTypeConstructor.getSupertypes();
        return type;
    }

    @Nullable
    private static JetType getDefaultPrimitiveNumberType(@NotNull Collection<JetType> supertypes) {
        if (supertypes.isEmpty()) {
            return null;
        }

        KotlinBuiltIns builtIns = supertypes.iterator().next().getConstructor().getBuiltIns();
        JetType doubleType = builtIns.getDoubleType();
        if (supertypes.contains(doubleType)) {
            return doubleType;
        }
        JetType intType = builtIns.getIntType();
        if (supertypes.contains(intType)) {
            return intType;
        }
        JetType longType = builtIns.getLongType();
        if (supertypes.contains(longType)) {
            return longType;
        }
        return null;
    }

    @NotNull
    public static JetType getPrimitiveNumberType(
            @NotNull IntegerValueTypeConstructor numberValueTypeConstructor,
            @NotNull JetType expectedType
    ) {
        if (noExpectedType(expectedType) || expectedType.isError()) {
            return getDefaultPrimitiveNumberType(numberValueTypeConstructor);
        }
        for (JetType primitiveNumberType : numberValueTypeConstructor.getSupertypes()) {
            if (JetTypeChecker.DEFAULT.isSubtypeOf(primitiveNumberType, expectedType)) {
                return primitiveNumberType;
            }
        }
        return getDefaultPrimitiveNumberType(numberValueTypeConstructor);
    }

    public static List<TypeConstructor> topologicallySortSuperclassesAndRecordAllInstances(
            @NotNull JetType type,
            @NotNull final Map<TypeConstructor, Set<JetType>> constructorToAllInstances,
            @NotNull final Set<TypeConstructor> visited
    ) {
        return DFS.dfs(
                Collections.singletonList(type),
                new DFS.Neighbors<JetType>() {
                    @NotNull
                    @Override
                    public Iterable<JetType> getNeighbors(JetType current) {
                        TypeSubstitutor substitutor = TypeSubstitutor.create(current);
                        Collection<JetType> supertypes = current.getConstructor().getSupertypes();
                        List<JetType> result = new ArrayList<JetType>(supertypes.size());
                        for (JetType supertype : supertypes) {
                            if (visited.contains(supertype.getConstructor())) {
                                continue;
                            }
                            result.add(substitutor.safeSubstitute(supertype, Variance.INVARIANT));
                        }
                        return result;
                    }
                },
                new DFS.Visited<JetType>() {
                    @Override
                    public boolean checkAndMarkVisited(JetType current) {
                        return visited.add(current.getConstructor());
                    }
                },
                new DFS.NodeHandlerWithListResult<JetType, TypeConstructor>() {
                    @Override
                    public boolean beforeChildren(JetType current) {
                        TypeConstructor constructor = current.getConstructor();

                        Set<JetType> instances = constructorToAllInstances.get(constructor);
                        if (instances == null) {
                            instances = new HashSet<JetType>();
                            constructorToAllInstances.put(constructor, instances);
                        }
                        instances.add(current);

                        return true;
                    }

                    @Override
                    public void afterChildren(JetType current) {
                        result.addFirst(current.getConstructor());
                    }
                }
        );
    }

    public static TypeSubstitutor makeConstantSubstitutor(Collection<TypeParameterDescriptor> typeParameterDescriptors, JetType type) {
        final Set<TypeConstructor> constructors = UtilsPackage.newHashSetWithExpectedSize(typeParameterDescriptors.size());
        for (TypeParameterDescriptor typeParameterDescriptor : typeParameterDescriptors) {
            constructors.add(typeParameterDescriptor.getTypeConstructor());
        }
        final TypeProjection projection = new TypeProjectionImpl(type);

        return TypeSubstitutor.create(new TypeConstructorSubstitution() {
            @Override
            public TypeProjection get(@NotNull TypeConstructor key) {
                if (constructors.contains(key)) {
                    return projection;
                }
                return null;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        });
    }

    public static boolean isTypeParameter(@NotNull JetType type) {
        return getTypeParameterDescriptorOrNull(type) != null;
    }

    public static boolean isNonReifiedTypeParemeter(@NotNull JetType type) {
        TypeParameterDescriptor typeParameterDescriptor = getTypeParameterDescriptorOrNull(type);
        return typeParameterDescriptor != null && !typeParameterDescriptor.isReified();
    }

    @Nullable
    public static TypeParameterDescriptor getTypeParameterDescriptorOrNull(@NotNull JetType type) {
        if (type.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor) {
            return (TypeParameterDescriptor) type.getConstructor().getDeclarationDescriptor();
        }
        return null;
    }

    private static abstract class AbstractTypeWithKnownNullability extends AbstractJetType {
        private final JetType delegate;

        private AbstractTypeWithKnownNullability(@NotNull JetType delegate) {
            this.delegate = delegate;
        }

        @Override
        @NotNull
        public TypeConstructor getConstructor() {
            return delegate.getConstructor();
        }

        @Override
        @NotNull
        public List<TypeProjection> getArguments() {
            return delegate.getArguments();
        }

        @Override
        public abstract boolean isMarkedNullable();

        @Override
        @NotNull
        public JetScope getMemberScope() {
            return delegate.getMemberScope();
        }

        @Override
        public boolean isError() {
            return delegate.isError();
        }

        @Override
        @NotNull
        public Annotations getAnnotations() {
            return delegate.getAnnotations();
        }

        @NotNull
        @Override
        public TypeSubstitution getSubstitution() {
            return delegate.getSubstitution();
        }

        @Nullable
        @Override
        public <T extends TypeCapability> T getCapability(@NotNull Class<T> capabilityClass) {
            return delegate.getCapability(capabilityClass);
        }

        @NotNull
        @Override
        public TypeCapabilities getCapabilities() {
            return delegate.getCapabilities();
        }
    }

    private static class NullableType extends AbstractTypeWithKnownNullability {

        private NullableType(@NotNull JetType delegate) {
            super(delegate);
        }

        @Override
        public boolean isMarkedNullable() {
            return true;
        }
    }

    private static class NotNullType extends AbstractTypeWithKnownNullability {

        private NotNullType(@NotNull JetType delegate) {
            super(delegate);
        }

        @Override
        public boolean isMarkedNullable() {
            return false;
        }
    }

}
