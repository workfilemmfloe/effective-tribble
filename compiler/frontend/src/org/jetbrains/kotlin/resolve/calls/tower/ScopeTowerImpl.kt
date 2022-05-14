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

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.containsError
import org.jetbrains.kotlin.utils.addToStdlib.check
import java.util.*


internal class ScopeTowerImpl(
        resolutionContext: ResolutionContext<*>,
        override val dynamicScope: MemberScope,
        override val syntheticScopes: SyntheticScopes,
        override val location: LookupLocation
): ScopeTower {
    override val dataFlowInfo: DataFlowDecorator = DataFlowDecoratorImpl(resolutionContext)
    override val lexicalScope: LexicalScope = resolutionContext.scope

    override val implicitReceivers = resolutionContext.scope.getImplicitReceiversHierarchy().
            mapNotNull { it.value.check { !it.type.containsError() } }

    override val isDebuggerContext = resolutionContext.isDebuggerContext
}

private class DataFlowDecoratorImpl(private val resolutionContext: ResolutionContext<*>): DataFlowDecorator {
    private val dataFlowInfo = resolutionContext.dataFlowInfo
    private val cache = HashMap<ReceiverValue, SmartCastInfo>()

    private fun getSmartCastInfo(receiver: ReceiverValue): SmartCastInfo
            = cache.getOrPut(receiver) {
        val dataFlowValue = DataFlowValueFactory.createDataFlowValue(receiver, resolutionContext)
        SmartCastInfo(dataFlowValue, dataFlowInfo.getCollectedTypes(dataFlowValue))
    }

    override fun getDataFlowValue(receiver: ReceiverValue): DataFlowValue = getSmartCastInfo(receiver).dataFlowValue

    override fun isStableReceiver(receiver: ReceiverValue): Boolean = getSmartCastInfo(receiver).dataFlowValue.isStable

    override fun getSmartCastTypes(receiver: ReceiverValue): Set<KotlinType> = getSmartCastInfo(receiver).possibleTypes

    private data class SmartCastInfo(val dataFlowValue: DataFlowValue, val possibleTypes: Set<KotlinType>)
}
