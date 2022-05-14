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

package org.jetbrains.kotlin.resolve;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.typeUtil.TypeUtilsKt;

import java.util.*;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.BindingContext.TYPE;
import static org.jetbrains.kotlin.resolve.BindingContext.TYPE_PARAMETER;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.classCanHaveAbstractMembers;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.classCanHaveOpenMembers;

public class DeclarationsChecker {
    @NotNull private final BindingTrace trace;
    @NotNull private final ModifiersChecker.ModifiersCheckingProcedure modifiersChecker;
    @NotNull private final DescriptorResolver descriptorResolver;
    @NotNull private final AnnotationChecker annotationChecker;
    @NotNull private final IdentifierChecker identifierChecker;

    public DeclarationsChecker(
            @NotNull DescriptorResolver descriptorResolver,
            @NotNull ModifiersChecker modifiersChecker,
            @NotNull AnnotationChecker annotationChecker,
            @NotNull IdentifierChecker identifierChecker,
            @NotNull BindingTrace trace
    ) {
        this.descriptorResolver = descriptorResolver;
        this.modifiersChecker = modifiersChecker.withTrace(trace);
        this.annotationChecker = annotationChecker;
        this.identifierChecker = identifierChecker;
        this.trace = trace;
    }

    public void process(@NotNull BodiesResolveContext bodiesResolveContext) {
        for (KtFile file : bodiesResolveContext.getFiles()) {
            checkModifiersAndAnnotationsInPackageDirective(file);
            annotationChecker.check(file, trace, null);
        }

        Map<KtClassOrObject, ClassDescriptorWithResolutionScopes> classes = bodiesResolveContext.getDeclaredClasses();
        for (Map.Entry<KtClassOrObject, ClassDescriptorWithResolutionScopes> entry : classes.entrySet()) {
            KtClassOrObject classOrObject = entry.getKey();
            ClassDescriptorWithResolutionScopes classDescriptor = entry.getValue();

            checkSupertypesForConsistency(classOrObject, classDescriptor);
            checkTypesInClassHeader(classOrObject);

            if (classOrObject instanceof KtClass) {
                KtClass ktClass = (KtClass) classOrObject;
                checkClass(bodiesResolveContext, ktClass, classDescriptor);
                descriptorResolver.checkNamesInConstraints(
                        ktClass, classDescriptor, classDescriptor.getScopeForClassHeaderResolution(), trace);
            }
            else if (classOrObject instanceof KtObjectDeclaration) {
                checkObject((KtObjectDeclaration) classOrObject, classDescriptor);
            }

            checkPrimaryConstructor(classOrObject, classDescriptor);

            modifiersChecker.checkModifiersForDeclaration(classOrObject, classDescriptor);
            identifierChecker.checkDeclaration(classOrObject, trace);
            checkClassExposedType(classOrObject, classDescriptor);
        }

        Map<KtNamedFunction, SimpleFunctionDescriptor> functions = bodiesResolveContext.getFunctions();
        for (Map.Entry<KtNamedFunction, SimpleFunctionDescriptor> entry : functions.entrySet()) {
            KtNamedFunction function = entry.getKey();
            SimpleFunctionDescriptor functionDescriptor = entry.getValue();

            checkFunction(function, functionDescriptor);
            modifiersChecker.checkModifiersForDeclaration(function, functionDescriptor);
            identifierChecker.checkDeclaration(function, trace);
        }

        Map<KtProperty, PropertyDescriptor> properties = bodiesResolveContext.getProperties();
        for (Map.Entry<KtProperty, PropertyDescriptor> entry : properties.entrySet()) {
            KtProperty property = entry.getKey();
            PropertyDescriptor propertyDescriptor = entry.getValue();

            checkProperty(property, propertyDescriptor);
            modifiersChecker.checkModifiersForDeclaration(property, propertyDescriptor);
            identifierChecker.checkDeclaration(property, trace);
        }

        for (Map.Entry<KtSecondaryConstructor, ConstructorDescriptor> entry : bodiesResolveContext.getSecondaryConstructors().entrySet()) {
            ConstructorDescriptor constructorDescriptor = entry.getValue();
            KtSecondaryConstructor declaration = entry.getKey();
            checkConstructorDeclaration(constructorDescriptor, declaration);
            checkFunctionExposedType(declaration, constructorDescriptor);
        }
    }

    private void checkConstructorDeclaration(ConstructorDescriptor constructorDescriptor, KtDeclaration declaration) {
        modifiersChecker.checkModifiersForDeclaration(declaration, constructorDescriptor);
        identifierChecker.checkDeclaration(declaration, trace);
    }

    private void checkModifiersAndAnnotationsInPackageDirective(KtFile file) {
        KtPackageDirective packageDirective = file.getPackageDirective();
        if (packageDirective == null) return;

        KtModifierList modifierList = packageDirective.getModifierList();
        if (modifierList == null) return;

        for (KtAnnotationEntry annotationEntry : modifierList.getAnnotationEntries()) {
            KtConstructorCalleeExpression calleeExpression = annotationEntry.getCalleeExpression();
            if (calleeExpression != null) {
                KtReferenceExpression reference = calleeExpression.getConstructorReferenceExpression();
                if (reference != null) {
                    trace.report(UNRESOLVED_REFERENCE.on(reference, reference));
                }
            }
        }
        annotationChecker.check(packageDirective, trace, null);
        ModifierCheckerCore.INSTANCE$.check(packageDirective, trace, null);
    }

    private void checkTypesInClassHeader(@NotNull KtClassOrObject classOrObject) {
        for (KtDelegationSpecifier delegationSpecifier : classOrObject.getDelegationSpecifiers()) {
            checkBoundsForTypeInClassHeader(delegationSpecifier.getTypeReference());
        }

        if (!(classOrObject instanceof KtClass)) return;
        KtClass ktClass = (KtClass) classOrObject;

        for (KtTypeParameter jetTypeParameter : ktClass.getTypeParameters()) {
            checkBoundsForTypeInClassHeader(jetTypeParameter.getExtendsBound());
            checkFinalUpperBounds(jetTypeParameter.getExtendsBound());
        }

        for (KtTypeConstraint constraint : ktClass.getTypeConstraints()) {
            checkBoundsForTypeInClassHeader(constraint.getBoundTypeReference());
            checkFinalUpperBounds(constraint.getBoundTypeReference());
        }
    }

    private void checkBoundsForTypeInClassHeader(@Nullable KtTypeReference typeReference) {
        if (typeReference != null) {
            KotlinType type = trace.getBindingContext().get(TYPE, typeReference);
            if (type != null) {
                DescriptorResolver.checkBounds(typeReference, type, trace);
            }
        }
    }

    private void checkFinalUpperBounds(@Nullable KtTypeReference typeReference) {
        if (typeReference != null) {
            KotlinType type = trace.getBindingContext().get(TYPE, typeReference);
            if (type != null) {
                DescriptorResolver.checkUpperBoundType(typeReference, type, trace);
            }
        }
    }

    private void checkSupertypesForConsistency(
            @NotNull KtClassOrObject classOrObject,
            @NotNull ClassDescriptor classDescriptor
    ) {
        checkSupertypesForConsistency(classDescriptor, classOrObject);
    }

    private void checkSupertypesForConsistency(
            @NotNull KtTypeParameter typeParameter,
            @NotNull TypeParameterDescriptor typeParameterDescriptor
    ) {
        checkSupertypesForConsistency(typeParameterDescriptor, typeParameter);
    }

    private void checkSupertypesForConsistency(
            @NotNull ClassifierDescriptor classifierDescriptor,
            @NotNull PsiElement sourceElement
    ) {
        Multimap<TypeConstructor, TypeProjection> multimap =
                SubstitutionUtils.buildDeepSubstitutionMultimap(classifierDescriptor.getDefaultType());
        for (Map.Entry<TypeConstructor, Collection<TypeProjection>> entry : multimap.asMap().entrySet()) {
            Collection<TypeProjection> projections = entry.getValue();
            if (projections.size() > 1) {
                TypeConstructor typeConstructor = entry.getKey();
                DeclarationDescriptor declarationDescriptor = typeConstructor.getDeclarationDescriptor();
                assert declarationDescriptor instanceof TypeParameterDescriptor : declarationDescriptor;
                TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) declarationDescriptor;

                // Immediate arguments of supertypes cannot be projected
                Set<KotlinType> conflictingTypes = Sets.newLinkedHashSet();
                for (TypeProjection projection : projections) {
                    conflictingTypes.add(projection.getType());
                }
                removeDuplicateTypes(conflictingTypes);
                if (conflictingTypes.size() > 1) {
                    DeclarationDescriptor containingDeclaration = typeParameterDescriptor.getContainingDeclaration();
                    assert containingDeclaration instanceof ClassDescriptor : containingDeclaration;
                    if (sourceElement instanceof KtClassOrObject) {
                        KtDelegationSpecifierList delegationSpecifierList = ((KtClassOrObject) sourceElement).getDelegationSpecifierList();
                        assert delegationSpecifierList != null;
                        //                        trace.getErrorHandler().genericError(delegationSpecifierList.getNode(), "Type parameter " + typeParameterDescriptor.getName() + " of " + containingDeclaration.getName() + " has inconsistent values: " + conflictingTypes);
                        trace.report(INCONSISTENT_TYPE_PARAMETER_VALUES
                                             .on(delegationSpecifierList, typeParameterDescriptor, (ClassDescriptor) containingDeclaration,
                                                 conflictingTypes));
                    }
                    else if (sourceElement instanceof KtTypeParameter) {
                        trace.report(INCONSISTENT_TYPE_PARAMETER_BOUNDS
                                             .on((KtTypeParameter) sourceElement, typeParameterDescriptor, (ClassDescriptor) containingDeclaration,
                                                 conflictingTypes));
                    }
                }
            }
        }
    }

    private void checkClassExposedType(@NotNull KtClassOrObject klass, @NotNull ClassDescriptor classDescriptor) {
        checkExposedSupertypes(klass, classDescriptor);
        checkExposedParameterBounds(klass, classDescriptor);

        if (classDescriptor.getUnsubstitutedPrimaryConstructor() != null && klass.getPrimaryConstructor() != null) {
            checkFunctionExposedType(klass.getPrimaryConstructor(), classDescriptor.getUnsubstitutedPrimaryConstructor());
        }
    }

    private void checkExposedParameterBounds(@NotNull KtClassOrObject klass, @NotNull ClassDescriptor classDescriptor) {
        EffectiveVisibility classVisibility = EffectiveVisibility.Companion.forClass(classDescriptor);
        List<KtTypeParameter> typeParameterList = klass.getTypeParameters();
        int i = 0;
        for (TypeParameterDescriptor typeParameterDescriptor : classDescriptor.getTypeConstructor().getParameters()) {
            if (i >= typeParameterList.size()) return;
            for (KotlinType upperBound : typeParameterDescriptor.getUpperBounds()) {
                EffectiveVisibility upperBoundVisibility = EffectiveVisibility.Companion.forType(upperBound);
                if (!upperBoundVisibility.sameOrMorePermissive(classVisibility)) {
                    KtTypeParameter typeParameter = typeParameterList.get(i);
                    trace.report(EXPOSED_TYPE_PARAMETER_BOUND.on(typeParameter, classVisibility, upperBoundVisibility));
                    break;
                }
            }
            i++;
        }
    }

    private void checkExposedSupertypes(@NotNull KtClassOrObject klass, @NotNull ClassDescriptor classDescriptor) {
        EffectiveVisibility classVisibility = EffectiveVisibility.Companion.forClass(classDescriptor);
        boolean isInterface = classDescriptor.getKind() == ClassKind.INTERFACE;
        List<KtDelegationSpecifier> delegationList = klass.getDelegationSpecifiers();
        int i = -1;
        for (KotlinType superType : classDescriptor.getTypeConstructor().getSupertypes()) {
            i++;
            if (i >= delegationList.size()) return;
            ClassDescriptor superDescriptor = TypeUtils.getClassDescriptor(superType);
            if (superDescriptor == null) {
                continue;
            }
            boolean superIsInterface = superDescriptor.getKind() == ClassKind.INTERFACE;
            if (superIsInterface != isInterface) {
                continue;
            }
            EffectiveVisibility superTypeVisibility = EffectiveVisibility.Companion.forType(superType);
            if (!superTypeVisibility.sameOrMorePermissive(classVisibility)) {
                if (isInterface) {
                    trace.report(EXPOSED_SUPER_INTERFACE.on(delegationList.get(i), classVisibility, superTypeVisibility));
                }
                else {
                    trace.report(EXPOSED_SUPER_CLASS.on(delegationList.get(i), classVisibility, superTypeVisibility));
                }
            }
        }
    }

    private static void removeDuplicateTypes(Set<KotlinType> conflictingTypes) {
        for (Iterator<KotlinType> iterator = conflictingTypes.iterator(); iterator.hasNext(); ) {
            KotlinType type = iterator.next();
            for (KotlinType otherType : conflictingTypes) {
                boolean subtypeOf = KotlinTypeChecker.DEFAULT.equalTypes(type, otherType);
                if (type != otherType && subtypeOf) {
                    iterator.remove();
                    break;
                }
            }
        }
    }

    private void checkObject(KtObjectDeclaration declaration, ClassDescriptor classDescriptor) {
        if  (declaration.isLocal() && !declaration.isCompanion() && !declaration.isObjectLiteral()) {
            trace.report(LOCAL_OBJECT_NOT_ALLOWED.on(declaration, classDescriptor));
        }
    }

    private void checkClass(BodiesResolveContext c, KtClass aClass, ClassDescriptorWithResolutionScopes classDescriptor) {
        checkOpenMembers(classDescriptor);
        checkTypeParameters(aClass);
        checkTypeParameterConstraints(aClass);
        FiniteBoundRestrictionChecker.check(aClass, classDescriptor, trace);
        NonExpansiveInheritanceRestrictionChecker.check(aClass, classDescriptor, trace);

        if (aClass.isInterface()) {
            checkConstructorInInterface(aClass);
            checkMethodsOfAnyInInterface(classDescriptor);
            if (aClass.isLocal() && !(classDescriptor.getContainingDeclaration() instanceof ClassDescriptor)) {
                trace.report(LOCAL_INTERFACE_NOT_ALLOWED.on(aClass, classDescriptor));
            }
        }
        else if (classDescriptor.getKind() == ClassKind.ANNOTATION_CLASS) {
            checkAnnotationClassWithBody(aClass);
            checkValOnAnnotationParameter(aClass);
        }
        else if (aClass instanceof KtEnumEntry) {
            checkEnumEntry((KtEnumEntry) aClass, classDescriptor);
        }
        for (CallableMemberDescriptor memberDescriptor : classDescriptor.getDeclaredCallableMembers()) {
            if (memberDescriptor.getKind() != CallableMemberDescriptor.Kind.DECLARATION) continue;
            KtNamedDeclaration member = (KtNamedDeclaration) DescriptorToSourceUtils.descriptorToDeclaration(memberDescriptor);
            if (member instanceof KtFunction && memberDescriptor instanceof FunctionDescriptor) {
                checkFunctionExposedType((KtFunction) member, (FunctionDescriptor) memberDescriptor);
            }
        }
    }

    private void checkPrimaryConstructor(KtClassOrObject classOrObject, ClassDescriptor classDescriptor) {
        ConstructorDescriptor primaryConstructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
        KtPrimaryConstructor declaration = classOrObject.getPrimaryConstructor();
        if (primaryConstructor == null || declaration == null) return;

        for (KtParameter parameter : declaration.getValueParameters()) {
            PropertyDescriptor propertyDescriptor = trace.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
            if (propertyDescriptor != null) {
                modifiersChecker.checkModifiersForDeclaration(parameter, propertyDescriptor);
                checkPropertyLateInit(parameter, propertyDescriptor);
            }
        }

        if (declaration.getModifierList() != null && !declaration.hasConstructorKeyword()) {
            trace.report(MISSING_CONSTRUCTOR_KEYWORD.on(declaration.getModifierList()));
        }

        if (!(classOrObject instanceof KtClass)) {
            trace.report(CONSTRUCTOR_IN_OBJECT.on(declaration));
        }

        checkConstructorDeclaration(primaryConstructor, declaration);
    }

    private void checkTypeParameters(KtTypeParameterListOwner typeParameterListOwner) {
        // TODO: Support annotation for type parameters
        for (KtTypeParameter jetTypeParameter : typeParameterListOwner.getTypeParameters()) {
            AnnotationResolver.reportUnsupportedAnnotationForTypeParameter(jetTypeParameter, trace);

            TypeParameterDescriptor typeParameter = trace.get(TYPE_PARAMETER, jetTypeParameter);
            if (typeParameter != null) {
                DescriptorResolver.checkConflictingUpperBounds(trace, typeParameter, jetTypeParameter);
            }
        }
    }

    private void checkTypeParameterConstraints(KtTypeParameterListOwner typeParameterListOwner) {
        List<KtTypeConstraint> constraints = typeParameterListOwner.getTypeConstraints();
        if (!constraints.isEmpty()) {
            for (KtTypeParameter typeParameter : typeParameterListOwner.getTypeParameters()) {
                if (typeParameter.getExtendsBound() != null && hasConstraints(typeParameter, constraints)) {
                    trace.report(MISPLACED_TYPE_PARAMETER_CONSTRAINTS.on(typeParameter));
                }
                TypeParameterDescriptor descriptor = trace.get(TYPE_PARAMETER, typeParameter);
                if (descriptor != null) {
                    checkSupertypesForConsistency(typeParameter, descriptor);
                }
            }
        }
    }

    private static boolean hasConstraints(KtTypeParameter typeParameter, List<KtTypeConstraint> constraints) {
        for (KtTypeConstraint constraint : constraints) {
            KtSimpleNameExpression parameterName = constraint.getSubjectTypeParameterName();
            if (parameterName != null && parameterName.getText().equals(typeParameter.getName())) {
                return true;
            }
        }
        return false;
    }

    private void checkConstructorInInterface(KtClass klass) {
        KtPrimaryConstructor primaryConstructor = klass.getPrimaryConstructor();
        if (primaryConstructor != null) {
            trace.report(CONSTRUCTOR_IN_INTERFACE.on(primaryConstructor));
        }
    }

    private void checkMethodsOfAnyInInterface(ClassDescriptorWithResolutionScopes classDescriptor) {
        for (CallableMemberDescriptor declaredCallableMember : classDescriptor.getDeclaredCallableMembers()) {
            if (!(declaredCallableMember instanceof FunctionDescriptor)) continue;
            FunctionDescriptor functionDescriptor = (FunctionDescriptor) declaredCallableMember;

            PsiElement declaration = DescriptorToSourceUtils.descriptorToDeclaration(functionDescriptor);
            if (!(declaration instanceof KtNamedFunction)) continue;
            KtNamedFunction functionDeclaration = (KtNamedFunction) declaration;

            if (isHidingParentMemberIfPresent(declaredCallableMember)) continue;

            if (isImplementingMethodOfAny(declaredCallableMember)) {
                trace.report(METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE.on(functionDeclaration));
            }
        }
    }

    private static final Set<String> METHOD_OF_ANY_NAMES = ImmutableSet.of("toString", "hashCode", "equals");

    private static boolean isImplementingMethodOfAny(CallableMemberDescriptor member) {
        if (!METHOD_OF_ANY_NAMES.contains(member.getName().asString())) return false;
        if (member.getModality() == Modality.ABSTRACT) return false;

        return isImplementingMethodOfAnyInternal(member, new HashSet<ClassDescriptor>());
    }

    private static boolean isImplementingMethodOfAnyInternal(CallableMemberDescriptor member, Set<ClassDescriptor> visitedClasses) {
        for (CallableMemberDescriptor overridden : member.getOverriddenDescriptors()) {
            DeclarationDescriptor containingDeclaration = overridden.getContainingDeclaration();
            if (!(containingDeclaration instanceof ClassDescriptor)) continue;
            ClassDescriptor containingClass = (ClassDescriptor) containingDeclaration;
            if (visitedClasses.contains(containingClass)) continue;

            if (DescriptorUtils.getFqName(containingClass).equals(KotlinBuiltIns.FQ_NAMES.any)) {
                return true;
            }

            if (isHidingParentMemberIfPresent(overridden)) continue;

            visitedClasses.add(containingClass);

            if (isImplementingMethodOfAnyInternal(overridden, visitedClasses)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isHidingParentMemberIfPresent(CallableMemberDescriptor member) {
        KtNamedDeclaration declaration = (KtNamedDeclaration) DescriptorToSourceUtils.descriptorToDeclaration(member);
        if (declaration != null) {
            KtModifierList modifierList = declaration.getModifierList();
            return modifierList == null || !modifierList.hasModifier(KtTokens.OVERRIDE_KEYWORD);
        }
        return false;
    }

    private void checkAnnotationClassWithBody(KtClassOrObject classOrObject) {
        if (classOrObject.getBody() != null) {
            trace.report(ANNOTATION_CLASS_WITH_BODY.on(classOrObject.getBody()));
        }
    }

    private void checkValOnAnnotationParameter(KtClass aClass) {
        for (KtParameter parameter : aClass.getPrimaryConstructorParameters()) {
            if (!parameter.hasValOrVar()) {
                trace.report(MISSING_VAL_ON_ANNOTATION_PARAMETER.on(parameter));
            }
        }
    }

    private void checkOpenMembers(ClassDescriptorWithResolutionScopes classDescriptor) {
        if (classCanHaveOpenMembers(classDescriptor)) return;

        for (CallableMemberDescriptor memberDescriptor : classDescriptor.getDeclaredCallableMembers()) {
            if (memberDescriptor.getKind() != CallableMemberDescriptor.Kind.DECLARATION) continue;
            KtNamedDeclaration member = (KtNamedDeclaration) DescriptorToSourceUtils.descriptorToDeclaration(memberDescriptor);
            if (member != null && member.hasModifier(KtTokens.OPEN_KEYWORD)) {
                trace.report(NON_FINAL_MEMBER_IN_FINAL_CLASS.on(member));
            }
        }
    }

    private void checkProperty(KtProperty property, PropertyDescriptor propertyDescriptor) {
        DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
        if (containingDeclaration instanceof ClassDescriptor) {
            checkPropertyAbstractness(property, propertyDescriptor, (ClassDescriptor) containingDeclaration);
        }
        checkPropertyLateInit(property, propertyDescriptor);
        checkPropertyInitializer(property, propertyDescriptor);
        checkAccessors(property, propertyDescriptor);
        checkTypeParameterConstraints(property);
        checkPropertyExposedType(property, propertyDescriptor);
        checkPropertyTypeParametersAreUsedInReceiverType(propertyDescriptor);
    }

    private void checkPropertyTypeParametersAreUsedInReceiverType(@NotNull PropertyDescriptor descriptor) {
        for (TypeParameterDescriptor typeParameter : descriptor.getTypeParameters()) {
            if (isTypeParameterUsedInReceiverType(typeParameter, descriptor)) continue;

            PsiElement typeParameterPsi = DescriptorToSourceUtils.getSourceFromDescriptor(typeParameter);
            if (typeParameterPsi instanceof KtTypeParameter) {
                trace.report(TYPE_PARAMETER_OF_PROPERTY_NOT_USED_IN_RECEIVER.on((KtTypeParameter) typeParameterPsi));
            }
        }
    }

    private static boolean isTypeParameterUsedInReceiverType(
            @NotNull final TypeParameterDescriptor parameter,
            @NotNull PropertyDescriptor descriptor
    ) {
        ReceiverParameterDescriptor receiverParameter = descriptor.getExtensionReceiverParameter();
        if (receiverParameter == null) return false;

        return TypeUtils.containsSpecialType(receiverParameter.getType(), new Function1<KotlinType, Boolean>() {
            @Override
            public Boolean invoke(KotlinType type) {
                return parameter.equals(type.getConstructor().getDeclarationDescriptor());
            }
        });
    }

    private void checkPropertyLateInit(@NotNull KtCallableDeclaration property, @NotNull PropertyDescriptor propertyDescriptor) {
        KtModifierList modifierList = property.getModifierList();
        if (modifierList == null) return;
        PsiElement modifier = modifierList.getModifier(KtTokens.LATEINIT_KEYWORD);
        if (modifier == null) return;

        if (!propertyDescriptor.isVar()) {
            trace.report(INAPPLICABLE_LATEINIT_MODIFIER.on(modifier, "is allowed only on mutable properties"));
        }

        boolean returnTypeIsNullable = true;
        boolean returnTypeIsPrimitive = true;

        KotlinType returnType = propertyDescriptor.getReturnType();
        if (returnType != null) {
            returnTypeIsNullable = TypeUtils.isNullableType(returnType);
            returnTypeIsPrimitive = KotlinBuiltIns.isPrimitiveType(returnType);
        }

        if (returnTypeIsNullable) {
            trace.report(INAPPLICABLE_LATEINIT_MODIFIER.on(modifier, "is not allowed on nullable properties"));
        }

        if (returnTypeIsPrimitive) {
            trace.report(INAPPLICABLE_LATEINIT_MODIFIER.on(modifier, "is not allowed on primitive type properties"));
        }

        boolean isAbstract = propertyDescriptor.getModality() == Modality.ABSTRACT;
        if (isAbstract) {
            trace.report(INAPPLICABLE_LATEINIT_MODIFIER.on(modifier, "is not allowed on abstract properties"));
        }

        if (property instanceof KtParameter) {
            trace.report(INAPPLICABLE_LATEINIT_MODIFIER.on(modifier, "is not allowed on primary constructor parameters"));
        }

        boolean hasDelegateExpressionOrInitializer = false;
        if (property instanceof KtProperty) {
            hasDelegateExpressionOrInitializer = ((KtProperty) property).hasDelegateExpressionOrInitializer();
            if (hasDelegateExpressionOrInitializer) {
                trace.report(INAPPLICABLE_LATEINIT_MODIFIER.on(modifier,
                        "is not allowed on properties with initializer or on delegated properties"));
            }
        }

        PropertyGetterDescriptor getter = propertyDescriptor.getGetter();
        PropertySetterDescriptor setter = propertyDescriptor.getSetter();

        boolean customGetterOrSetter = false;
        if (getter != null) {
            customGetterOrSetter = getter.hasBody();
        }
        if (setter != null) {
            customGetterOrSetter |= setter.hasBody();
        }

        if (!hasDelegateExpressionOrInitializer && customGetterOrSetter) {
            trace.report(INAPPLICABLE_LATEINIT_MODIFIER.on(modifier, "is not allowed on properties with a custom getter or setter"));
        }

        boolean hasBackingField =
                Boolean.TRUE.equals(trace.getBindingContext().get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor));

        if (!isAbstract && !customGetterOrSetter && !hasDelegateExpressionOrInitializer && !hasBackingField) {
            trace.report(INAPPLICABLE_LATEINIT_MODIFIER.on(modifier, "is not allowed on properties without backing field"));
        }

        if (propertyDescriptor.getExtensionReceiverParameter() != null) {
            trace.report(INAPPLICABLE_LATEINIT_MODIFIER.on(modifier, "is not allowed on extension properties"));
        }
    }

    private void checkPropertyAbstractness(
            @NotNull KtProperty property,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull ClassDescriptor classDescriptor
    ) {
        KtPropertyAccessor getter = property.getGetter();
        KtPropertyAccessor setter = property.getSetter();
        KtModifierList modifierList = property.getModifierList();

        if (modifierList != null && modifierList.hasModifier(KtTokens.ABSTRACT_KEYWORD)) { //has abstract modifier
            if (!classCanHaveAbstractMembers(classDescriptor)) {
                String name = property.getName();
                trace.report(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS.on(property, name != null ? name : "", classDescriptor));
                return;
            }
            if (classDescriptor.getKind() == ClassKind.INTERFACE) {
                trace.report(ABSTRACT_MODIFIER_IN_INTERFACE.on(property));
            }
        }

        if (propertyDescriptor.getModality() == Modality.ABSTRACT) {
            KtExpression initializer = property.getInitializer();
            if (initializer != null) {
                trace.report(ABSTRACT_PROPERTY_WITH_INITIALIZER.on(initializer));
            }
            KtPropertyDelegate delegate = property.getDelegate();
            if (delegate != null) {
                trace.report(ABSTRACT_DELEGATED_PROPERTY.on(delegate));
            }
            if (getter != null && getter.hasBody()) {
                trace.report(ABSTRACT_PROPERTY_WITH_GETTER.on(getter));
            }
            if (setter != null && setter.hasBody()) {
                trace.report(ABSTRACT_PROPERTY_WITH_SETTER.on(setter));
            }
        }
    }

    private void checkPropertyInitializer(@NotNull KtProperty property, @NotNull PropertyDescriptor propertyDescriptor) {
        KtPropertyAccessor getter = property.getGetter();
        KtPropertyAccessor setter = property.getSetter();
        boolean hasAccessorImplementation = (getter != null && getter.hasBody()) ||
                                            (setter != null && setter.hasBody());

        DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
        boolean inTrait = containingDeclaration instanceof ClassDescriptor && ((ClassDescriptor)containingDeclaration).getKind() == ClassKind.INTERFACE;
        if (propertyDescriptor.getModality() == Modality.ABSTRACT) {
            if (!property.hasDelegateExpressionOrInitializer() && property.getTypeReference() == null) {
                trace.report(PROPERTY_WITH_NO_TYPE_NO_INITIALIZER.on(property));
            }
            if (inTrait && property.hasModifier(KtTokens.PRIVATE_KEYWORD) && !property.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
                trace.report(PRIVATE_PROPERTY_IN_INTERFACE.on(property));
            }
            return;
        }
        KtExpression initializer = property.getInitializer();
        KtPropertyDelegate delegate = property.getDelegate();
        boolean backingFieldRequired =
                Boolean.TRUE.equals(trace.getBindingContext().get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor));

        if (inTrait && backingFieldRequired && hasAccessorImplementation) {
            trace.report(BACKING_FIELD_IN_INTERFACE.on(property));
        }

        if (initializer == null && delegate == null) {
            boolean error = false;
            if (backingFieldRequired && !inTrait && !propertyDescriptor.isLateInit() &&
                Boolean.TRUE.equals(trace.getBindingContext().get(BindingContext.IS_UNINITIALIZED, propertyDescriptor))) {
                if (!(containingDeclaration instanceof ClassDescriptor) || hasAccessorImplementation) {
                    error = true;
                    trace.report(MUST_BE_INITIALIZED.on(property));
                }
                else {
                    error = true;
                    trace.report(MUST_BE_INITIALIZED_OR_BE_ABSTRACT.on(property));
                }
            }
            if (!error && property.getTypeReference() == null) {
                trace.report(PROPERTY_WITH_NO_TYPE_NO_INITIALIZER.on(property));
            }
            if (inTrait && property.hasModifier(KtTokens.FINAL_KEYWORD) && backingFieldRequired) {
                trace.report(FINAL_PROPERTY_IN_INTERFACE.on(property));
            }
            return;
        }

        if (inTrait) {
            if (delegate != null) {
                trace.report(DELEGATED_PROPERTY_IN_INTERFACE.on(delegate));
            }
            else {
                trace.report(PROPERTY_INITIALIZER_IN_INTERFACE.on(initializer));
            }
        }
        else if (delegate == null) {
            if (!backingFieldRequired) {
                trace.report(PROPERTY_INITIALIZER_NO_BACKING_FIELD.on(initializer));
            }
            else if (property.getReceiverTypeReference() != null) {
                trace.report(EXTENSION_PROPERTY_WITH_BACKING_FIELD.on(initializer));
            }
        }
    }

    private void checkMemberReceiverExposedType(@Nullable KtTypeReference typeReference, @NotNull CallableMemberDescriptor memberDescriptor) {
        if (typeReference == null) return;
        ReceiverParameterDescriptor receiverParameterDescriptor = memberDescriptor.getExtensionReceiverParameter();
        if (receiverParameterDescriptor == null) return;
        EffectiveVisibility memberVisibility = EffectiveVisibility.Companion.forMember(memberDescriptor);
        EffectiveVisibility receiverTypeVisibility = EffectiveVisibility.Companion.forType(receiverParameterDescriptor.getType());
        if (!receiverTypeVisibility.sameOrMorePermissive(memberVisibility)) {
            trace.report(EXPOSED_RECEIVER_TYPE.on(typeReference, memberVisibility, receiverTypeVisibility));
        }
    }

    private void checkPropertyExposedType(@NotNull KtProperty property, @NotNull PropertyDescriptor propertyDescriptor) {
        EffectiveVisibility propertyVisibility = EffectiveVisibility.Companion.forMember(propertyDescriptor);
        EffectiveVisibility typeVisibility = EffectiveVisibility.Companion.forType(propertyDescriptor.getType());
        if (!typeVisibility.sameOrMorePermissive(propertyVisibility)) {
            trace.report(EXPOSED_PROPERTY_TYPE.on(property, propertyVisibility, typeVisibility));
        }
        checkMemberReceiverExposedType(property.getReceiverTypeReference(), propertyDescriptor);
    }

    protected void checkFunction(KtNamedFunction function, SimpleFunctionDescriptor functionDescriptor) {
        KtTypeParameterList typeParameterList = function.getTypeParameterList();
        PsiElement nameIdentifier = function.getNameIdentifier();
        if (typeParameterList != null && nameIdentifier != null &&
            typeParameterList.getTextRange().getStartOffset() > nameIdentifier.getTextRange().getStartOffset()) {
            trace.report(DEPRECATED_TYPE_PARAMETER_SYNTAX.on(typeParameterList));
        }
        checkTypeParameterConstraints(function);

        DeclarationDescriptor containingDescriptor = functionDescriptor.getContainingDeclaration();
        boolean hasAbstractModifier = function.hasModifier(KtTokens.ABSTRACT_KEYWORD);
        boolean hasExternalModifier = function.hasModifier(KtTokens.EXTERNAL_KEYWORD);

        if (containingDescriptor instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) containingDescriptor;
            boolean inTrait = classDescriptor.getKind() == ClassKind.INTERFACE;
            if (hasAbstractModifier && !classCanHaveAbstractMembers(classDescriptor)) {
                trace.report(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS.on(function, functionDescriptor.getName().asString(), classDescriptor));
            }
            if (hasAbstractModifier && inTrait) {
                trace.report(ABSTRACT_MODIFIER_IN_INTERFACE.on(function));
            }
            boolean hasBody = function.hasBody();
            if (hasBody && hasAbstractModifier) {
                trace.report(ABSTRACT_FUNCTION_WITH_BODY.on(function, functionDescriptor));
            }
            if (!hasBody && inTrait) {
                if (function.hasModifier(KtTokens.FINAL_KEYWORD) && !hasExternalModifier) {
                    trace.report(FINAL_FUNCTION_WITH_NO_BODY.on(function, functionDescriptor));
                }
                if (function.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
                    trace.report(PRIVATE_FUNCTION_WITH_NO_BODY.on(function, functionDescriptor));
                }
            }
            if (!hasBody && !hasAbstractModifier && !hasExternalModifier && !inTrait) {
                trace.report(NON_ABSTRACT_FUNCTION_WITH_NO_BODY.on(function, functionDescriptor));
            }
            return;
        }
        if (!function.hasBody() && !hasAbstractModifier && !hasExternalModifier) {
            trace.report(NON_MEMBER_FUNCTION_NO_BODY.on(function, functionDescriptor));
        }
        if (TypeUtilsKt.isNothing(functionDescriptor.getReturnType()) && !function.hasDeclaredReturnType()) {
            trace.report(IMPLICIT_NOTHING_RETURN_TYPE.on(function.getNameIdentifier() != null ? function.getNameIdentifier() : function));
        }
        checkFunctionExposedType(function, functionDescriptor);
    }

    private void checkFunctionExposedType(@NotNull KtFunction function, @NotNull FunctionDescriptor functionDescriptor) {
        EffectiveVisibility functionVisibility = EffectiveVisibility.Companion.forMember(functionDescriptor);
        if (!(function instanceof KtConstructor)) {
            EffectiveVisibility returnTypeVisibility = EffectiveVisibility.Companion.forType(functionDescriptor.getReturnType());
            if (!returnTypeVisibility.sameOrMorePermissive(functionVisibility)) {
                PsiElement reportOn = function.getNameIdentifier();
                if (reportOn == null) {
                    reportOn = function;
                }
                trace.report(EXPOSED_FUNCTION_RETURN_TYPE.on(reportOn, functionVisibility, returnTypeVisibility));
            }
        }
        int i = 0;
        for (ValueParameterDescriptor parameterDescriptor : functionDescriptor.getValueParameters()) {
            EffectiveVisibility typeVisibility = EffectiveVisibility.Companion.forType(parameterDescriptor.getType());
            if (!typeVisibility.sameOrMorePermissive(functionVisibility) && i < function.getValueParameters().size()) {
                trace.report(EXPOSED_PARAMETER_TYPE.on(function.getValueParameters().get(i), functionVisibility, typeVisibility));
            }
            i++;
        }
        checkMemberReceiverExposedType(function.getReceiverTypeReference(), functionDescriptor);
    }

    private void checkAccessors(@NotNull KtProperty property, @NotNull PropertyDescriptor propertyDescriptor) {
        for (KtPropertyAccessor accessor : property.getAccessors()) {
            PropertyAccessorDescriptor propertyAccessorDescriptor = accessor.isGetter() ? propertyDescriptor.getGetter() : propertyDescriptor.getSetter();
            assert propertyAccessorDescriptor != null : "No property accessor descriptor for " + property.getText();
            modifiersChecker.checkModifiersForDeclaration(accessor, propertyAccessorDescriptor);
            identifierChecker.checkDeclaration(accessor, trace);
        }
        checkAccessor(propertyDescriptor, property.getGetter(), propertyDescriptor.getGetter());
        checkAccessor(propertyDescriptor, property.getSetter(), propertyDescriptor.getSetter());
    }

    private void reportVisibilityModifierDiagnostics(Collection<PsiElement> tokens, DiagnosticFactory0<PsiElement> diagnostic) {
        for (PsiElement token : tokens) {
            trace.report(diagnostic.on(token));
        }
    }

    private void checkAccessor(
            @NotNull PropertyDescriptor propertyDescriptor,
            @Nullable KtPropertyAccessor accessor,
            @Nullable PropertyAccessorDescriptor accessorDescriptor
    ) {
        if (accessor == null) return;
        KtModifierList accessorModifierList = accessor.getModifierList();
        if (accessorModifierList != null && accessorDescriptor != null) {
            Map<KtModifierKeywordToken, PsiElement> tokens = modifiersChecker.getTokensCorrespondingToModifiers(accessorModifierList, Sets
                    .newHashSet(KtTokens.PUBLIC_KEYWORD, KtTokens.PROTECTED_KEYWORD, KtTokens.PRIVATE_KEYWORD,
                                KtTokens.INTERNAL_KEYWORD));
            if (accessor.isGetter()) {
                if (accessorDescriptor.getVisibility() != propertyDescriptor.getVisibility()) {
                    reportVisibilityModifierDiagnostics(tokens.values(), Errors.GETTER_VISIBILITY_DIFFERS_FROM_PROPERTY_VISIBILITY);
                }
                else {
                    reportVisibilityModifierDiagnostics(tokens.values(), Errors.REDUNDANT_MODIFIER_IN_GETTER);
                }
            }
            else {
                if (propertyDescriptor.getModality() == Modality.ABSTRACT
                    && accessorDescriptor.getVisibility() == Visibilities.PRIVATE
                    && propertyDescriptor.getVisibility() != Visibilities.PRIVATE) {
                    reportVisibilityModifierDiagnostics(tokens.values(), Errors.ACCESSOR_VISIBILITY_FOR_ABSTRACT_PROPERTY);
                }
                else if (propertyDescriptor.isLateInit() && accessorDescriptor.getVisibility() != propertyDescriptor.getVisibility()) {
                    reportVisibilityModifierDiagnostics(tokens.values(), Errors.SETTER_VISIBILITY_DIFFERS_FROM_LATEINIT_VISIBILITY);
                }
            }
        }
    }

    private void checkEnumEntry(@NotNull KtEnumEntry enumEntry, @NotNull ClassDescriptor classDescriptor) {
        DeclarationDescriptor declaration = classDescriptor.getContainingDeclaration();
        if (DescriptorUtils.isEnumClass(declaration)) {
            if (!enumEntry.hasInitializer() && !hasDefaultConstructor((ClassDescriptor) declaration)) {
                trace.report(ENUM_ENTRY_SHOULD_BE_INITIALIZED.on(enumEntry));
            }
        }
        else {
            assert DescriptorUtils.isInterface(declaration) : "Enum entry should be declared in enum class: " + classDescriptor;
        }
    }

    private static boolean hasDefaultConstructor(@NotNull ClassDescriptor classDescriptor) {
        for (ConstructorDescriptor constructor : classDescriptor.getConstructors()) {
            if (constructor.getValueParameters().isEmpty()) return true;
        }
        return false;
    }
}
