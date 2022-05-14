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

package org.jetbrains.kotlin.android.synthetic.descriptors

import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.SimpleType
import java.util.*

class LazySyntheticElementResolveContext(private val module: ModuleDescriptor, storageManager: StorageManager) {
    private val context = storageManager.createLazyValue {
        module.createResolveContext()
    }

    internal operator fun invoke() = context()

    private fun ModuleDescriptor.createResolveContext(): SyntheticElementResolveContext {
        fun find(fqName: String) = module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName(fqName)))

        val viewDescriptor = find(AndroidConst.VIEW_FQNAME) ?: return SyntheticElementResolveContext.ERROR_CONTEXT
        val activityDescriptor = find(AndroidConst.ACTIVITY_FQNAME) ?: return SyntheticElementResolveContext.ERROR_CONTEXT
        val fragmentDescriptor = find(AndroidConst.FRAGMENT_FQNAME)
        val dialogDescriptor = find(AndroidConst.DIALOG_FQNAME) ?: return SyntheticElementResolveContext.ERROR_CONTEXT
        val supportActivityDescriptor = find(AndroidConst.SUPPORT_FRAGMENT_ACTIVITY_FQNAME)
        val supportFragmentDescriptor = find(AndroidConst.SUPPORT_FRAGMENT_FQNAME)

        return SyntheticElementResolveContext(
                viewDescriptor.defaultType,
                activityDescriptor.defaultType,
                fragmentDescriptor?.defaultType,
                dialogDescriptor.defaultType,
                supportActivityDescriptor?.defaultType,
                supportFragmentDescriptor?.defaultType)
    }
}

internal class SyntheticElementResolveContext(
        val viewType: SimpleType,
        val activityType: SimpleType,
        val fragmentType: SimpleType?,
        val dialogType: SimpleType,
        val supportActivityType: SimpleType?,
        val supportFragmentType: SimpleType?) {
    companion object {
        private fun errorType() = ErrorUtils.createErrorType("")
        val ERROR_CONTEXT = SyntheticElementResolveContext(errorType(), errorType(), null, errorType(), null, null)
    }

    private val widgetReceivers by lazy {
        val receivers = ArrayList<WidgetReceiver>(4)
        receivers += WidgetReceiver(activityType, mayHaveCache = true)
        receivers += WidgetReceiver(dialogType, mayHaveCache = false)
        fragmentType?.let { receivers += WidgetReceiver(it, mayHaveCache = true) }
        supportFragmentType?.let { receivers += WidgetReceiver(it, mayHaveCache = true) }
        receivers
    }

    val fragmentTypes: List<Pair<SimpleType, SimpleType>> by lazy {
        if (fragmentType == null) {
            emptyList<Pair<SimpleType, SimpleType>>()
        }
        else {
            val types = ArrayList<Pair<SimpleType, SimpleType>>(4)
            types += Pair(activityType, fragmentType)
            types += Pair(fragmentType, fragmentType)
            if (supportActivityType != null && supportFragmentType != null) {
                types += Pair(supportFragmentType, supportFragmentType)
                types += Pair(supportActivityType, supportFragmentType)
            }
            types
        }
    }

    fun getWidgetReceivers(forView: Boolean): List<WidgetReceiver> {
        if (forView) return listOf(WidgetReceiver(viewType, mayHaveCache = false))
        return widgetReceivers
    }

}

class WidgetReceiver(val type: SimpleType, val mayHaveCache: Boolean)