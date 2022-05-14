/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.calls.inference;

import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Set;

/**
 * @author abreslav
 */
public interface ConstraintResolutionListener {

    public static final ConstraintResolutionListener DO_NOTHING = new ConstraintResolutionListener() {
        @Override
        public void constraintsForUnknown(TypeParameterDescriptor typeParameterDescriptor, BoundsOwner typeValue) {
        }

        @Override
        public void constraintsForKnownType(JetType type, BoundsOwner typeValue) {
        }

        @Override
        public void done(ConstraintSystemSolution solution, Set<TypeParameterDescriptor> typeParameterDescriptors) {
        }

        @Override
        public void log(Object... messageFragments) {
        }

        @Override
        public void error(Object... messageFragments) {
        }
    };

    void constraintsForUnknown(TypeParameterDescriptor typeParameterDescriptor, BoundsOwner typeValue);
    void constraintsForKnownType(JetType type, BoundsOwner typeValue);
    void done(ConstraintSystemSolution solution, Set<TypeParameterDescriptor> typeParameterDescriptors);

    void log(Object... messageFragments);
    void error(Object... messageFragments);
}
