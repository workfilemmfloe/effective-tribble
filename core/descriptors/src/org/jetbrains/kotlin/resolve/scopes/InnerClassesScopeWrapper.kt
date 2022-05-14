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

package org.jetbrains.kotlin.resolve.scopes

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.Printer

public class InnerClassesScopeWrapper(val workerScope: MemberScope) : MemberScopeImpl() {
    override fun getContributedClassifier(name: Name, location: LookupLocation) = workerScope.getContributedClassifier(name, location) as? ClassDescriptor

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): List<ClassDescriptor> {
        val restrictedFilter = kindFilter.restrictedToKindsOrNull(DescriptorKindFilter.CLASSIFIERS_MASK) ?: return listOf()
        return workerScope.getContributedDescriptors(restrictedFilter, nameFilter).filterIsInstance<ClassDescriptor>()
    }

    override fun printScopeStructure(p: Printer) {
        p.println("InnerClassesScopeWrapper for scope:")
        workerScope.printScopeStructure(p)
    }

    override fun toString() = "Classes from $workerScope"
}
