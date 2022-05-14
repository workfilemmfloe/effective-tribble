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

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.utils.parentsWithSelf
import org.jetbrains.kotlin.utils.addToStdlib.check
import java.util.*

interface TowerContext<C> {
    val name: Name
    val scopeTower: ScopeTower

    fun createCandidate(
            towerCandidate: CandidateWithBoundDispatchReceiver<*>,
            explicitReceiverKind: ExplicitReceiverKind,
            extensionReceiver: ReceiverValue?
    ): C

    fun getStatus(candidate: C): ResolutionCandidateStatus

    fun transformCandidate(variable: C, invoke: C): C

    fun contextForVariable(stripExplicitReceiver: Boolean): TowerContext<C>

    // foo() -> ReceiverValue(foo), context for invoke
    // null means that there is no invoke on variable
    fun contextForInvoke(variable: C, useExplicitReceiver: Boolean): Pair<ReceiverValue, TowerContext<C>>?
}

internal sealed class TowerData {
    object Empty : TowerData()
    class OnlyImplicitReceiver(val implicitReceiver: ReceiverValue): TowerData()
    class TowerLevel(val level: ScopeTowerLevel) : TowerData()
    class BothTowerLevelAndImplicitReceiver(val level: ScopeTowerLevel, val implicitReceiver: ReceiverValue) : TowerData()
}

internal interface ScopeTowerProcessor<C> {
    // Candidates with matched receivers (dispatch receiver was already matched in ScopeTowerLevel)
    // Candidates in one groups have same priority, first group has highest priority.
    fun process(data: TowerData): List<Collection<C>>
}

class TowerResolver {
    internal fun <C> runResolve(
            context: TowerContext<C>,
            processor: ScopeTowerProcessor<C>,
            useOrder: Boolean = true
    ): Collection<C> {
        return run(context, processor, useOrder, SuccessfulResultCollector(context))
    }

    internal fun <C> collectAllCandidates(context: TowerContext<C>, processor: ScopeTowerProcessor<C>): Collection<C> {
        return run(context, processor, false, AllCandidatesCollector(context))
    }

    private fun ScopeTower.createNonLocalLevels(): List<ScopeTowerLevel> {
        val result = ArrayList<ScopeTowerLevel>()

        lexicalScope.parentsWithSelf.forEach { scope ->
            if (scope is LexicalScope) {
                if (!scope.kind.withLocalDescriptors) result.add(ScopeBasedTowerLevel(this, scope))

                scope.implicitReceiver?.let { result.add(ReceiverScopeTowerLevel(this, it.value)) }
            }
            else {
                result.add(ImportingScopeBasedTowerLevel(this, scope as ImportingScope))
            }
        }

        return result
    }

    private fun ScopeTower.createTowerDataList(): List<TowerData> = ArrayList<TowerData>().apply {
        operator fun TowerData.unaryPlus() = add(this)

        val localLevels = lexicalScope.parentsWithSelf.
                filterIsInstance<LexicalScope>().filter { it.kind.withLocalDescriptors }.
                map { ScopeBasedTowerLevel(this@createTowerDataList, it) }

        val nonLocalLevels = createNonLocalLevels()
        val hidesMembersLevel = HidesMembersTowerLevel(this@createTowerDataList)
        val syntheticLevel = SyntheticScopeBasedTowerLevel(this@createTowerDataList, syntheticScopes)

        // hides members extensions for explicit receiver
        + TowerData.TowerLevel(hidesMembersLevel)
        // possibly there is explicit member
        + TowerData.Empty
        // synthetic member for explicit receiver
        + TowerData.TowerLevel(syntheticLevel)

        // local non-extensions or extension for explicit receiver
        for (localLevel in localLevels) {
            + TowerData.TowerLevel(localLevel)
        }

        for (scope in lexicalScope.parentsWithSelf) {
            if (scope is LexicalScope) {
                // statics
                if (!scope.kind.withLocalDescriptors) {
                    + TowerData.TowerLevel(ScopeBasedTowerLevel(this@createTowerDataList, scope))
                }

                val implicitReceiver = scope.implicitReceiver?.value
                if (implicitReceiver != null) {
                    // hides members extensions
                    + TowerData.BothTowerLevelAndImplicitReceiver(hidesMembersLevel, implicitReceiver)

                    // members of implicit receiver or member extension for explicit receiver
                    + TowerData.TowerLevel(ReceiverScopeTowerLevel(this@createTowerDataList, implicitReceiver))

                    // synthetic members
                    + TowerData.BothTowerLevelAndImplicitReceiver(syntheticLevel, implicitReceiver)

                    // invokeExtension on local variable
                    + TowerData.OnlyImplicitReceiver(implicitReceiver)

                    // local extensions for implicit receiver
                    for (localLevel in localLevels) {
                        + TowerData.BothTowerLevelAndImplicitReceiver(localLevel, implicitReceiver)
                    }

                    // extension for implicit receiver
                    for (nonLocalLevel in nonLocalLevels) {
                        + TowerData.BothTowerLevelAndImplicitReceiver(nonLocalLevel, implicitReceiver)
                    }
                }
            }
            else {
                // functions with no receiver or extension for explicit receiver
                + TowerData.TowerLevel(ImportingScopeBasedTowerLevel(this@createTowerDataList, scope as ImportingScope))
            }
        }
    }

    private fun <C> run(
            context: TowerContext<C>,
            processor: ScopeTowerProcessor<C>,
            useOrder: Boolean,
            resultCollector: ResultCollector<C>
    ): Collection<C> {

        for (towerData in context.scopeTower.createTowerDataList()) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

            val candidatesGroups = if (useOrder) {
                processor.process(towerData)
            }
            else {
                listOf(processor.process(towerData).flatMap { it })
            }

            for (candidatesGroup in candidatesGroups) {
                resultCollector.pushCandidates(candidatesGroup)
                resultCollector.getSuccessfulCandidates()?.let { return it }
            }
        }

        return resultCollector.getFinalCandidates()
    }


    internal abstract class ResultCollector<C>(val context: TowerContext<C>) {
        abstract fun getSuccessfulCandidates(): Collection<C>?

        abstract fun getFinalCandidates(): Collection<C>

        fun pushCandidates(candidates: Collection<C>) {
            val filteredCandidates = candidates.filter {
                context.getStatus(it).resultingApplicability != ResolutionCandidateApplicability.HIDDEN
            }
            if (filteredCandidates.isNotEmpty()) addCandidates(filteredCandidates)
        }

        protected abstract fun addCandidates(candidates: Collection<C>)
    }

    internal class AllCandidatesCollector<C>(context: TowerContext<C>): ResultCollector<C>(context) {
        private val allCandidates = ArrayList<C>()

        override fun getSuccessfulCandidates(): Collection<C>? = null

        override fun getFinalCandidates(): Collection<C> = allCandidates

        override fun addCandidates(candidates: Collection<C>) {
            allCandidates.addAll(candidates)
        }

    }

    internal class SuccessfulResultCollector<C>(context: TowerContext<C>): ResultCollector<C>(context) {
        private var currentCandidates: Collection<C> = emptyList()
        private var currentLevel: ResolutionCandidateApplicability? = null

        override fun getSuccessfulCandidates(): Collection<C>? = getResolved() ?: getResolvedSynthetic()

        fun getResolved() = currentCandidates.check { currentLevel == ResolutionCandidateApplicability.RESOLVED }

        fun getResolvedSynthetic() = currentCandidates.check { currentLevel == ResolutionCandidateApplicability.RESOLVED_SYNTHESIZED }

        fun getResolvedLowPriority() = currentCandidates.check { currentLevel == ResolutionCandidateApplicability.RESOLVED_LOW_PRIORITY }

        fun getErrors() = currentCandidates.check {
            currentLevel == null || currentLevel!! > ResolutionCandidateApplicability.RESOLVED_LOW_PRIORITY
        }

        override fun getFinalCandidates() = getResolved() ?: getResolvedSynthetic() ?: getResolvedLowPriority() ?: getErrors() ?: emptyList()

        override fun addCandidates(candidates: Collection<C>) {
            val minimalLevel = candidates.map { context.getStatus(it).resultingApplicability }.min()!!
            if (currentLevel == null || currentLevel!! > minimalLevel) {
                currentLevel = minimalLevel
                currentCandidates = candidates.filter { context.getStatus(it).resultingApplicability == minimalLevel }
            }
        }
    }
}
