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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.refactoring.changeSignature.MethodDescriptor
import com.intellij.refactoring.changeSignature.OverriderUsageInfo
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.KotlinLightMethod
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToDeclarationUtil
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.JetFunctionDefinitionUsage

import java.util.*
import kotlin.properties.Delegates

public class JetChangeSignatureData(
        override val baseDescriptor: FunctionDescriptor,
        override val baseDeclaration: PsiElement,
        private val descriptorsForSignatureChange: Collection<FunctionDescriptor>
) : JetMethodDescriptor {
    private val parameters: MutableList<JetParameterInfo>

            ;{
        val valueParameters = when {
            baseDeclaration is JetFunction -> baseDeclaration.getValueParameters()
            baseDeclaration is JetClass -> baseDeclaration.getPrimaryConstructorParameters()
            else -> null
        }
        parameters = baseDescriptor.getValueParameters().mapTo(ArrayList()) { parameterDescriptor ->
            val jetParameter = valueParameters?.get(parameterDescriptor.getIndex())
            JetParameterInfo(
                    originalIndex = parameterDescriptor.getIndex(),
                    name = parameterDescriptor.getName().asString(),
                    type = parameterDescriptor.getType(),
                    defaultValueForParameter = jetParameter?.getDefaultValue(),
                    valOrVarNode = jetParameter?.getValOrVarNode(),
                    modifierList = jetParameter?.getModifierList()
            )
        }
    }

    override val primaryFunctions: Collection<JetFunctionDefinitionUsage<PsiElement>> by Delegates.lazy {
        descriptorsForSignatureChange.map {
            val declaration = DescriptorToDeclarationUtil.getDeclaration(baseDeclaration.getProject(), it)
            assert(declaration != null) { "No declaration found for " + baseDescriptor }
            JetFunctionDefinitionUsage<PsiElement>(declaration, it, null, null)
        }
    }

    override val originalPrimaryFunction: JetFunctionDefinitionUsage<PsiElement> by Delegates.lazy {
        primaryFunctions.first { it.getDeclaration() == baseDeclaration }
    }

    override val affectedFunctions: Collection<UsageInfo> by Delegates.lazy {
        primaryFunctions + primaryFunctions.flatMapTo(HashSet<UsageInfo>()) { primaryFunction ->
            val primaryDeclaration = primaryFunction.getDeclaration() as? JetNamedFunction
            val lightMethod = primaryDeclaration?.let { LightClassUtil.getLightClassMethod(it) }
            val overrides = lightMethod?.let { OverridingMethodsSearch.search(it).findAll() } ?: Collections.emptyList()
            overrides.map { method ->
                if (method is KotlinLightMethod) {
                    val overridingDeclaration = method.origin
                    val overridingDescriptor = overridingDeclaration?.resolveToDescriptor() as FunctionDescriptor
                    JetFunctionDefinitionUsage<PsiElement>(overridingDeclaration, overridingDescriptor, primaryFunction, null)
                }
                else OverriderUsageInfo(method, lightMethod, true, true, true)
            }.filterNotNullTo(HashSet<UsageInfo>())
        }
    }

    override fun getParameters(): List<JetParameterInfo> {
        return parameters
    }

    public fun addParameter(parameter: JetParameterInfo) {
        parameters.add(parameter)
    }

    public fun removeParameter(index: Int) {
        parameters.remove(index)
    }

    public fun clearParameters() {
        parameters.clear()
    }

    override fun getName(): String {
        if (baseDescriptor is ConstructorDescriptor) {
            return baseDescriptor.getContainingDeclaration().getName().asString()
        }
        else if (baseDescriptor is AnonymousFunctionDescriptor) {
            return ""
        }
        else {
            return baseDescriptor.getName().asString()
        }
    }

    override fun getParametersCount(): Int {
        return baseDescriptor.getValueParameters().size()
    }

    override fun getVisibility(): Visibility {
        return baseDescriptor.getVisibility()
    }

    override fun getMethod(): PsiElement {
        return baseDeclaration
    }

    override fun canChangeVisibility(): Boolean {
        val parent = baseDescriptor.getContainingDeclaration()
        return !(baseDescriptor is AnonymousFunctionDescriptor || parent is ClassDescriptor && parent.getKind() == ClassKind.TRAIT)
    }

    override fun canChangeParameters(): Boolean {
        return true
    }

    override fun canChangeName(): Boolean {
        return !(baseDescriptor is ConstructorDescriptor || baseDescriptor is AnonymousFunctionDescriptor)
    }

    override fun canChangeReturnType(): MethodDescriptor.ReadWriteOption {
        return if (baseDescriptor is ConstructorDescriptor) MethodDescriptor.ReadWriteOption.None else MethodDescriptor.ReadWriteOption.ReadWrite
    }
}
