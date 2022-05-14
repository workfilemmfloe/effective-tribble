/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.resolver

import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaClassDescriptor
import org.jetbrains.jet.lang.resolve.java.descriptor.SamConstructorDescriptor

public trait SamConversionResolver {
    public fun resolveSamConstructor(name: Name, scope: JetScope): SamConstructorDescriptor?

    public fun <D : FunctionDescriptor> resolveSamAdapter(original: D): D?

    public fun resolveFunctionTypeIfSamInterface(
            classDescriptor: JavaClassDescriptor,
            resolveMethod: (JavaMethod) -> FunctionDescriptor
    ): JetType?
}
