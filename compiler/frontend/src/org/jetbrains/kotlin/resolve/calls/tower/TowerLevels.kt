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

import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.descriptorUtil.hasClassValueDescriptor
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.ResolutionScope
import org.jetbrains.kotlin.resolve.scopes.receivers.QualifierReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.types.typeUtil.getImmediateSuperclassNotAny
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

internal abstract class AbstractScopeTowerLevel(
        protected val scopeTower: ScopeTower
): ScopeTowerLevel {
    protected val location: LookupLocation get() = scopeTower.location

    protected fun <D : CallableDescriptor> createCandidateDescriptor(
            descriptor: D,
            dispatchReceiver: ReceiverValue?,
            specialError: ResolutionDiagnostic? = null,
            dispatchReceiverSmartCastType: KotlinType? = null
    ): CandidateWithBoundDispatchReceiver<D> {
        val diagnostics = SmartList<ResolutionDiagnostic>()
        diagnostics.addIfNotNull(specialError)

        if (ErrorUtils.isError(descriptor)) {
            diagnostics.add(ErrorDescriptorDiagnostic)
        }
        else {
            if (descriptor.isSynthesized) diagnostics.add(SynthesizedDescriptorDiagnostic)
            if (dispatchReceiverSmartCastType != null) diagnostics.add(UsedSmartCastForDispatchReceiver(dispatchReceiverSmartCastType))

            Visibilities.findInvisibleMember(
                    dispatchReceiver ?: ReceiverValue.NO_RECEIVER, descriptor,
                    scopeTower.lexicalScope.ownerDescriptor
            )?.let { diagnostics.add(VisibilityError(it)) }
        }
        return CandidateWithBoundDispatchReceiverImpl(dispatchReceiver, descriptor, diagnostics)
    }

}

// todo KT-9538 Unresolved inner class via subclass reference
// todo add static methods & fields with error
internal class ReceiverScopeTowerLevel(
        scopeTower: ScopeTower,
        val dispatchReceiver: ReceiverValue
): AbstractScopeTowerLevel(scopeTower) {

    private fun <D: CallableDescriptor> collectMembers(
            getMembers: ResolutionScope.(KotlinType?) -> Collection<D>
    ): Collection<CandidateWithBoundDispatchReceiver<D>> {
        val result = ArrayList<CandidateWithBoundDispatchReceiver<D>>(0)
        dispatchReceiver.type.memberScope.getMembers(dispatchReceiver.type).mapTo(result) {
            createCandidateDescriptor(it, dispatchReceiver)
        }

        val smartCastPossibleTypes = scopeTower.dataFlowInfo.getSmartCastTypes(dispatchReceiver)
        val unstableError = if (scopeTower.dataFlowInfo.isStableReceiver(dispatchReceiver)) null else UnstableSmartCastDiagnostic

        for (possibleType in smartCastPossibleTypes) {
            possibleType.memberScope.getMembers(possibleType).mapTo(result) {
                createCandidateDescriptor(it, dispatchReceiver, unstableError, dispatchReceiverSmartCastType = possibleType)
            }
        }

        if (dispatchReceiver.type.isDynamic()) {
            scopeTower.dynamicScope.getMembers(null).mapTo(result) {
                createCandidateDescriptor(it, dispatchReceiver, DynamicDescriptorDiagnostic)
            }
        }

        return result
    }

    override fun getVariables(name: Name): Collection<CandidateWithBoundDispatchReceiver<VariableDescriptor>> {
        return collectMembers { getContributedVariables(name, location) }
    }

    override fun getFunctions(name: Name): Collection<CandidateWithBoundDispatchReceiver<FunctionDescriptor>> {
        return collectMembers {
            getContributedFunctions(name, location) + it.getInnerConstructors(name, location)
        }
    }
}

internal class QualifierScopeTowerLevel(scopeTower: ScopeTower, val qualifier: QualifierReceiver) : AbstractScopeTowerLevel(scopeTower) {
    override fun getVariables(name: Name) = qualifier.getNestedClassesAndPackageMembersScope()
            .getContributedVariablesAndObjects(name, location).map {
                createCandidateDescriptor(it, dispatchReceiver = null)
            }

    override fun getFunctions(name: Name) = qualifier.getNestedClassesAndPackageMembersScope()
            .getContributedFunctionsAndConstructors(name, location).map {
                createCandidateDescriptor(it, dispatchReceiver = null)
            }
}

// KT-3335 Creating imported super class' inner class fails in codegen
internal open class ScopeBasedTowerLevel protected constructor(
        scopeTower: ScopeTower,
        private val resolutionScope: ResolutionScope
) : AbstractScopeTowerLevel(scopeTower) {

    internal constructor(scopeTower: ScopeTower, lexicalScope: LexicalScope): this(scopeTower, lexicalScope as ResolutionScope)

    override fun getVariables(name: Name): Collection<CandidateWithBoundDispatchReceiver<VariableDescriptor>>
            = resolutionScope.getContributedVariablesAndObjects(name, location).map {
                createCandidateDescriptor(it, dispatchReceiver = null)
            }

    override fun getFunctions(name: Name): Collection<CandidateWithBoundDispatchReceiver<FunctionDescriptor>>
            = resolutionScope.getContributedFunctionsAndConstructors(name, location).map {
                createCandidateDescriptor(it, dispatchReceiver = null)
            }
}

internal class ImportingScopeBasedTowerLevel(
        scopeTower: ScopeTower,
        private val importingScope: ImportingScope,
        private val receiversForSyntheticExtensions: Collection<KotlinType>
): ScopeBasedTowerLevel(scopeTower, importingScope) {

    override fun getVariables(name: Name): Collection<CandidateWithBoundDispatchReceiver<VariableDescriptor>> {
        val synthetic = importingScope.getContributedSyntheticExtensionProperties(receiversForSyntheticExtensions, name, location).map {
            createCandidateDescriptor(it, dispatchReceiver = null)
        }
        return super.getVariables(name) + synthetic
    }

    override fun getFunctions(name: Name): Collection<CandidateWithBoundDispatchReceiver<FunctionDescriptor>> {
        val synthetic = importingScope.getContributedSyntheticExtensionFunctions(receiversForSyntheticExtensions, name, location).map {
            createCandidateDescriptor(it, dispatchReceiver = null)
        }
        return super.getFunctions(name) + synthetic
    }
}

private fun KotlinType.getClassifierFromMeAndSuperclasses(name: Name, location: LookupLocation): ClassifierDescriptor? {
    var superclass: KotlinType? = this
    while (superclass != null) {
        superclass.memberScope.getContributedClassifier(name, location)?.let { return it }
        superclass = superclass.getImmediateSuperclassNotAny()
    }
    return null
}

private fun KotlinType?.getInnerConstructors(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
    val classifierDescriptor = getClassWithConstructors(this?.getClassifierFromMeAndSuperclasses(name, location))
    return classifierDescriptor?.constructors?.filter { it.dispatchReceiverParameter != null } ?: emptyList()
}

private fun ResolutionScope.getContributedFunctionsAndConstructors(name: Name, location: LookupLocation): Collection<FunctionDescriptor> {
    val classWithConstructors = getClassWithConstructors(getContributedClassifier(name, location))
    return getContributedFunctions(name, location) +
           (classWithConstructors?.constructors?.filter { it.dispatchReceiverParameter == null } ?: emptyList())
}

private fun ResolutionScope.getContributedVariablesAndObjects(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
    val objectDescriptor = getFakeDescriptorForObject(getContributedClassifier(name, location))

    return getContributedVariables(name, location) + listOfNotNull(objectDescriptor)
}


private fun getFakeDescriptorForObject(classifier: ClassifierDescriptor?): FakeCallableDescriptorForObject? {
    if (classifier !is ClassDescriptor || !classifier.hasClassValueDescriptor) return null // todo

    return FakeCallableDescriptorForObject(classifier)
}

private fun getClassWithConstructors(classifier: ClassifierDescriptor?): ClassDescriptor? {
    if (classifier !is ClassDescriptor || ErrorUtils.isError(classifier)
        // Constructors of singletons shouldn't be callable from the code
        || classifier.kind.isSingleton) {
        return null
    }
    else {
        return classifier
    }
}