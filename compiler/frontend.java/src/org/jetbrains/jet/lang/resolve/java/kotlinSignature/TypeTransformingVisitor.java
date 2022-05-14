/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.kotlinSignature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.KotlinToJavaTypesMap;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.*;

class TypeTransformingVisitor extends JetVisitor<JetType, Void> {
    private final JetType originalType;
    private final Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> originalToAltTypeParameters;

    private TypeTransformingVisitor(
            JetType originalType,
            Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> originalToAltTypeParameters
    ) {
        this.originalType = originalType;
        this.originalToAltTypeParameters = Collections.unmodifiableMap(originalToAltTypeParameters);
    }

    @NotNull
    public static JetType computeType(
            @NotNull JetTypeElement alternativeTypeElement,
            @NotNull JetType originalType,
            @NotNull Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> originalToAltTypeParameters
    ) {
        JetType computedType = alternativeTypeElement.accept(new TypeTransformingVisitor(originalType, originalToAltTypeParameters), null);
        assert (computedType != null);
        return computedType;
    }

    @Override
    public JetType visitNullableType(JetNullableType nullableType, Void aVoid) {
        if (!originalType.isNullable()) {
            throw new AlternativeSignatureMismatchException("Auto type '%s' is not-null, while type in alternative signature is nullable: '%s'",
                 DescriptorRenderer.TEXT.renderType(originalType), nullableType.getText());
        }
        return TypeUtils.makeNullable(computeType(nullableType.getInnerType(), originalType, originalToAltTypeParameters));
    }

    @Override
    public JetType visitFunctionType(JetFunctionType type, Void data) {
        return visitCommonType(type.getReceiverTypeRef() == null
                ? KotlinBuiltIns.getInstance().getFunction(type.getParameters().size())
                : KotlinBuiltIns.getInstance().getExtensionFunction(type.getParameters().size()), type);
    }

    @Override
    public JetType visitTupleType(JetTupleType type, Void data) {
        return visitCommonType(KotlinBuiltIns.getInstance().getTuple(type.getComponentTypeRefs().size()), type);
    }

    @Override
    public JetType visitUserType(JetUserType type, Void data) {
        JetUserType qualifier = type.getQualifier();

        //noinspection ConstantConditions
        String shortName = type.getReferenceExpression().getReferencedName();
        String longName = (qualifier == null ? "" : qualifier.getText() + ".") + shortName;

        if (KotlinBuiltIns.getInstance().UNIT_ALIAS.getName().equals(longName)) {
            return visitCommonType(KotlinBuiltIns.getInstance().getTuple(0), type);
        }

        return visitCommonType(longName, type);
    }

    private JetType visitCommonType(@NotNull ClassDescriptor classDescriptor, @NotNull JetTypeElement type) {
        return visitCommonType(DescriptorUtils.getFQName(classDescriptor).toSafe().getFqName(), type);
    }

    private JetType visitCommonType(@NotNull String qualifiedName, @NotNull JetTypeElement type) {
        TypeConstructor originalTypeConstructor = originalType.getConstructor();
        ClassifierDescriptor declarationDescriptor = originalTypeConstructor.getDeclarationDescriptor();
        assert declarationDescriptor != null;
        String fqName = DescriptorUtils.getFQName(declarationDescriptor).toSafe().getFqName();
        ClassDescriptor classFromLibrary = getAutoTypeAnalogWithinBuiltins(qualifiedName);
        if (!isSameName(qualifiedName, fqName) && classFromLibrary == null) {
            throw new AlternativeSignatureMismatchException("Alternative signature type mismatch, expected: %s, actual: %s", qualifiedName, fqName);
        }

        List<TypeProjection> arguments = originalType.getArguments();

        if (arguments.size() != type.getTypeArgumentsAsTypes().size()) {
            throw new AlternativeSignatureMismatchException("'%s' type in method signature has %d type arguments, while '%s' in alternative signature has %d of them",
                 DescriptorRenderer.TEXT.renderType(originalType), arguments.size(), type.getText(),
                 type.getTypeArgumentsAsTypes().size());
        }

        List<TypeProjection> altArguments = new ArrayList<TypeProjection>();
        for (int i = 0, size = arguments.size(); i < size; i++) {
            JetTypeElement argumentAlternativeTypeElement = type.getTypeArgumentsAsTypes().get(i).getTypeElement();
            assert argumentAlternativeTypeElement != null;

            TypeProjection argument = arguments.get(i);
            JetType alternativeType = computeType(argumentAlternativeTypeElement, argument.getType(), originalToAltTypeParameters);
            Variance variance = argument.getProjectionKind();
            Variance altVariance;
            if (type instanceof JetUserType) {
                JetTypeProjection typeProjection = ((JetUserType) type).getTypeArguments().get(i);
                switch (typeProjection.getProjectionKind()) {
                    case IN:
                        altVariance = Variance.IN_VARIANCE;
                        break;
                    case OUT:
                        altVariance = Variance.OUT_VARIANCE;
                        break;
                    case STAR:
                        throw new AlternativeSignatureMismatchException("Star projection is not available in alternative signatures");
                    default:
                        altVariance = Variance.INVARIANT;
                }
                if (altVariance != variance && variance != Variance.INVARIANT) {
                    throw new AlternativeSignatureMismatchException("Variance mismatch, actual: %s, in alternative signature: %s", variance, altVariance);
                }
            }
            else {
                altVariance = variance;
            }
            altArguments.add(new TypeProjection(altVariance, alternativeType));
        }

        TypeConstructor typeConstructor;
        if (classFromLibrary != null) {
            typeConstructor = classFromLibrary.getTypeConstructor();
        }
        else {
            typeConstructor = originalTypeConstructor;
        }
        ClassifierDescriptor typeConstructorClassifier = typeConstructor.getDeclarationDescriptor();
        if (typeConstructorClassifier instanceof TypeParameterDescriptor && originalToAltTypeParameters.containsKey(typeConstructorClassifier)) {
            typeConstructor = originalToAltTypeParameters.get(typeConstructorClassifier).getTypeConstructor();
        }
        JetScope memberScope;
        if (typeConstructorClassifier instanceof TypeParameterDescriptor) {
            memberScope = ((TypeParameterDescriptor) typeConstructorClassifier).getUpperBoundsAsType().getMemberScope();
        }
        else if (typeConstructorClassifier instanceof ClassDescriptor) {
            memberScope = ((ClassDescriptor) typeConstructorClassifier).getMemberScope(altArguments);
        }
        else {
            throw new AssertionError("Unexpected class of type constructor classifier "
                                     + (typeConstructorClassifier == null ? "null" : typeConstructorClassifier.getClass().getName()));
        }
        return new JetTypeImpl(originalType.getAnnotations(), typeConstructor, false,
                               altArguments, memberScope);
    }

    @Nullable
    private ClassDescriptor getAutoTypeAnalogWithinBuiltins(String qualifiedName) {
        Type javaAnalog = KotlinToJavaTypesMap.getInstance().getJavaAnalog(originalType);
        if (javaAnalog == null || javaAnalog.getSort() != Type.OBJECT)  return null;
        Collection<ClassDescriptor> descriptors =
                JavaToKotlinClassMap.getInstance().mapPlatformClass(JvmClassName.byType(javaAnalog).getFqName());
        for (ClassDescriptor descriptor : descriptors) {
            String fqName = DescriptorUtils.getFQName(descriptor).getFqName();
            if (isSameName(qualifiedName, fqName)) {
                return descriptor;
            }
        }
        return null;
    }

    @Override
    public JetType visitSelfType(JetSelfType type, Void data) {
        throw new UnsupportedOperationException("Self-types are not supported yet");
    }

    private static boolean isSameName(String qualifiedName, String fullyQualifiedName) {
        return fullyQualifiedName.equals(qualifiedName) || fullyQualifiedName.endsWith("." + qualifiedName);
    }
}
