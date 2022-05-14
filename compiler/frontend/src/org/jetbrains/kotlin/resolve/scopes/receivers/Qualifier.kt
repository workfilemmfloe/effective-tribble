/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.scopes.receivers

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getTopmostParentQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.descriptorUtil.classValueType
import org.jetbrains.kotlin.resolve.scopes.ChainedMemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

interface Qualifier : QualifierReceiver {
    val referenceExpression: KtSimpleNameExpression
}

val Qualifier.expression: KtExpression
    get() = referenceExpression.getTopmostParentQualifiedExpressionForSelector() ?: referenceExpression

class PackageQualifier(
        override val referenceExpression: KtSimpleNameExpression,
        override val descriptor: PackageViewDescriptor
) : Qualifier {
    override val classValueReceiver: ReceiverValue? get() = null
    override val staticScope: MemberScope get() = descriptor.memberScope

    override fun toString() = "Package{$descriptor}"
}

class TypeParameterQualifier(
        override val referenceExpression: KtSimpleNameExpression,
        override val descriptor: TypeParameterDescriptor
) : Qualifier {
    override val classValueReceiver: ReceiverValue? get() = null
    override val staticScope: MemberScope get() = MemberScope.Empty

    override fun toString() = "TypeParameter{$descriptor}"
}

class ClassQualifier(
        override val referenceExpression: KtSimpleNameExpression,
        override val descriptor: ClassDescriptor
) : Qualifier {
    override val classValueReceiver: ClassValueReceiver? = descriptor.classValueType?.let {
        ClassValueReceiver(this, it)
    }

    override val staticScope: MemberScope get() {
        val scopes = ArrayList<MemberScope>(2)

        scopes.add(descriptor.staticScope)

        if (descriptor.kind != ClassKind.ENUM_ENTRY) {
            scopes.add(descriptor.unsubstitutedInnerClassesScope)
        }

        return ChainedMemberScope("Static scope for ${descriptor.name} as class or object", scopes)
    }

    override fun toString() = "Class{$descriptor}"
}

class ClassValueReceiver(val classQualifier: ClassQualifier, private val type: KotlinType) : ExpressionReceiver {
    override fun getType() = type

    override val expression: KtExpression
        get() = classQualifier.expression
}
