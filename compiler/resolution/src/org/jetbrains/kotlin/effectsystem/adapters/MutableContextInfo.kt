/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.effectsystem.adapters

import org.jetbrains.kotlin.effectsystem.structure.ESEffect
import org.jetbrains.kotlin.effectsystem.structure.ESValue
import org.jetbrains.kotlin.types.KotlinType

class MutableContextInfo private constructor(
        val firedEffects: MutableList<ESEffect>,
        val deniedEffects: MutableList<ESEffect>,
        val subtypes: MutableMap<ESValue, MutableSet<KotlinType>>,
        val notSubtypes: MutableMap<ESValue, MutableSet<KotlinType>>,
        val equalValues: MutableMap<ESValue, MutableSet<ESValue>>,
        val notEqualValues: MutableMap<ESValue, MutableSet<ESValue>>
) {
    companion object {
        val EMPTY: MutableContextInfo get() = MutableContextInfo(
                firedEffects = mutableListOf(),
                deniedEffects = mutableListOf(),
                subtypes = mutableMapOf(),
                notSubtypes = mutableMapOf(),
                equalValues = mutableMapOf(),
                notEqualValues = mutableMapOf()
        )
    }

    fun subtype(value: ESValue, type: KotlinType): MutableContextInfo {
        subtypes.initAndAdd(value, type)
        return this
    }

    fun notSubtype(value: ESValue, type: KotlinType): MutableContextInfo {
        notSubtypes.initAndAdd(value, type)
        return this
    }

    fun equal(left: ESValue, right: ESValue): MutableContextInfo {
        equalValues.initAndAdd(left, right)
        equalValues.initAndAdd(right, left)
        return this
    }

    fun notEqual(left: ESValue, right: ESValue): MutableContextInfo {
        notEqualValues.initAndAdd(left, right)
        notEqualValues.initAndAdd(right, left)
        return this
    }

    fun fire(effect: ESEffect): MutableContextInfo {
        firedEffects += effect
        return this
    }

    fun deny(effect: ESEffect): MutableContextInfo {
        deniedEffects += effect
        return this
    }

    fun or(other: MutableContextInfo): MutableContextInfo {
        return MutableContextInfo(
            firedEffects = firedEffects.intersect(other.firedEffects).toMutableList(),
            deniedEffects = deniedEffects.intersect(other.deniedEffects).toMutableList(),
            subtypes = subtypes.intersect(other.subtypes),
            notSubtypes = notSubtypes.intersect(other.notSubtypes),
            equalValues = equalValues.intersect(other.equalValues),
            notEqualValues = notEqualValues.intersect(other.notEqualValues)
        )
    }

    fun and(other: MutableContextInfo): MutableContextInfo {
        return MutableContextInfo(
            firedEffects = firedEffects.union(other.firedEffects).toMutableList(),
            deniedEffects = deniedEffects.union(other.deniedEffects).toMutableList(),
            subtypes = subtypes.union(other.subtypes),
            notSubtypes = notSubtypes.union(other.notSubtypes),
            equalValues = equalValues.union(other.equalValues),
            notEqualValues = notEqualValues.union(other.notEqualValues)
        )
    }

    private fun <D> MutableMap<ESValue, MutableSet<D>>.intersect(that: MutableMap<ESValue, MutableSet<D>>): MutableMap<ESValue, MutableSet<D>> {
        val result = mutableMapOf<ESValue, MutableSet<D>>()

        val allKeys = this.keys.intersect(that.keys)
        allKeys.forEach {
            val newValues = this[it]!!.intersect(that[it]!!)
            if (newValues.isNotEmpty()) result[it] = newValues.toMutableSet()
        }
        return result
    }

    private fun <D> Map<ESValue, MutableSet<D>>.union(that: Map<ESValue, MutableSet<D>>): MutableMap<ESValue, MutableSet<D>> {
        val result = mutableMapOf<ESValue, MutableSet<D>>()
        result.putAll(this)
        that.entries.forEach { (thatKey, thatValue) ->
            val oldValue = result[thatKey] ?: mutableSetOf()
            oldValue.addAll(thatValue)
            result[thatKey] = oldValue
        }
        return result
    }

    private fun <D> MutableMap<ESValue, MutableSet<D>>.initAndAdd(key: ESValue, value: D) {
        this.compute(key) { _, maybeValues ->
            val setOfValues = maybeValues ?: mutableSetOf()
            setOfValues.add(value)
            setOfValues
        }
    }

    fun print(): String {
        return with(StringBuffer()) {
            val info = this@MutableContextInfo

            append("Fired effects: ")
            append(info.firedEffects.joinToString(separator = ", " ))
            appendln("")

            append("Denied effects: ")
            append(info.deniedEffects.joinToString(separator = ", " ))
            appendln()

            subtypes.entries.filter { it.value.isNotEmpty() }.forEach { (key, value) ->
                append(key.toString())
                append(" is ")
                appendln(value.toString())
            }

            notSubtypes.entries.filter { it.value.isNotEmpty() }.forEach { (key, value) ->
                append(key.toString())
                append(" !is ")
                appendln(value.toString())
            }

            equalValues.entries.filter { it.value.isNotEmpty() }.forEach { (key, value) ->
                append(key.toString())
                append(" == ")
                appendln(value.toString())
            }

            notEqualValues.entries.filter { it.value.isNotEmpty() }.forEach { (key, value) ->
                append(key.toString())
                append(" != ")
                appendln(value.toString())
            }

            this.toString()
        }
    }
}