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

package org.jetbrains.kotlin.asJava

import com.intellij.psi.*
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.psi.*
import java.util.Collections
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import java.util.ArrayList
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.utils.addToStdlib.singletonList
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList

public fun JetClassOrObject.toLightClass(): KotlinLightClass? = LightClassUtil.getPsiClass(this) as KotlinLightClass?

public fun JetDeclaration.toLightElements(): List<PsiNamedElement> =
        when (this) {
            is JetClassOrObject -> LightClassUtil.getPsiClass(this).singletonOrEmptyList()
            is JetNamedFunction,
            is JetSecondaryConstructor -> LightClassUtil.getLightClassMethod(this as JetFunction).singletonOrEmptyList()
            is JetProperty -> LightClassUtil.getLightClassPropertyMethods(this).toList()
            is JetPropertyAccessor -> LightClassUtil.getLightClassAccessorMethod(this).singletonOrEmptyList()
            is JetParameter -> ArrayList<PsiNamedElement>().let { elements ->
                toPsiParameter()?.let { psiParameter -> elements.add(psiParameter) }
                LightClassUtil.getLightClassPropertyMethods(this).toCollection(elements)

                elements
            }
            is JetTypeParameter -> toPsiTypeParameters()
            else -> listOf()
        }

public fun PsiElement.toLightMethods(): List<PsiMethod> =
        when (this) {
            is JetFunction -> LightClassUtil.getLightClassMethod(this).singletonOrEmptyList()
            is JetProperty -> LightClassUtil.getLightClassPropertyMethods(this).toList()
            is JetParameter -> LightClassUtil.getLightClassPropertyMethods(this).toList()
            is JetPropertyAccessor -> LightClassUtil.getLightClassAccessorMethod(this).singletonOrEmptyList()
            is JetClass -> LightClassUtil.getPsiClass(this)?.getConstructors()?.first().singletonOrEmptyList()
            is PsiMethod -> this.singletonList()
            else -> listOf()
        }

public fun PsiElement.getRepresentativeLightMethod(): PsiMethod? =
        when (this) {
            is JetFunction -> LightClassUtil.getLightClassMethod(this)
            is JetProperty -> LightClassUtil.getLightClassPropertyMethods(this).getGetter()
            is JetParameter -> LightClassUtil.getLightClassPropertyMethods(this).getGetter()
            is JetPropertyAccessor -> LightClassUtil.getLightClassAccessorMethod(this)
            is PsiMethod -> this
            else -> null
        }

public fun JetParameter.toPsiParameter(): PsiParameter? {
    val paramList = getNonStrictParentOfType<JetParameterList>() ?: return null

    val paramIndex = paramList.getParameters().indexOf(this)
    val owner = paramList.getParent()
    val lightParamIndex = if (owner is JetDeclaration && owner.isExtensionDeclaration()) paramIndex + 1 else paramIndex

    val method: PsiMethod =
            when (owner) {
                is JetFunction -> LightClassUtil.getLightClassMethod(owner)
                is JetPropertyAccessor -> LightClassUtil.getLightClassAccessorMethod(owner)
                else -> null
            } ?: return null

    return method.getParameterList().getParameters()[lightParamIndex]
}

public fun JetTypeParameter.toPsiTypeParameters(): List<PsiTypeParameter> {
    val paramList = getNonStrictParentOfType<JetTypeParameterList>()
    if (paramList == null) return listOf()

    val paramIndex = paramList.getParameters().indexOf(this)
    val jetDeclaration = paramList.getNonStrictParentOfType<JetDeclaration>() ?: return listOf()
    val lightOwners = jetDeclaration.toLightElements()

    return lightOwners.map { lightOwner -> (lightOwner as PsiTypeParameterListOwner).getTypeParameters()[paramIndex] }
}

// Returns original declaration if given PsiElement is a Kotlin light element, and element itself otherwise
public val PsiElement.unwrapped: PsiElement?
    get() = if (this is KotlinLightElement<*, *>) getOrigin() else this

public val PsiElement.namedUnwrappedElement: PsiNamedElement?
    get() = unwrapped?.getNonStrictParentOfType<PsiNamedElement>()
