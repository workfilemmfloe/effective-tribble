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

@file:JvmName("SpecialBuiltinMembers")
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
import org.jetbrains.kotlin.load.java.BuiltinSpecialProperties.getBuiltinSpecialPropertyGetterName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

object BuiltinSpecialProperties {
    private val PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP = mapOf(
            FqName("kotlin.Enum.name")                 to Name.identifier("name"),
            FqName("kotlin.Enum.ordinal")              to Name.identifier("ordinal"),
            FqName("kotlin.Collection.size")           to Name.identifier("size"),
            FqName("kotlin.Map.size")                  to Name.identifier("size"),
            FqName("kotlin.CharSequence.length")       to Name.identifier("length"),
            FqName("kotlin.Map.keys")                  to Name.identifier("keySet"),
            FqName("kotlin.Map.values")                to Name.identifier("values"),
            FqName("kotlin.Map.entries")               to Name.identifier("entrySet")
    )

    private val GETTER_JVM_NAME_TO_PROPERTIES_SHORT_NAME_MAP: Map<Name, List<Name>> =
            PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP.getInversedShortNamesMap()

    private val FQ_NAMES = PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP.keySet()
    private val SHORT_NAMES = FQ_NAMES.map { it.shortName() }.toSet()

    fun hasBuiltinSpecialPropertyFqName(callableMemberDescriptor: CallableMemberDescriptor): Boolean {
        if (callableMemberDescriptor.name !in SHORT_NAMES) return false

        return callableMemberDescriptor.hasBuiltinSpecialPropertyFqNameImpl()
    }

    fun CallableMemberDescriptor.hasBuiltinSpecialPropertyFqNameImpl(): Boolean {
        if (fqNameOrNull() in FQ_NAMES) return true
        if (!isFromBuiltins()) return false

        return overriddenDescriptors.any { hasBuiltinSpecialPropertyFqName(it) }
    }

    fun getPropertyNameCandidatesBySpecialGetterName(name1: Name): List<Name> =
            GETTER_JVM_NAME_TO_PROPERTIES_SHORT_NAME_MAP[name1] ?: emptyList()

    fun CallableMemberDescriptor.getBuiltinSpecialPropertyGetterName(): String? {
        assert(isFromBuiltins()) { "This method is defined only for builtin members, but $this found" }

        val descriptor = propertyIfAccessor.firstOverridden { hasBuiltinSpecialPropertyFqName(it) } ?: return null
        return PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP[descriptor.fqNameSafe]?.asString()
    }
}

object BuiltinMethodsWithSpecialJvmSignature {
    private val ERASED_COLLECTION_PARAMETER_FQ_NAMES = setOf(
            FqName("kotlin.Collection.containsAll"),
            FqName("kotlin.MutableCollection.removeAll"),
            FqName("kotlin.MutableCollection.retainAll")
    )
    private val GENERIC_PARAMETERS_FQ_NAMES = setOf(
            FqName("kotlin.Collection.contains"),
            FqName("kotlin.MutableCollection.remove"),
            FqName("kotlin.Map.containsKey"),
            FqName("kotlin.Map.containsValue"),
            FqName("kotlin.Map.get"),
            FqName("kotlin.MutableMap.remove"),
            FqName("kotlin.List.indexOf"),
            FqName("kotlin.List.lastIndexOf")
    )

    private val ERASED_VALUE_PARAMETERS_FQ_NAMES =
            GENERIC_PARAMETERS_FQ_NAMES + ERASED_COLLECTION_PARAMETER_FQ_NAMES

    private val ERASED_VALUE_PARAMETERS_SHORT_NAMES =
            ERASED_VALUE_PARAMETERS_FQ_NAMES.map { it.shortName() }.toSet()

    private val CallableMemberDescriptor.hasErasedValueParametersInJava: Boolean
        get() = fqNameOrNull() in ERASED_VALUE_PARAMETERS_FQ_NAMES

    @JvmStatic
    fun getOverriddenBuiltinFunctionWithErasedValueParametersInJava(
            functionDescriptor: FunctionDescriptor
    ): FunctionDescriptor? {
        if (!functionDescriptor.name.sameAsBuiltinMethodWithErasedValueParameters) return null
        return functionDescriptor.firstOverridden { it.hasErasedValueParametersInJava } as FunctionDescriptor?
    }

    val Name.sameAsBuiltinMethodWithErasedValueParameters: Boolean
        get () = this in ERASED_VALUE_PARAMETERS_SHORT_NAMES

    enum class SpecialSignatureInfo(val signature: String?) {
        ONE_COLLECTION_PARAMETER("(Ljava/util/Collection<+Ljava/lang/Object;>;)Z"),
        GENERIC_PARAMETER(null)
    }

    fun CallableMemberDescriptor.isBuiltinWithSpecialDescriptorInJvm(): Boolean {
        if (!isFromBuiltins()) return false
        return getSpecialSignatureInfo() == SpecialSignatureInfo.GENERIC_PARAMETER || overridesBuiltinSpecialDeclaration()
    }

    @JvmStatic
    fun CallableMemberDescriptor.getSpecialSignatureInfo(): SpecialSignatureInfo? {
        val builtinFqName = firstOverridden { it is FunctionDescriptor && it.hasErasedValueParametersInJava }?.fqNameOrNull()
                ?: return null

        return when (builtinFqName) {
            in ERASED_COLLECTION_PARAMETER_FQ_NAMES -> SpecialSignatureInfo.ONE_COLLECTION_PARAMETER
            in GENERIC_PARAMETERS_FQ_NAMES -> SpecialSignatureInfo.GENERIC_PARAMETER
            else -> error("Unexpected kind of special builtin: $builtinFqName")
        }
    }
}

object BuiltinSpecialMethods {
    val REMOVE_AT_FQ_NAME = FqName("kotlin.MutableList.removeAt")

    val FQ_NAMES_TO_JVM_MAP: Map<FqName, Name> = mapOf(
            FqName("kotlin.Number.toByte")    to Name.identifier("byteValue"),
            FqName("kotlin.Number.toShort")   to Name.identifier("shortValue"),
            FqName("kotlin.Number.toInt")     to Name.identifier("intValue"),
            FqName("kotlin.Number.toLong")    to Name.identifier("longValue"),
            FqName("kotlin.Number.toFloat")   to Name.identifier("floatValue"),
            FqName("kotlin.Number.toDouble")  to Name.identifier("doubleValue"),
            REMOVE_AT_FQ_NAME                 to Name.identifier("remove"),
            FqName("kotlin.CharSequence.get") to Name.identifier("charAt")
    )

    val ORIGINAL_SHORT_NAMES: List<Name> = FQ_NAMES_TO_JVM_MAP.keySet().map { it.shortName() }

    val JVM_SHORT_NAME_TO_BUILTIN_SHORT_NAMES_MAP: Map<Name, List<Name>> = FQ_NAMES_TO_JVM_MAP.getInversedShortNamesMap()

    val Name.sameAsRenamedInJvmBuiltin: Boolean
        get() = this in ORIGINAL_SHORT_NAMES

    fun getSpecialJvmName(callableMemberDescriptor: CallableMemberDescriptor): Name? {
        return FQ_NAMES_TO_JVM_MAP[callableMemberDescriptor.fqNameOrNull() ?: return null]
    }

    fun isBuiltinFunctionWithDifferentNameInJvm(callableMemberDescriptor: CallableMemberDescriptor): Boolean {
        if (!callableMemberDescriptor.isFromBuiltins()) return false
        val fqName = callableMemberDescriptor.fqNameOrNull() ?: return false
        return callableMemberDescriptor.firstOverridden { FQ_NAMES_TO_JVM_MAP.containsKey(fqName) } != null
    }

    fun getSpecialBuiltinFunctionsByJvmName(name: Name): List<Name> =
            JVM_SHORT_NAME_TO_BUILTIN_SHORT_NAMES_MAP[name] ?: emptyList()


    val CallableMemberDescriptor.isRemoveAtByIndex: Boolean
        get() = name.asString() == "removeAt" && fqNameOrNull() == REMOVE_AT_FQ_NAME
}

@Suppress("UNCHECKED_CAST")
fun <T : CallableMemberDescriptor> T.getBuiltinSpecialOverridden(): T? {
    return when (this) {
        is PropertyDescriptor, is PropertyAccessorDescriptor ->
            firstOverridden { BuiltinSpecialProperties.hasBuiltinSpecialPropertyFqName(it.propertyIfAccessor) } as T?
        else -> firstOverridden { BuiltinSpecialMethods.isBuiltinFunctionWithDifferentNameInJvm(it) } as T?
    }
}

fun CallableMemberDescriptor.overridesBuiltinSpecialDeclaration(): Boolean = getBuiltinSpecialOverridden() != null

fun getJvmMethodNameIfSpecial(callableMemberDescriptor: CallableMemberDescriptor): String? {
    val builtinOverridden = getBuiltinOverriddenThatAffectsJvmName(callableMemberDescriptor)?.propertyIfAccessor
            ?: return null
    return when (builtinOverridden) {
        is PropertyDescriptor -> builtinOverridden.getBuiltinSpecialPropertyGetterName()
        else -> BuiltinSpecialMethods.getSpecialJvmName(builtinOverridden)?.asString()
    }
}

private fun getBuiltinOverriddenThatAffectsJvmName(
        callableMemberDescriptor: CallableMemberDescriptor
): CallableMemberDescriptor? {
    val overriddenBuiltin = callableMemberDescriptor.getBuiltinSpecialOverridden() ?: return null

    if (callableMemberDescriptor.isFromJavaOrBuiltins()) return overriddenBuiltin

    return null
}

// Util methods
private val CallableMemberDescriptor.isFromJava: Boolean
    get() = propertyIfAccessor is JavaCallableMemberDescriptor && propertyIfAccessor.containingDeclaration is JavaClassDescriptor

private fun CallableMemberDescriptor.isFromBuiltins(): Boolean {
    if (!(propertyIfAccessor.fqNameOrNull()?.firstSegmentIs(KotlinBuiltIns.BUILT_INS_PACKAGE_NAME) ?: false)) return false
    return builtIns.builtInsModule == module
}

private val CallableMemberDescriptor.propertyIfAccessor: CallableMemberDescriptor
    get() = if (this is PropertyAccessorDescriptor) correspondingProperty else this

private fun CallableDescriptor.fqNameOrNull(): FqName? = fqNameUnsafe.check { it.isSafe }?.toSafe()

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

public fun CallableMemberDescriptor.isFromJavaOrBuiltins() = isFromJava || isFromBuiltins()

private fun Map<FqName, Name>.getInversedShortNamesMap(): Map<Name, List<Name>> =
        entrySet().groupBy { it.value }.mapValues { entry -> entry.value.map { it.key.shortName() } }