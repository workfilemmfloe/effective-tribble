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

package org.jetbrains.kotlin.idea.search.usagesSearch

import com.intellij.psi.PsiConstructorCall
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.codegen.PropertyCodegen
import org.jetbrains.kotlin.asJava.KotlinLightMethod
import org.jetbrains.kotlin.resolve.OverrideResolver
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.idea.references.*
import org.jetbrains.kotlin.idea.findUsages.UsageTypeUtils
import org.jetbrains.kotlin.idea.findUsages.UsageTypeEnum
import org.jetbrains.kotlin.idea.caches.resolve.analyze

val JetDeclaration.descriptor: DeclarationDescriptor?
    get() = this.analyze().get(BindingContext.DECLARATION_TO_DESCRIPTOR, this)

val JetParameter.propertyDescriptor: PropertyDescriptor?
    get() = this.analyze().get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, this)

fun PsiReference.checkUsageVsOriginalDescriptor(
        target: JetDeclaration,
        declarationToDescriptor: (JetDeclaration) -> DeclarationDescriptor? = {it.descriptor},
        checker: (usageDescriptor: DeclarationDescriptor, targetDescriptor: DeclarationDescriptor) -> Boolean
): Boolean {
    return unwrappedTargets.any {
        if (it is JetDeclaration) {
            val usageDescriptor = declarationToDescriptor(it)
            val targetDescriptor = declarationToDescriptor(target)
            usageDescriptor != null && targetDescriptor != null && checker(usageDescriptor, targetDescriptor)
        }
        else false
    }
}

fun PsiReference.isImportUsage(): Boolean =
        getElement()!!.getNonStrictParentOfType<JetImportDirective>() != null

fun PsiReference.isConstructorUsage(jetClassOrObject: JetClassOrObject): Boolean = with (getElement()!!) {
    fun getCallDescriptor(bindingContext: BindingContext): DeclarationDescriptor? {
        val constructorCalleeExpression = getNonStrictParentOfType<JetConstructorCalleeExpression>()
        if (constructorCalleeExpression != null) {
            return bindingContext.get(BindingContext.REFERENCE_TARGET, constructorCalleeExpression.getConstructorReferenceExpression())
        }

        val callExpression = getNonStrictParentOfType<JetCallExpression>()
        if (callExpression != null) {
            val callee = callExpression.getCalleeExpression()
            if (callee is JetReferenceExpression) {
                return bindingContext.get(BindingContext.REFERENCE_TARGET, callee)
            }
        }

        return null
    }

    fun checkJavaUsage(): Boolean {
        val call = getNonStrictParentOfType<PsiConstructorCall>()
        return call == getParent() && call?.resolveConstructor()?.getContainingClass()?.getNavigationElement() == jetClassOrObject
    }

    fun checkKotlinUsage(): Boolean {
        if (this !is JetElement) return false

        val bindingContext = this.analyze()

        val descriptor = getCallDescriptor(bindingContext)
        if (descriptor !is ConstructorDescriptor) return false

        return DescriptorToSourceUtils.descriptorToDeclaration(descriptor.getContainingDeclaration()) == jetClassOrObject
    }

    checkJavaUsage() || checkKotlinUsage()
}

// Check if reference resolves to extension function whose receiver is the same as declaration's parent (or its superclass)
// Used in extension search
fun PsiReference.isExtensionOfDeclarationClassUsage(declaration: JetNamedDeclaration): Boolean =
        checkUsageVsOriginalDescriptor(declaration) { (usageDescriptor, targetDescriptor) ->
            when {
                usageDescriptor == targetDescriptor -> false
                usageDescriptor !is FunctionDescriptor -> false
                else -> {
                    val receiverDescriptor =
                            usageDescriptor.getExtensionReceiverParameter()?.getType()?.getConstructor()?.getDeclarationDescriptor()
                    val containingDescriptor = targetDescriptor.getContainingDeclaration()

                    containingDescriptor == receiverDescriptor
                    || (containingDescriptor is ClassDescriptor
                        && receiverDescriptor is ClassDescriptor
                        && DescriptorUtils.isSubclass(containingDescriptor, receiverDescriptor))
                }
            }
        }

// Check if reference resolves to the declaration with the same parent
// Used in overload search
fun PsiReference.isUsageInContainingDeclaration(declaration: JetNamedDeclaration): Boolean =
        checkUsageVsOriginalDescriptor(declaration) { (usageDescriptor, targetDescriptor) ->
            usageDescriptor != targetDescriptor
            && usageDescriptor.getContainingDeclaration() == targetDescriptor.getContainingDeclaration()
        }

fun PsiReference.isCallableOverrideUsage(declaration: JetNamedDeclaration): Boolean {
    val decl2Desc = {(declaration: JetDeclaration) ->
        if (declaration is JetParameter && declaration.hasValOrVarNode()) declaration.propertyDescriptor else declaration.descriptor
    }

    return checkUsageVsOriginalDescriptor(declaration, decl2Desc) { (usageDescriptor, targetDescriptor) ->
        usageDescriptor is CallableDescriptor && targetDescriptor is CallableDescriptor
        && OverrideResolver.overrides(usageDescriptor, targetDescriptor)
    }
}


// Check if reference resolves to property getter
// Works for JetProperty and JetParameter
fun PsiReference.isPropertyReadOnlyUsage(): Boolean {
    if (UsageTypeUtils.getUsageType(getElement()) == UsageTypeEnum.READ) return true

    val refTarget = resolve()
    if (refTarget is KotlinLightMethod) {
        val origin = refTarget.origin
        val declaration: JetNamedDeclaration? = when (origin) {
            is JetPropertyAccessor -> origin.getNonStrictParentOfType<JetProperty>()
            is JetProperty, is JetParameter -> origin as JetNamedDeclaration
            else -> null
        }
        return declaration != null && refTarget.getName() == PropertyCodegen.getterName(declaration.getNameAsName())
    }

    return false
}
