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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.psi.JetConstructorCalleeExpression
import org.jetbrains.kotlin.psi.JetDelegatorToSuperClass
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetUserType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.Variance
import java.util.Collections

public object CreateClassFromTypeReferenceActionFactory : CreateClassFromUsageFactory<JetUserType>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): JetUserType? {
        return QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetUserType>())
    }

    override fun getPossibleClassKinds(element: JetUserType, diagnostic: Diagnostic): List<ClassKind> {
        val typeRefParent = element.parent?.parent
        if (typeRefParent is JetConstructorCalleeExpression) return Collections.emptyList()

        val interfaceExpected = typeRefParent is JetDelegatorToSuperClass

        val isQualifier = (element.parent as? JetUserType)?.let { it.qualifier == element } ?: false

        return when {
            interfaceExpected -> Collections.singletonList(ClassKind.INTERFACE)
            else -> ClassKind.values().filter {
                val noTypeArguments = element.typeArgumentsAsTypes.isEmpty()
               ^when (it) {
                    ClassKind.OBJECT -> noTypeArguments && isQualifier
                    ClassKind.ANNOTATION_CLASS -> noTypeArguments && !isQualifier
                    ClassKind.ENUM_ENTRY -> false
                    ClassKind.ENUM_CLASS -> noTypeArguments
                    else -> true
                }
            }
        }
    }

    override fun createQuickFixData(element: JetUserType, diagnostic: Diagnostic): ClassInfo? {
        val name = element.referenceExpression?.getReferencedName() ?: return null
        if (element.parent?.parent is JetConstructorCalleeExpression) return null

        val file = element.containingFile as? JetFile ?: return null

        val context = element.analyze()
        val qualifier = element.qualifier?.referenceExpression
        val qualifierDescriptor = qualifier?.let { context[BindingContext.REFERENCE_TARGET, it] }

        val targetParent = getTargetParentByQualifier(file, qualifier != null, qualifierDescriptor) ?: return null

        val anyType = KotlinBuiltIns.getInstance().anyType

        return ClassInfo(
                name = name,
                targetParent = targetParent,
                expectedTypeInfo = TypeInfo.Empty,
                typeArguments = element.typeArgumentsAsTypes.map {
                    if (it != null) TypeInfo(it, Variance.INVARIANT) else TypeInfo(anyType, Variance.INVARIANT)
                }
        )
    }
}
