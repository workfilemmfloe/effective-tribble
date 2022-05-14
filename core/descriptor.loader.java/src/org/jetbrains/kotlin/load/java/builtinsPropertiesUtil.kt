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

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addToStdlib.check

private val BUILTIN_SPECIAL_PROPERTIES_FQ_NAMES = setOf(FqName("kotlin.Collection.size"), FqName("kotlin.Map.size"))
private val BUILTIN_SPECIAL_PROPERTIES_SHORT_NAMES = BUILTIN_SPECIAL_PROPERTIES_FQ_NAMES.map { it.shortName() }.toSet()


private val BUILTIN_METHODS_ERASED_COLLECTION_PARAMETER_FQ_NAMES = setOf(FqName("kotlin.Collection.containsAll"))
private val BUILTIN_METHODS_GENERIC_PARAMETERS_FQ_NAMES = setOf(
        FqName("kotlin.Collection.contains"), FqName("kotlin.MutableCollection.remove")
)

private val BUILTIN_METHODS_ERASED_VALUE_PARAMETERS_FQ_NAMES =
        BUILTIN_METHODS_GENERIC_PARAMETERS_FQ_NAMES + BUILTIN_METHODS_ERASED_COLLECTION_PARAMETER_FQ_NAMES

private val BUILTIN_METHODS_ERASED_VALUE_PARAMETERS_SHORT_NAMES =
        BUILTIN_METHODS_ERASED_VALUE_PARAMETERS_FQ_NAMES.map { it.shortName() }.toSet()

private object BuiltinSpecialMethods {
    val REMOVE_AT_FQ_NAME = FqName("kotlin.MutableList.removeAt")

    val FQ_NAMES_TO_JVM_MAP: Map<FqName, Name> = mapOf(
            REMOVE_AT_FQ_NAME to Name.identifier("remove"),
            FqName("kotlin.CharSequence.get") to Name.identifier("charAt")
    )

    val ORIGINAL_SHORT_NAMES: List<Name> = FQ_NAMES_TO_JVM_MAP.keySet().map { it.shortName() }

    private val JVM_SHORT_NAME_TO_BUILTIN_FQ_NAMES_MAP: Map<Name, List<FqName>> =
            FQ_NAMES_TO_JVM_MAP.entrySet().groupBy { it.value }.mapValues { entry -> entry.value.map { it.key } }

    val JVM_SHORT_NAME_TO_BUILTIN_SHORT_NAMES_MAP: Map<Name, List<Name>> =
            JVM_SHORT_NAME_TO_BUILTIN_FQ_NAMES_MAP.mapValues { it.value.map { it.shortName() } }
}

public fun CallableMemberDescriptor.hasBuiltinSpecialPropertyFqName(): Boolean {
    if (name !in BUILTIN_SPECIAL_PROPERTIES_SHORT_NAMES) return false

    return hasBuiltinSpecialPropertyFqNameImpl()
}

private fun CallableMemberDescriptor.hasBuiltinSpecialPropertyFqNameImpl(): Boolean {
    if (fqNameOrNull() in BUILTIN_SPECIAL_PROPERTIES_FQ_NAMES) return true

    if (!isFromBuiltins()) return false

    return overriddenDescriptors.any(CallableMemberDescriptor::hasBuiltinSpecialPropertyFqName)
}

public fun CallableMemberDescriptor.isFromBuiltins(): Boolean {
    if (!(propertyIfAccessor.fqNameOrNull()?.firstSegmentIs(KotlinBuiltIns.BUILT_INS_PACKAGE_NAME) ?: false)) return false
    return builtIns.builtInsModule == module
}

private fun CallableDescriptor.fqNameOrNull(): FqName? = fqNameUnsafe.check { it.isSafe }?.toSafe()

val Name.isBuiltinSpecialPropertyName: Boolean get() = this in BUILTIN_SPECIAL_PROPERTIES_SHORT_NAMES

public fun CallableMemberDescriptor.getBuiltinSpecialPropertyAccessorName(): String? {
    return propertyIfAccessor.check { it.hasBuiltinSpecialPropertyFqName() }?.name?.asString()
}

@Suppress("UNCHECKED_CAST")
fun <T : CallableMemberDescriptor> T.getBuiltinSpecialOverridden(): T? {
    return when (this) {
        is PropertyDescriptor, is PropertyAccessorDescriptor ->
            firstOverridden { it.propertyIfAccessor.hasBuiltinSpecialPropertyFqName() } as T?
        else -> firstOverridden { it.isBuiltinFunctionWithDifferentNameInJvm() } as T?
    }
}

private fun CallableMemberDescriptor.isBuiltinFunctionWithDifferentNameInJvm(): Boolean {
    if (!isFromBuiltins()) return false
    val fqName = fqNameOrNull() ?: return false
    return firstOverridden { BuiltinSpecialMethods.FQ_NAMES_TO_JVM_MAP.containsKey(fqName) } != null
}

fun CallableMemberDescriptor.overridesBuiltinSpecialDeclaration(): Boolean = getBuiltinSpecialOverridden() != null

public fun CallableMemberDescriptor.getJvmMethodNameIfSpecial(): String? {
    val builtinOverridden = getBuiltinOverriddenThatAffectsJvmName()?.propertyIfAccessor ?: return null
    return when (builtinOverridden) {
        is PropertyDescriptor -> builtinOverridden.getBuiltinSpecialPropertyAccessorName()
        else -> builtinOverridden.specialJvmName()?.asString()
    }
}

private fun CallableMemberDescriptor.getBuiltinOverriddenThatAffectsJvmName(): CallableMemberDescriptor? {
    val overriddenBuiltin = getBuiltinSpecialOverridden() ?: return null

    if (isFromJavaOrBuiltins()) return overriddenBuiltin

    return null
}

fun CallableMemberDescriptor.isFromJavaOrBuiltins() = isFromJava || isFromBuiltins()

private fun CallableMemberDescriptor.specialJvmName(): Name? {
    return BuiltinSpecialMethods.FQ_NAMES_TO_JVM_MAP[fqNameOrNull() ?: return null]
}

public fun getSpecialBuiltinFunctionsByJvmName(name: Name): List<Name> =
        BuiltinSpecialMethods.JVM_SHORT_NAME_TO_BUILTIN_SHORT_NAMES_MAP[name] ?: emptyList()

public val CallableMemberDescriptor.isRemoveAtByIndex: Boolean
    get() = name.asString() == "removeAt" && fqNameOrNull() == BuiltinSpecialMethods.REMOVE_AT_FQ_NAME

private val CallableMemberDescriptor.isFromJava: Boolean
    get() = propertyIfAccessor is JavaCallableMemberDescriptor && propertyIfAccessor.containingDeclaration is JavaClassDescriptor

private val CallableMemberDescriptor.propertyIfAccessor: CallableMemberDescriptor
    get() = if (this is PropertyAccessorDescriptor) correspondingProperty else this

private val CallableMemberDescriptor.hasErasedValueParametersInJava: Boolean
    get() = fqNameOrNull() in BUILTIN_METHODS_ERASED_VALUE_PARAMETERS_FQ_NAMES

val Name.sameAsRenamedInJvmBuiltin: Boolean
    get() = this in BuiltinSpecialMethods.ORIGINAL_SHORT_NAMES

fun FunctionDescriptor.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(): FunctionDescriptor? {
    if (!name.sameAsBuiltinMethodWithErasedValueParameters) return null
    return firstOverridden { it.hasErasedValueParametersInJava } as FunctionDescriptor?
}

private fun CallableMemberDescriptor.firstOverridden(
        predicate: (CallableMemberDescriptor) -> Boolean
): CallableMemberDescriptor? {
    var result: CallableMemberDescriptor? = null
    return DFS.dfs(listOf(this),
        object : DFS.Neighbors<CallableMemberDescriptor> {
            override fun getNeighbors(current: CallableMemberDescriptor?): Iterable<CallableMemberDescriptor> {
                return current?.overriddenDescriptors ?: emptyList()
            }
        },
        object : DFS.AbstractNodeHandler<CallableMemberDescriptor, CallableMemberDescriptor?>() {
            override fun beforeChildren(current: CallableMemberDescriptor) = result == null
            override fun afterChildren(current: CallableMemberDescriptor) {
                if (result == null && predicate(current)) {
                    result = current
                }
            }
            override fun result(): CallableMemberDescriptor? = result
        }
    )
}

val Name.sameAsBuiltinMethodWithErasedValueParameters: Boolean
    get () = this in BUILTIN_METHODS_ERASED_VALUE_PARAMETERS_SHORT_NAMES

enum class SpecialSignatureInfo(val signature: String?) {
    ONE_COLLECTION_PARAMETER("(Ljava/util/Collection<+Ljava/lang/Object;>;)Z"),
    GENERIC_PARAMETER(null)
}

fun CallableMemberDescriptor.getSpecialSignatureInfo(): SpecialSignatureInfo? {
    val builtinFqName = firstOverridden { it is FunctionDescriptor && it.hasErasedValueParametersInJava }?.fqNameOrNull()
            ?: return null

    return when (builtinFqName) {
        in BUILTIN_METHODS_ERASED_COLLECTION_PARAMETER_FQ_NAMES -> SpecialSignatureInfo.ONE_COLLECTION_PARAMETER
        in BUILTIN_METHODS_GENERIC_PARAMETERS_FQ_NAMES -> SpecialSignatureInfo.GENERIC_PARAMETER
        else -> error("Unexpected kind of special builtin: $builtinFqName")
    }
}

fun CallableMemberDescriptor.isBuiltinWithSpecialDescriptorInJvm(): Boolean {
    if (!isFromBuiltins()) return false
    return getSpecialSignatureInfo() == SpecialSignatureInfo.GENERIC_PARAMETER || overridesBuiltinSpecialDeclaration()
}
