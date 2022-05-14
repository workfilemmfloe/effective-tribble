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

package org.jetbrains.kotlin.resolve.varianceChecker

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedure.EnrichedProjectionKind.*
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedure.*
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.typeBinding.TypeBinding
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.resolve.typeBinding.createTypeBinding
import org.jetbrains.kotlin.resolve.typeBinding.createTypeBindingForReturnType
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.TopDownAnalysisContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.types.Variance.*
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyAccessorDescriptorImpl
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.KtTypeParameterListOwner
import org.jetbrains.kotlin.resolve.BindingContext


class VarianceChecker(private val trace: BindingTrace) {

    fun check(c: TopDownAnalysisContext) {
        checkClasses(c)
        checkMembers(c)
    }

    private fun checkClasses(c: TopDownAnalysisContext) {
        for (jetClassOrObject in c.declaredClasses!!.keys) {
            if (jetClassOrObject is KtClass) {
                for (specifier in jetClassOrObject.getSuperTypeListEntries()) {
                    specifier.getTypeReference()?.checkTypePosition(trace.bindingContext, OUT_VARIANCE, trace)
                }
                jetClassOrObject.checkTypeParameters(trace.getBindingContext(), OUT_VARIANCE, trace)
            }
        }
    }

    private fun checkMembers(c: TopDownAnalysisContext) {
        for ((declaration, descriptor) in c.members) {
            if (!Visibilities.isPrivate(descriptor.visibility)) {
                checkCallableDeclaration(trace.bindingContext, declaration, descriptor, trace)
            }
        }
    }

    class VarianceConflictDiagnosticData(
            val containingType: KotlinType,
            val typeParameter: TypeParameterDescriptor,
            val occurrencePosition: Variance
    )

    companion object {
        @JvmStatic
        fun recordPrivateToThisIfNeeded(trace: BindingTrace, descriptor: CallableMemberDescriptor) {
            if (isIrrelevant(descriptor) || descriptor.visibility != Visibilities.PRIVATE) return

            val psiElement = descriptor.source.getPsi()
            if (psiElement !is KtCallableDeclaration) return

            if (!checkCallableDeclaration(trace.bindingContext, psiElement, descriptor, DiagnosticSink.DO_NOTHING)) {
                recordPrivateToThis(descriptor)
            }
        }

        private fun isIrrelevant(descriptor: CallableDescriptor): Boolean {
            val containingClass = descriptor.containingDeclaration
            if (containingClass !is ClassDescriptor) return true

            return containingClass.typeConstructor.parameters.all { it.variance == INVARIANT }
        }

        private fun recordPrivateToThis(descriptor: CallableMemberDescriptor) {
            if (descriptor is FunctionDescriptorImpl) {
                descriptor.visibility = Visibilities.PRIVATE_TO_THIS;
            }
            else if (descriptor is PropertyDescriptorImpl) {
                descriptor.visibility = Visibilities.PRIVATE_TO_THIS;
                for (accessor in descriptor.accessors) {
                    (accessor as PropertyAccessorDescriptorImpl).visibility = Visibilities.PRIVATE_TO_THIS
                }
            }
            else {
                throw IllegalStateException("Unexpected descriptor type: ${descriptor.javaClass.name}")
            }
        }

        private fun checkCallableDeclaration(
                trace: BindingContext,
                declaration: KtCallableDeclaration,
                descriptor: CallableDescriptor,
                diagnosticSink: DiagnosticSink
        ): Boolean {
            if (isIrrelevant(descriptor)) return true
            var noError = true

            noError = noError and declaration.checkTypeParameters(trace, IN_VARIANCE, diagnosticSink)

            noError = noError and declaration.receiverTypeReference?.checkTypePosition(trace, IN_VARIANCE, diagnosticSink)

            for (parameter in declaration.valueParameters) {
                noError = noError and parameter.getTypeReference()?.checkTypePosition(trace, IN_VARIANCE, diagnosticSink)
            }

            val returnTypePosition = if (descriptor is VariableDescriptor && descriptor.isVar) INVARIANT else OUT_VARIANCE
            noError = noError and declaration.createTypeBindingForReturnType(trace)?.checkTypePosition(returnTypePosition, diagnosticSink)

            return noError
        }

        private fun KtTypeParameterListOwner.checkTypeParameters(
                trace: BindingContext,
                typePosition: Variance,
                diagnosticSink: DiagnosticSink
        ): Boolean {
            var noError = true
            for (typeParameter in typeParameters) {
                noError = noError and typeParameter.getExtendsBound()?.checkTypePosition(trace, typePosition, diagnosticSink)
            }
            for (typeConstraint in typeConstraints) {
                noError = noError and typeConstraint.getBoundTypeReference()?.checkTypePosition(trace, typePosition, diagnosticSink)
            }
            return noError
        }

        private fun KtTypeReference.checkTypePosition(trace: BindingContext, position: Variance, diagnosticSink: DiagnosticSink)
                = createTypeBinding(trace)?.checkTypePosition(position, diagnosticSink)

        private fun TypeBinding<PsiElement>.checkTypePosition(position: Variance, diagnosticSink: DiagnosticSink)
                = checkTypePosition(kotlinType, position, diagnosticSink)

        private fun TypeBinding<PsiElement>.checkTypePosition(containingType: KotlinType, position: Variance, diagnosticSink: DiagnosticSink): Boolean {
            val classifierDescriptor = kotlinType.constructor.declarationDescriptor
            if (classifierDescriptor is TypeParameterDescriptor) {
                val declarationVariance = classifierDescriptor.variance
                if (!declarationVariance.allowsPosition(position)
                        && !kotlinType.annotations.hasAnnotation(KotlinBuiltIns.FQ_NAMES.unsafeVariance)) {
                    diagnosticSink.report(
                            Errors.TYPE_VARIANCE_CONFLICT.on(
                                    psiElement,
                                    VarianceConflictDiagnosticData(containingType, classifierDescriptor, position)
                            )
                    )
                }
                return declarationVariance.allowsPosition(position)
            }

            var noError = true
            for (argumentBinding in getArgumentBindings()) {
                if (argumentBinding == null || argumentBinding.typeParameterDescriptor == null) continue

                val projectionKind = getEffectiveProjectionKind(argumentBinding.typeParameterDescriptor!!, argumentBinding.typeProjection)!!
                val newPosition = when (projectionKind) {
                    OUT -> position
                    IN -> position.opposite()
                    INV -> INVARIANT
                    STAR -> null // CONFLICTING_PROJECTION error was reported
                }
                if (newPosition != null) {
                    noError = noError and argumentBinding.typeBinding.checkTypePosition(containingType, newPosition, diagnosticSink)
                }
            }
            return noError
        }

        private infix fun Boolean.and(other: Boolean?) = if (other == null) this else this and other
    }
}
