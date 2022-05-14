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

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassQualifier
import org.jetbrains.kotlin.resolve.scopes.receivers.Receiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.containsError
import org.jetbrains.kotlin.utils.addToStdlib.check
import java.util.*


internal class CandidateWithBoundDispatchReceiverImpl<D : CallableDescriptor>(
        override val dispatchReceiver: ReceiverValue?,
        override val descriptor: D,
        override val diagnostics: List<ResolutionDiagnostic>
) : CandidateWithBoundDispatchReceiver<D>

internal class ScopeTowerImpl(
        resolutionContext: ResolutionContext<*>,
        override val dynamicScope: MemberScope,
        private val explicitReceiver: Receiver?,
        override val location: LookupLocation
): ScopeTower {
    override val dataFlowInfo: DataFlowDecorator = DataFlowDecoratorImpl(resolutionContext)
    override val lexicalScope: LexicalScope = resolutionContext.scope

    override val implicitReceivers = resolutionContext.scope.getImplicitReceiversHierarchy().
            mapNotNull { it.value.check { !it.type.containsError() } }

    override val levels: Sequence<ScopeTowerLevel> = createPrototypeLevels().asSequence().map { it.asTowerLevel(this) }

    // we shouldn't calculate this before we entrance to some importing scope
    private val receiversForSyntheticExtensions: Collection<KotlinType> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        if (explicitReceiver != null) {
            if (explicitReceiver is ReceiverValue) {
                return@lazy dataFlowInfo.getAllPossibleTypes(explicitReceiver)
            }

            if (explicitReceiver is ClassQualifier) {
                explicitReceiver.classValueReceiver?.let {
                    return@lazy dataFlowInfo.getAllPossibleTypes(it)
                }
            }

            // explicit receiver is package or class without companion object
            emptyList()
        }
        else {
            implicitReceivers.flatMap { dataFlowInfo.getAllPossibleTypes(it) }
        }
    }

    private sealed class LevelFactory {
        abstract fun asTowerLevel(resolveTower: ScopeTower): ScopeTowerLevel

        class Scope(val lexicalScope: LexicalScope): LevelFactory() {
            override fun asTowerLevel(resolveTower: ScopeTower) = ScopeBasedTowerLevel(resolveTower, lexicalScope)
        }

        class Receiver(val implicitReceiver: ReceiverParameterDescriptor): LevelFactory() {
            override fun asTowerLevel(resolveTower: ScopeTower) = ReceiverScopeTowerLevel(resolveTower, implicitReceiver.value)
        }

        class ImportingScopeFactory(val importingScope: ImportingScope, val lazyReceiversForSyntheticExtensions: () -> Collection<KotlinType>): LevelFactory() {
            override fun asTowerLevel(resolveTower: ScopeTower) = ImportingScopeBasedTowerLevel(resolveTower, importingScope, lazyReceiversForSyntheticExtensions())
        }
    }

    private fun createPrototypeLevels(): List<LevelFactory> {
        val result = ArrayList<LevelFactory>()

        // locals win
        lexicalScope.parentsWithSelf.
                filterIsInstance<LexicalScope>().
                filter { it.kind.withLocalDescriptors }.
                mapTo(result) { LevelFactory.Scope(it) }

        lexicalScope.parentsWithSelf.forEach { scope ->
            if (scope is LexicalScope) {
                if (!scope.kind.withLocalDescriptors) result.add(LevelFactory.Scope(scope))

                scope.implicitReceiver?.let { result.add(LevelFactory.Receiver(it)) }
            }
            else {
                result.add(LevelFactory.ImportingScopeFactory(scope as ImportingScope, { receiversForSyntheticExtensions }))
            }
        }

        return result
    }

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

    override fun isStableReceiver(receiver: ReceiverValue): Boolean = getSmartCastInfo(receiver).dataFlowValue.isPredictable

    override fun getSmartCastTypes(receiver: ReceiverValue): Set<KotlinType> = getSmartCastInfo(receiver).possibleTypes

    private data class SmartCastInfo(val dataFlowValue: DataFlowValue, val possibleTypes: Set<KotlinType>)
}
