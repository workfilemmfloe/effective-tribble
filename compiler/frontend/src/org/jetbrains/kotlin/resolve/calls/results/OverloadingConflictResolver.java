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

package org.jetbrains.kotlin.resolve.calls.results;

import gnu.trove.THashSet;
import gnu.trove.TObjectHashingStrategy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.resolve.OverrideResolver;
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode;
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;

import java.util.List;
import java.util.Set;

public class OverloadingConflictResolver {

    private final KotlinBuiltIns builtIns;

    public OverloadingConflictResolver(@NotNull KotlinBuiltIns builtIns) {
        this.builtIns = builtIns;
    }

    @Nullable
    public <D extends CallableDescriptor> MutableResolvedCall<D> findMaximallySpecific(
            @NotNull Set<MutableResolvedCall<D>> candidates,
            boolean discriminateGenericDescriptors,
            @NotNull CheckArgumentTypesMode checkArgumentsMode
    ) {
        // Different smartcasts may lead to the same candidate descriptor wrapped into different ResolvedCallImpl objects
        Set<MutableResolvedCall<D>> maximallySpecific = new THashSet<MutableResolvedCall<D>>(new TObjectHashingStrategy<MutableResolvedCall<D>>() {
                    @Override
                    public boolean equals(MutableResolvedCall<D> o1, MutableResolvedCall<D> o2) {
                        return o1 == null ? o2 == null : o1.getResultingDescriptor().equals(o2.getResultingDescriptor());
                    }

                    @Override
                    public int computeHashCode(MutableResolvedCall<D> object) {
                        return object == null ? 0 : object.getResultingDescriptor().hashCode();
                    }
                });
        for (MutableResolvedCall<D> candidateCall : candidates) {
            if (isMaximallySpecific(candidateCall, candidates, discriminateGenericDescriptors, checkArgumentsMode)) {
                maximallySpecific.add(candidateCall);
            }
        }
        return maximallySpecific.size() == 1 ? maximallySpecific.iterator().next() : null;
    }

    private <D extends CallableDescriptor> boolean isMaximallySpecific(
            @NotNull MutableResolvedCall<D> candidateCall,
            @NotNull Set<MutableResolvedCall<D>> candidates,
            boolean discriminateGenericDescriptors,
            @NotNull CheckArgumentTypesMode checkArgumentsMode
    ) {
        D me = candidateCall.getResultingDescriptor();

        boolean isInvoke = candidateCall instanceof VariableAsFunctionResolvedCall;
        VariableDescriptor variable;
        if (isInvoke) {
            variable = ((VariableAsFunctionResolvedCall) candidateCall).getVariableCall().getResultingDescriptor();
        }
        else {
            variable = null;
        }

        for (MutableResolvedCall<D> otherCall : candidates) {
            D other = otherCall.getResultingDescriptor();
            if (other == me) continue;

            if (definitelyNotMaximallySpecific(me, other, discriminateGenericDescriptors, checkArgumentsMode)) {

                if (!isInvoke) return false;

                assert otherCall instanceof VariableAsFunctionResolvedCall : "'invoke' candidate goes with usual one: " + candidateCall + otherCall;
                ResolvedCall<VariableDescriptor> otherVariableCall = ((VariableAsFunctionResolvedCall) otherCall).getVariableCall();
                if (definitelyNotMaximallySpecific(variable, otherVariableCall.getResultingDescriptor(), discriminateGenericDescriptors, checkArgumentsMode)) {
                    return false;
                }
            }
        }
        return true;
    }

    private <D extends CallableDescriptor> boolean definitelyNotMaximallySpecific(
            D me,
            D other,
            boolean discriminateGenericDescriptors,
            @NotNull CheckArgumentTypesMode checkArgumentsMode
    ) {
        return !moreSpecific(me, other, discriminateGenericDescriptors, checkArgumentsMode) ||
               moreSpecific(other, me, discriminateGenericDescriptors, checkArgumentsMode);
    }

    /**
     * Let < mean "more specific"
     * Subtype < supertype
     * Double < Float
     * Int < Long
     * Int < Short < Byte
     */
    private <Descriptor extends CallableDescriptor> boolean moreSpecific(
            Descriptor f,
            Descriptor g,
            boolean discriminateGenericDescriptors,
            @NotNull CheckArgumentTypesMode checkArgumentsMode
    ) {
        boolean resolvingCallableReference = checkArgumentsMode == CheckArgumentTypesMode.CHECK_CALLABLE_TYPE;

        if (f.getContainingDeclaration() instanceof ScriptDescriptor && g.getContainingDeclaration() instanceof ScriptDescriptor) {
            ScriptDescriptor fs = (ScriptDescriptor) f.getContainingDeclaration();
            ScriptDescriptor gs = (ScriptDescriptor) g.getContainingDeclaration();

            if (fs.getPriority() != gs.getPriority()) {
                return fs.getPriority() > gs.getPriority();
            }
        }

        boolean isGenericF = isGeneric(f);
        boolean isGenericG = isGeneric(g);
        if (discriminateGenericDescriptors) {
            if (!isGenericF && isGenericG) return true;
            if (isGenericF && !isGenericG) return false;

            if (isGenericF && isGenericG) {
                return moreSpecific(BoundsSubstitutor.substituteBounds(f), BoundsSubstitutor.substituteBounds(g), false, checkArgumentsMode);
            }
        }

        if (OverrideResolver.overrides(f, g)) return true;
        if (OverrideResolver.overrides(g, f)) return false;

        ReceiverParameterDescriptor receiverOfF = f.getExtensionReceiverParameter();
        ReceiverParameterDescriptor receiverOfG = g.getExtensionReceiverParameter();
        if (receiverOfF != null && receiverOfG != null) {
            if (!typeMoreSpecific(receiverOfF.getType(), receiverOfG.getType())) return false;
        }

        List<ValueParameterDescriptor> fParams = f.getValueParameters();
        List<ValueParameterDescriptor> gParams = g.getValueParameters();

        int fSize = fParams.size();
        int gSize = gParams.size();

        boolean fIsVararg = isVariableArity(fParams);
        boolean gIsVararg = isVariableArity(gParams);

        if (resolvingCallableReference && fIsVararg != gIsVararg) return false;
        if (!fIsVararg && gIsVararg) return true;
        if (fIsVararg && !gIsVararg) return false;

        if (!fIsVararg && !gIsVararg) {
            if (resolvingCallableReference && fSize != gSize) return false;
            if (!resolvingCallableReference && fSize > gSize) return false;

            for (int i = 0; i < fSize; i++) {
                ValueParameterDescriptor fParam = fParams.get(i);
                ValueParameterDescriptor gParam = gParams.get(i);

                KotlinType fParamType = fParam.getType();
                KotlinType gParamType = gParam.getType();

                if (!typeMoreSpecific(fParamType, gParamType)) {
                    return false;
                }
            }
        }

        if (fIsVararg && gIsVararg) {
            // Check matching parameters
            int minSize = Math.min(fSize, gSize);
            for (int i = 0; i < minSize - 1; i++) {
                ValueParameterDescriptor fParam = fParams.get(i);
                ValueParameterDescriptor gParam = gParams.get(i);

                KotlinType fParamType = fParam.getType();
                KotlinType gParamType = gParam.getType();

                if (!typeMoreSpecific(fParamType, gParamType)) {
                    return false;
                }
            }

            // Check the non-matching parameters of one function against the vararg parameter of the other function
            // Example:
            //   f(vararg vf : T)
            //   g(a : A, vararg vg : T)
            // here we check that typeOf(a) < elementTypeOf(vf) and elementTypeOf(vg) < elementTypeOf(vf)
            if (fSize < gSize) {
                ValueParameterDescriptor fParam = fParams.get(fSize - 1);
                KotlinType fParamType = fParam.getVarargElementType();
                assert fParamType != null : "fIsVararg guarantees this";
                for (int i = fSize - 1; i < gSize; i++) {
                    ValueParameterDescriptor gParam = gParams.get(i);
                    if (!typeMoreSpecific(fParamType, getVarargElementTypeOrType(gParam))) {
                        return false;
                    }
                }
            }
            else {
                ValueParameterDescriptor gParam = gParams.get(gSize - 1);
                KotlinType gParamType = gParam.getVarargElementType();
                assert gParamType != null : "gIsVararg guarantees this";
                for (int i = gSize - 1; i < fSize; i++) {
                    ValueParameterDescriptor fParam = fParams.get(i);
                    if (!typeMoreSpecific(getVarargElementTypeOrType(fParam), gParamType)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @NotNull
    private static KotlinType getVarargElementTypeOrType(@NotNull ValueParameterDescriptor parameterDescriptor) {
        KotlinType varargElementType = parameterDescriptor.getVarargElementType();
        if (varargElementType != null) {
            return varargElementType;
        }
        return parameterDescriptor.getType();
    }

    private static boolean isVariableArity(List<ValueParameterDescriptor> fParams) {
        int fSize = fParams.size();
        return fSize > 0 && fParams.get(fSize - 1).getVarargElementType() != null;
    }

    private static boolean isGeneric(CallableDescriptor f) {
        return !f.getOriginal().getTypeParameters().isEmpty();
    }

    private boolean typeMoreSpecific(@NotNull KotlinType specific, @NotNull KotlinType general) {
        boolean isSubtype = KotlinTypeChecker.DEFAULT.isSubtypeOf(specific, general) ||
                            numericTypeMoreSpecific(specific, general);

        if (!isSubtype) return false;

        Specificity.Relation sThanG = TypeCapabilitiesKt.getSpecificityRelationTo(specific, general);
        Specificity.Relation gThanS = TypeCapabilitiesKt.getSpecificityRelationTo(general, specific);
        if (sThanG == Specificity.Relation.LESS_SPECIFIC && gThanS != Specificity.Relation.LESS_SPECIFIC) {
            return false;
        }

        return true;
    }

    private boolean numericTypeMoreSpecific(@NotNull KotlinType specific, @NotNull KotlinType general) {
        KotlinType _double = builtIns.getDoubleType();
        KotlinType _float = builtIns.getFloatType();
        KotlinType _long = builtIns.getLongType();
        KotlinType _int = builtIns.getIntType();
        KotlinType _byte = builtIns.getByteType();
        KotlinType _short = builtIns.getShortType();

        if (TypeUtils.equalTypes(specific, _double) && TypeUtils.equalTypes(general, _float)) return true;
        if (TypeUtils.equalTypes(specific, _int)) {
            if (TypeUtils.equalTypes(general, _long)) return true;
            if (TypeUtils.equalTypes(general, _byte)) return true;
            if (TypeUtils.equalTypes(general, _short)) return true;
        }
        if (TypeUtils.equalTypes(specific, _short) && TypeUtils.equalTypes(general, _byte)) return true;
        return false;
    }

}
