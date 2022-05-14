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

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiNamedElement;
import com.intellij.util.containers.ComparatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMethodWrapper;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Evgeny Gerashchenko
 * @since 6/5/12
 */
public class AlternativeMethodSignatureData extends ElementAlternativeSignatureData {
    private final JetNamedFunction altFunDeclaration;
    private final PsiMethodWrapper method;

    private JavaDescriptorResolver.ValueParameterDescriptors altValueParameters;
    private JetType altReturnType;
    private List<TypeParameterDescriptor> altTypeParameters;

    private final Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> originalToAltTypeParameters =
            new HashMap<TypeParameterDescriptor, TypeParameterDescriptorImpl>();

    public AlternativeMethodSignatureData(
            @NotNull PsiMethodWrapper method,
            @NotNull JavaDescriptorResolver.ValueParameterDescriptors valueParameterDescriptors,
            @Nullable JetType originalReturnType,
            @NotNull List<TypeParameterDescriptor> methodTypeParameters
    ) {
        String signature = method.getSignatureAnnotation().signature();
        if (signature.isEmpty()) {
            setAnnotated(false);
            altFunDeclaration = null;
            this.method = null;
            return;
        }

        setAnnotated(true);
        this.method = method;
        Project project = method.getPsiMethod().getProject();
        altFunDeclaration = JetPsiFactory.createFunction(project, signature);

        for (TypeParameterDescriptor typeParameter : methodTypeParameters) {
            originalToAltTypeParameters.put(typeParameter,
                                            TypeParameterDescriptorImpl.createForFurtherModification(
                                                    typeParameter.getContainingDeclaration(),
                                                    typeParameter.getAnnotations(),
                                                    typeParameter.isReified(),
                                                    typeParameter.getVariance(),
                                                    typeParameter.getName(),
                                                    typeParameter.getIndex()));
        }

        try {
            checkForSyntaxErrors(altFunDeclaration);
            checkEqualFunctionNames(altFunDeclaration, method);

            computeTypeParameters(methodTypeParameters);
            computeValueParameters(valueParameterDescriptors);

            if (originalReturnType != null) {
                altReturnType = computeReturnType(originalReturnType, altFunDeclaration.getReturnTypeRef(), originalToAltTypeParameters);
            }
        }
        catch (AlternativeSignatureMismatchException e) {
            setError(e.getMessage());
        }
    }

    @NotNull
    public JavaDescriptorResolver.ValueParameterDescriptors getValueParameters() {
        checkForErrors();
        return altValueParameters;
    }

    @NotNull
    public JetType getReturnType() {
        checkForErrors();
        return altReturnType;
    }

    @NotNull
    public List<TypeParameterDescriptor> getTypeParameters() {
        checkForErrors();
        return altTypeParameters;
    }

    private void computeValueParameters(JavaDescriptorResolver.ValueParameterDescriptors valueParameterDescriptors) {
        List<ValueParameterDescriptor> parameterDescriptors = valueParameterDescriptors.getDescriptors();

        if (parameterDescriptors.size() != altFunDeclaration.getValueParameters().size()) {
            throw new AlternativeSignatureMismatchException("Method signature has %d value parameters, but alternative signature has %d",
                                                            parameterDescriptors.size(), altFunDeclaration.getValueParameters().size());
        }

        List<ValueParameterDescriptor> altParamDescriptors = new ArrayList<ValueParameterDescriptor>();
        for (int i = 0, size = parameterDescriptors.size(); i < size; i++) {
            ValueParameterDescriptor originalParameterDescriptor = parameterDescriptors.get(i);
            JetParameter annotationValueParameter = altFunDeclaration.getValueParameters().get(i);

            //noinspection ConstantConditions
            JetTypeElement alternativeTypeElement = annotationValueParameter.getTypeReference().getTypeElement();
            assert alternativeTypeElement != null;

            JetType alternativeType;
            JetType alternativeVarargElementType;

            JetType originalParamVarargElementType = originalParameterDescriptor.getVarargElementType();
            if (originalParamVarargElementType == null) {
                if (annotationValueParameter.isVarArg()) {
                    throw new AlternativeSignatureMismatchException("Parameter in method signature is not vararg, but in alternative signature it is vararg");
                }

                alternativeType = TypeTransformingVisitor.computeType(alternativeTypeElement, originalParameterDescriptor.getType(), originalToAltTypeParameters);
                alternativeVarargElementType = null;
            }
            else {
                if (!annotationValueParameter.isVarArg()) {
                    throw new AlternativeSignatureMismatchException("Parameter in method signature is vararg, but in alternative signature it is not");
                }

                alternativeVarargElementType = TypeTransformingVisitor.computeType(alternativeTypeElement, originalParamVarargElementType,
                                                                                   originalToAltTypeParameters);
                alternativeType = KotlinBuiltIns.getInstance().getArrayType(alternativeVarargElementType);
            }

            altParamDescriptors.add(new ValueParameterDescriptorImpl(
                    originalParameterDescriptor.getContainingDeclaration(),
                    originalParameterDescriptor.getIndex(),
                    originalParameterDescriptor.getAnnotations(),
                    originalParameterDescriptor.getName(),
                    originalParameterDescriptor.isVar(),
                    alternativeType,
                    originalParameterDescriptor.declaresDefaultValue(),
                    alternativeVarargElementType));
        }

        if (valueParameterDescriptors.getReceiverType() != null) {
            throw new UnsupportedOperationException("Alternative annotations for extension functions are not supported yet");
        }

        altValueParameters = new JavaDescriptorResolver.ValueParameterDescriptors(null, altParamDescriptors);
    }

    private void computeTypeParameters(List<TypeParameterDescriptor> typeParameters) {
        if (typeParameters.size() != altFunDeclaration.getTypeParameters().size()) {
            throw new AlternativeSignatureMismatchException("Method signature has %d type parameters, but alternative signature has %d",
                                                            typeParameters.size(), altFunDeclaration.getTypeParameters().size());
        }

        altTypeParameters = new ArrayList<TypeParameterDescriptor>();

        for (int i = 0, size = typeParameters.size(); i < size; i++) {
            TypeParameterDescriptor originalTypeParamDescriptor = typeParameters.get(i);

            TypeParameterDescriptorImpl altParamDescriptor = originalToAltTypeParameters.get(originalTypeParamDescriptor);
            JetTypeParameter altTypeParameter = altFunDeclaration.getTypeParameters().get(i);

            int upperBoundIndex = 0;
            for (JetType upperBound : originalTypeParamDescriptor.getUpperBounds()) {
                JetTypeElement altTypeElement;

                if (upperBoundIndex == 0) {
                    JetTypeReference extendsBound = altTypeParameter.getExtendsBound();
                    if (extendsBound == null) { // default upper bound
                        assert originalTypeParamDescriptor.getUpperBounds().size() == 1;
                        altParamDescriptor.addDefaultUpperBound();
                        break;
                    }
                    else {
                        altTypeElement = extendsBound.getTypeElement();
                    }
                }
                else {
                    JetTypeConstraint constraint =
                            findTypeParameterConstraint(altFunDeclaration, originalTypeParamDescriptor.getName(), upperBoundIndex);
                    if (constraint == null) {
                        throw new AlternativeSignatureMismatchException("Upper bound #%d for type parameter %s is missing",
                                                                        upperBoundIndex, originalTypeParamDescriptor.getName());
                    }
                    //noinspection ConstantConditions
                    altTypeElement = constraint.getBoundTypeReference().getTypeElement();
                }

                assert (altTypeElement != null);

                altParamDescriptor.addUpperBound(TypeTransformingVisitor.computeType(altTypeElement, upperBound,
                                                                                     originalToAltTypeParameters));
                upperBoundIndex++;
            }

            if (findTypeParameterConstraint(altFunDeclaration, originalTypeParamDescriptor.getName(), upperBoundIndex) != null) {
                throw new AlternativeSignatureMismatchException("Extra upper bound #%d for type parameter %s", upperBoundIndex, originalTypeParamDescriptor.getName());
            }

            altParamDescriptor.setInitialized();
            altTypeParameters.add(altParamDescriptor);
        }
    }

    @Nullable
    private static JetTypeConstraint findTypeParameterConstraint(@NotNull JetFunction function, @NotNull Name typeParameterName, int index) {
        if (index != 0) {
            int currentIndex = 0;
            for (JetTypeConstraint constraint : function.getTypeConstraints()) {
                JetSimpleNameExpression parameterName = constraint.getSubjectTypeParameterName();
                assert parameterName != null;
                if (typeParameterName.equals(parameterName.getReferencedNameAsName())) {
                    currentIndex++;
                }
                if (currentIndex == index) {
                    return constraint;
                }
            }
        }
        return null;
    }

    private static void checkEqualFunctionNames(PsiNamedElement namedElement, PsiMethodWrapper method) {
        if (!ComparatorUtil.equalsNullable(method.getName(), namedElement.getName())) {
            throw new AlternativeSignatureMismatchException("Function names mismatch, original: %s, alternative: %s",
                                                            method.getName(), namedElement.getName());
        }
    }
}
