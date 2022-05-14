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

package org.jetbrains.kotlin.types.expressions.unqualifiedSuper

import com.intellij.util.SmartList
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.psi.JetDotQualifiedExpression
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetSuperExpression
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.addToStdlib.singletonList


public fun resolveUnqualifiedSuperFromExpressionContext(
        superExpression: JetSuperExpression,
        supertypes: Collection<JetType>,
        anyType: JetType
): Collection<JetType> {
    val parentElement = superExpression.parent

    if (parentElement is JetDotQualifiedExpression) {
        val selectorExpression = parentElement.selectorExpression
        when (selectorExpression) {
            is JetCallExpression -> {
                // super.foo(...): foo can be a function or a property of a callable type
                val calleeExpression = selectorExpression.calleeExpression
                if (calleeExpression is JetSimpleNameExpression) {
                    val calleeName = calleeExpression.getReferencedNameAsName()
                    val location = NoLookupLocation.WHEN_TYPING
                    if (isCallingMethodOfAny(selectorExpression, calleeName)) {
                        return resolveSupertypesForMethodOfAny(supertypes, calleeName, location, anyType)
                    }
                    else {
                        return resolveSupertypesByCalleeName(supertypes, calleeName, location)
                    }
                }
            }
            is JetSimpleNameExpression -> {
                // super.x: x can be a property only
                // NB there are no properties in kotlin.Any
                return resolveSupertypesByPropertyName(supertypes, selectorExpression.getReferencedNameAsName(), NoLookupLocation.WHEN_TYPING)
            }
        }
    }

    return emptyList()
}

private val ARITY_OF_METHODS_OF_ANY = hashMapOf("hashCode" to 0, "equals" to 1, "toString" to 0)

private fun isCallingMethodOfAny(callExpression: JetCallExpression, calleeName: Name): Boolean =
        ARITY_OF_METHODS_OF_ANY.getOrElse(calleeName.asString(), { -1 }) == callExpression.valueArguments.size()

public fun isPossiblyAmbiguousUnqualifiedSuper(superExpression: JetSuperExpression, supertypes: Collection<JetType>): Boolean =
        supertypes.size() > 1 ||
        (supertypes.size() == 1 && supertypes.single().isInterface() && isCallingMethodOfAnyWithSuper(superExpression))

private fun isCallingMethodOfAnyWithSuper(superExpression: JetSuperExpression): Boolean {
    val parent = superExpression.parent
    if (parent is JetDotQualifiedExpression) {
        val selectorExpression = parent.selectorExpression
        if (selectorExpression is JetCallExpression) {
            val calleeExpression = selectorExpression.calleeExpression
            if (calleeExpression is JetSimpleNameExpression) {
                val calleeName = calleeExpression.getReferencedNameAsName()
                return isCallingMethodOfAny(selectorExpression, calleeName)
            }
        }
    }

    return false
}

private fun JetType.isInterface(): Boolean =
        TypeUtils.getClassDescriptor(this)?.kind == ClassKind.INTERFACE

private fun resolveSupertypesForMethodOfAny(supertypes: Collection<JetType>, calleeName: Name, location: LookupLocation, anyType: JetType): Collection<JetType> {
    val typesWithConcreteOverride = resolveSupertypesByMembers(supertypes, false) { getFunctionMembers(it, calleeName, location) }
    return if (typesWithConcreteOverride.isNotEmpty())
        typesWithConcreteOverride
    else
        anyType.singletonList()
}

private fun resolveSupertypesByCalleeName(supertypes: Collection<JetType>, calleeName: Name, location: LookupLocation): Collection<JetType> =
        resolveSupertypesByMembers(supertypes, true) { getFunctionMembers(it, calleeName, location) + getPropertyMembers(it, calleeName, location) }

private fun resolveSupertypesByPropertyName(supertypes: Collection<JetType>, propertyName: Name, location: LookupLocation): Collection<JetType> =
        resolveSupertypesByMembers(supertypes, true) { getPropertyMembers(it, propertyName, location) }

private inline fun resolveSupertypesByMembers(
        supertypes: Collection<JetType>,
        allowArbitraryMembers: Boolean,
        getMembers: (JetType) -> Collection<MemberDescriptor>
): Collection<JetType> {
    val typesWithConcreteMembers = SmartList<JetType>()
    val typesWithArbitraryMembers = SmartList<JetType>()

    for (supertype in supertypes) {
        val members = getMembers(supertype)
        if (members.isNotEmpty()) {
            typesWithArbitraryMembers.add(supertype)
            if (members.any { isConcreteMember(supertype, it) }) {
                typesWithConcreteMembers.add(supertype)
            }
        }
    }

    return if (typesWithConcreteMembers.isNotEmpty()) typesWithConcreteMembers
        else if (allowArbitraryMembers) typesWithArbitraryMembers
        else emptyList<JetType>()
}

private fun getFunctionMembers(type: JetType, name: Name, location: LookupLocation): Collection<MemberDescriptor> =
        type.memberScope.getFunctions(name, location)

private fun getPropertyMembers(type: JetType, name: Name, location: LookupLocation): Collection<MemberDescriptor> =
        type.memberScope.getProperties(name, location).filterIsInstanceTo(SmartList<MemberDescriptor>())

private fun isConcreteMember(supertype: JetType, memberDescriptor: MemberDescriptor): Boolean {
    // "Concrete member" is a function or a property that is not abstract,
    // and is not an implicit fake override for a method of Any on an interface.

    if (memberDescriptor.modality == Modality.ABSTRACT)
        return false

    val classDescriptorForSupertype = TypeUtils.getClassDescriptor(supertype)
    val memberKind = (memberDescriptor as CallableMemberDescriptor).kind
    if (classDescriptorForSupertype?.kind == ClassKind.INTERFACE && memberKind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        // We have a fake override on interface. It should have a dispatch receiver, which should not be Any.
        val dispatchReceiverType = memberDescriptor.dispatchReceiverParameter?.type ?: return false
        val dispatchReceiverClass = TypeUtils.getClassDescriptor(dispatchReceiverType) ?: return false
        return !KotlinBuiltIns.isAny(dispatchReceiverClass)
    }

    return true
}
