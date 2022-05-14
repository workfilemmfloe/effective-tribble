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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.ParameterInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.SecondaryConstructorInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.idea.core.refactoring.canRefactor
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.psi.JetConstructorDelegationCall
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.Variance

object CreateConstructorFromDelegationCallActionFactory : JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val delegationCall = diagnostic.getPsiElement().getStrictParentOfType<JetConstructorDelegationCall>() ?: return null
        val calleeExpression = delegationCall.getCalleeExpression() ?: return null
        val currentClass = delegationCall.getStrictParentOfType<JetClass>() ?: return null

        val project = currentClass.getProject()

        val classDescriptor = currentClass.resolveToDescriptor() as? ClassDescriptor ?: return null

        val targetClass = if (calleeExpression.isThis()) {
            currentClass
        }
        else {
            val superClassDescriptor = DescriptorUtils.getSuperclassDescriptors(classDescriptor)
                    .singleOrNull { it.getKind() == ClassKind.CLASS } ?: return null
            DescriptorToSourceUtilsIde.getAnyDeclaration(project, superClassDescriptor) ?: return null
        }
        if (!(targetClass.canRefactor() && (targetClass is JetClass || targetClass is PsiClass))) return null

        val anyType = KotlinBuiltIns.getInstance().getNullableAnyType()
        val parameters = delegationCall.getValueArguments().map {
            ParameterInfo(
                    it.getArgumentExpression()?.let { TypeInfo(it, Variance.IN_VARIANCE) } ?: TypeInfo(anyType, Variance.IN_VARIANCE),
                    it.getArgumentName()?.getReferenceExpression()?.getReferencedName()
            )
        }

        return CreateCallableFromUsageFix(delegationCall, SecondaryConstructorInfo(parameters, targetClass), false)
    }
}
