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

package org.jetbrains.kotlin.resolve.calls.smartcasts;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.JetNodeTypes;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.scopes.receivers.*;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeUtils;

import static org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET;

/**
 * This class is intended to create data flow values for different kind of expressions.
 * Then data flow values serve as keys to obtain data flow information for these expressions.
 */
public class DataFlowValueFactory {
    private DataFlowValueFactory() {
    }

    @NotNull
    public static DataFlowValue createDataFlowValue(
            @NotNull JetExpression expression,
            @NotNull JetType type,
            @NotNull ResolutionContext resolutionContext
    ) {
        return createDataFlowValue(expression, type, resolutionContext.trace.getBindingContext(),
                                   resolutionContext.scope.getContainingDeclaration());
    }

    @NotNull
    public static DataFlowValue createDataFlowValue(
            @NotNull JetExpression expression,
            @NotNull JetType type,
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor containingDeclaration
    ) {
        if (expression instanceof JetConstantExpression) {
            JetConstantExpression constantExpression = (JetConstantExpression) expression;
            if (constantExpression.getNode().getElementType() == JetNodeTypes.NULL) return DataFlowValue.NULL;
        }
        if (type.isError()) return DataFlowValue.ERROR;
        if (KotlinBuiltIns.getInstance().getNullableNothingType().equals(type)) {
            return DataFlowValue.NULL; // 'null' is the only inhabitant of 'Nothing?'
        }
        IdentifierInfo result = getIdForStableIdentifier(expression, bindingContext, containingDeclaration);
        return new DataFlowValue(result == NO_IDENTIFIER_INFO ? expression : result.id, type, result.isStable, getImmanentNullability(type));
    }

    @NotNull
    public static DataFlowValue createDataFlowValue(@NotNull ThisReceiver receiver) {
        JetType type = receiver.getType();
        return new DataFlowValue(receiver, type, true, getImmanentNullability(type));
    }

    @NotNull
    public static DataFlowValue createDataFlowValue(
            @NotNull ReceiverValue receiverValue,
            @NotNull ResolutionContext resolutionContext
    ) {
        return createDataFlowValue(receiverValue, resolutionContext.trace.getBindingContext(),
                                   resolutionContext.scope.getContainingDeclaration());
    }

    @NotNull
    public static DataFlowValue createDataFlowValue(
            @NotNull ReceiverValue receiverValue,
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor containingDeclaration
    ) {
        if (receiverValue instanceof TransientReceiver || receiverValue instanceof ScriptReceiver) {
            // SCRIPT: smartcasts data flow
            JetType type = receiverValue.getType();
            boolean nullable = type.isMarkedNullable() || TypeUtils.hasNullableSuperType(type);
            return new DataFlowValue(receiverValue, type, nullable, Nullability.NOT_NULL);
        }
        else if (receiverValue instanceof ClassReceiver || receiverValue instanceof ExtensionReceiver) {
            return createDataFlowValue((ThisReceiver) receiverValue);
        }
        else if (receiverValue instanceof ExpressionReceiver) {
            return createDataFlowValue(((ExpressionReceiver) receiverValue).getExpression(),
                                       receiverValue.getType(),
                                       bindingContext,
                                       containingDeclaration);
        }
        else if (receiverValue == ReceiverValue.NO_RECEIVER) {
            throw new IllegalArgumentException("No DataFlowValue exists for ReceiverValue.NO_RECEIVER");
        }
        else {
            throw new UnsupportedOperationException("Unsupported receiver value: " + receiverValue.getClass().getName());
        }
    }

    @NotNull
    public static DataFlowValue createDataFlowValue(
            @NotNull VariableDescriptor variableDescriptor,
            @Nullable ModuleDescriptor usageContainingModule
    ) {
        JetType type = variableDescriptor.getType();
        return new DataFlowValue(variableDescriptor, type,
                                 isStableVariable(variableDescriptor, usageContainingModule),
                                 getImmanentNullability(type));
    }

    @NotNull
    private static Nullability getImmanentNullability(@NotNull JetType type) {
        return TypeUtils.isNullableType(type) ? Nullability.UNKNOWN : Nullability.NOT_NULL;
    }

    private static class IdentifierInfo {
        public final Object id;
        public final boolean isStable;
        public final boolean isPackage;

        private IdentifierInfo(Object id, boolean isStable, boolean isPackage) {
            this.id = id;
            this.isStable = isStable;
            this.isPackage = isPackage;
        }
    }

    private static final IdentifierInfo NO_IDENTIFIER_INFO = new IdentifierInfo(null, false, false) {
        @Override
        public String toString() {
            return "NO_IDENTIFIER_INFO";
        }
    };

    @NotNull
    private static IdentifierInfo createInfo(Object id, boolean isStable) {
        return new IdentifierInfo(id, isStable, false);
    }

    @NotNull
    private static IdentifierInfo createPackageInfo(Object id) {
        return new IdentifierInfo(id, true, true);
    }

    @NotNull
    private static IdentifierInfo combineInfo(@Nullable IdentifierInfo receiverInfo, @NotNull IdentifierInfo selectorInfo) {
        if (selectorInfo.id == null) {
            return NO_IDENTIFIER_INFO;
        }
        if (receiverInfo == null || receiverInfo == NO_IDENTIFIER_INFO || receiverInfo.isPackage) {
            return selectorInfo;
        }
        return createInfo(Pair.create(receiverInfo.id, selectorInfo.id), receiverInfo.isStable && selectorInfo.isStable);
    }

    @NotNull
    private static IdentifierInfo getIdForStableIdentifier(
            @Nullable JetExpression expression,
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor containingDeclaration
    ) {
        if (expression != null) {
            JetExpression deparenthesized = JetPsiUtil.deparenthesize(expression);
            if (expression != deparenthesized) {
                return getIdForStableIdentifier(deparenthesized, bindingContext, containingDeclaration);
            }
        }
        if (expression instanceof JetQualifiedExpression) {
            JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) expression;
            JetExpression receiverExpression = qualifiedExpression.getReceiverExpression();
            JetExpression selectorExpression = qualifiedExpression.getSelectorExpression();
            IdentifierInfo receiverId = getIdForStableIdentifier(receiverExpression, bindingContext, containingDeclaration);
            IdentifierInfo selectorId = getIdForStableIdentifier(selectorExpression, bindingContext, containingDeclaration);

            return combineInfo(receiverId, selectorId);
        }
        if (expression instanceof JetSimpleNameExpression) {
            return getIdForSimpleNameExpression((JetSimpleNameExpression) expression, bindingContext, containingDeclaration);
        }
        else if (expression instanceof JetThisExpression) {
            JetThisExpression thisExpression = (JetThisExpression) expression;
            DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, thisExpression.getInstanceReference());

            return getIdForThisReceiver(declarationDescriptor);
        }
        else if (expression instanceof JetRootPackageExpression) {
            //todo return createPackageInfo());
        }
        return NO_IDENTIFIER_INFO;
    }

    @NotNull
    private static IdentifierInfo getIdForSimpleNameExpression(
            @NotNull JetSimpleNameExpression simpleNameExpression,
            @NotNull BindingContext bindingContext,
            @NotNull DeclarationDescriptor containingDeclaration
    ) {
        DeclarationDescriptor declarationDescriptor = bindingContext.get(REFERENCE_TARGET, simpleNameExpression);
        if (declarationDescriptor instanceof VariableDescriptor) {
            ResolvedCall<?> resolvedCall = CallUtilPackage.getResolvedCall(simpleNameExpression, bindingContext);

            // todo uncomment assert
            // KT-4113
            // for now it fails for resolving 'invoke' convention, return it after 'invoke' algorithm changes
            // assert resolvedCall != null : "Cannot create right identifier info if the resolved call is not known yet for
            ModuleDescriptor usageModuleDescriptor = DescriptorUtils.getContainingModuleOrNull(containingDeclaration);
            IdentifierInfo receiverInfo =
                    resolvedCall != null ? getIdForImplicitReceiver(resolvedCall.getDispatchReceiver(), simpleNameExpression) : null;

            VariableDescriptor variableDescriptor = (VariableDescriptor) declarationDescriptor;
            return combineInfo(receiverInfo, createInfo(variableDescriptor,
                                                        isStableVariable(variableDescriptor, usageModuleDescriptor)));
        }
        if (declarationDescriptor instanceof PackageViewDescriptor) {
            return createPackageInfo(declarationDescriptor);
        }
        return NO_IDENTIFIER_INFO;
    }

    @Nullable
    private static IdentifierInfo getIdForImplicitReceiver(@NotNull ReceiverValue receiverValue, @Nullable JetExpression expression) {
        if (receiverValue instanceof ThisReceiver) {
            return getIdForThisReceiver(((ThisReceiver) receiverValue).getDeclarationDescriptor());
        }
        else {
            assert !(receiverValue instanceof TransientReceiver)
                    : "Transient receiver is implicit for an explicit expression: " + expression + ". Receiver: " + receiverValue;
            // For ExpressionReceiver there is an explicit "this" expression and it was analyzed earlier
            return null;
        }
    }

    @NotNull
    private static IdentifierInfo getIdForThisReceiver(@Nullable DeclarationDescriptor descriptorOfThisReceiver) {
        if (descriptorOfThisReceiver instanceof CallableDescriptor) {
            ReceiverParameterDescriptor receiverParameter = ((CallableDescriptor) descriptorOfThisReceiver).getExtensionReceiverParameter();
            assert receiverParameter != null : "'This' refers to the callable member without a receiver parameter: " +
                                               descriptorOfThisReceiver;
            return createInfo(receiverParameter.getValue(), true);
        }
        if (descriptorOfThisReceiver instanceof ClassDescriptor) {
            return createInfo(((ClassDescriptor) descriptorOfThisReceiver).getThisAsReceiverParameter().getValue(), true);
        }
        return NO_IDENTIFIER_INFO;
    }

    /**
     * Determines whether a variable with a given descriptor is stable or not at the given usage place.
     * <p/>
     * Stable means that the variable value cannot change. The simple (non-property) variable is considered stable if it's immutable (val).
     * <p/>
     * If the variable is a property, it's considered stable if it's immutable (val) AND it's final (not open) AND
     * the default getter is in use (otherwise nobody can guarantee that a getter is consistent) AND
     * (it's private OR internal OR used at the same module where it's defined).
     * The last check corresponds to a risk of changing property definition in another module, e.g. from "val" to "var".
     *
     * @param variableDescriptor    descriptor of a considered variable
     * @param usageModule a module with a considered usage place, or null if it's not known (not recommended)
     * @return true if variable is stable, false otherwise
     */
    public static boolean isStableVariable(
            @NotNull VariableDescriptor variableDescriptor,
            @Nullable ModuleDescriptor usageModule
    ) {
        if (variableDescriptor.isVar()) return false;
        if (variableDescriptor instanceof PropertyDescriptor) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) variableDescriptor;
            if (!isFinal(propertyDescriptor)) return false;
            if (!hasDefaultGetter(propertyDescriptor)) return false;
            if (!invisibleFromOtherModules(propertyDescriptor)) {
                ModuleDescriptor declarationModule = DescriptorUtils.getContainingModule(propertyDescriptor);
                if (usageModule == null || !usageModule.equals(declarationModule)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isFinal(PropertyDescriptor propertyDescriptor) {
        DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
        if (containingDeclaration instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
            if (classDescriptor.getModality().isOverridable() && propertyDescriptor.getModality().isOverridable()) return false;
        }
        else {
            if (propertyDescriptor.getModality().isOverridable()) {
                throw new IllegalStateException("Property outside a class must not be overridable: " + propertyDescriptor.getName());
            }
        }
        return true;
    }

    private static boolean invisibleFromOtherModules(@NotNull DeclarationDescriptorWithVisibility descriptor) {
        if (Visibilities.INVISIBLE_FROM_OTHER_MODULES.contains(descriptor.getVisibility())) return true;

        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (!(containingDeclaration instanceof DeclarationDescriptorWithVisibility)) {
            return false;
        }

        return invisibleFromOtherModules((DeclarationDescriptorWithVisibility) containingDeclaration);
    }

    private static boolean hasDefaultGetter(PropertyDescriptor propertyDescriptor) {
        PropertyGetterDescriptor getter = propertyDescriptor.getGetter();
        return getter == null || getter.isDefault();
    }
}
