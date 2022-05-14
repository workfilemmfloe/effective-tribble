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

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.utils.keysToMapExceptNulls
import java.util.Comparator

public object CodegenUtilKt {

    // class Foo : Bar by baz
    //   descriptor = Foo
    //   toTrait = Bar
    //   delegateExpressionType = typeof(baz)
    // return Map<member of Foo, corresponding member of typeOf(baz)>
    @JvmStatic
    public fun getDelegates(
            descriptor: ClassDescriptor,
            toTrait: ClassDescriptor,
            delegateExpressionType: JetType? = null
    ): Map<CallableMemberDescriptor, CallableDescriptor> {
        if (delegateExpressionType?.isDynamic() ?: false) return mapOf();

        return descriptor.getDefaultType().getMemberScope().getDescriptors().asSequence()
            .filterIsInstance<CallableMemberDescriptor>()
            .filter { it.getKind() == CallableMemberDescriptor.Kind.DELEGATION }
            .asIterable()
            .sortedWith(MemberComparator.INSTANCE)
            .keysToMapExceptNulls {
                delegatingMember ->

                val actualDelegates = DescriptorUtils.getAllOverriddenDescriptors(delegatingMember)
                        .filter { it.getContainingDeclaration() == toTrait }
                        .map {
                            overriddenDescriptor ->
                            val scope = (delegateExpressionType ?: toTrait.getDefaultType()).getMemberScope()
                            val name = overriddenDescriptor.getName()

                            // this is the actual member of delegateExpressionType that we are delegating to
                            (scope.getFunctions(name, NoLookupLocation.FROM_BACKEND) + scope.getProperties(name, NoLookupLocation.FROM_BACKEND))
                                    .first {
                                        (listOf(it) + DescriptorUtils.getAllOverriddenDescriptors(it)).map { it.getOriginal() }.contains(overriddenDescriptor.getOriginal())
                                    }
                        }
                assert(actualDelegates.size() <= 1) { "Many delegates found for $delegatingMember: $actualDelegates" }

                actualDelegates.firstOrNull()
            }
    }
}
