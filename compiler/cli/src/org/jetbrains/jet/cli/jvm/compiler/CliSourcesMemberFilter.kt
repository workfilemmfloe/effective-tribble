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

package org.jetbrains.jet.cli.jvm.compiler

import org.jetbrains.jet.descriptors.serialization.descriptors.MemberFilter
import org.jetbrains.jet.descriptors.serialization.ProtoBuf
import org.jetbrains.jet.codegen.PackageCodegen.getPackagePartInternalName
import org.jetbrains.jet.descriptors.serialization.NameResolver
import org.jetbrains.jet.descriptors.serialization.JavaProtoBuf
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor
import org.jetbrains.jet.lang.resolve.java.JvmClassName

public class CliSourcesMemberFilter(environment: JetCoreEnvironment): MemberFilter {
    val packagePartClassNames = environment.getSourceFiles().map { getPackagePartInternalName(it) }.toSet()

    override fun acceptPackagePartClass(container: PackageFragmentDescriptor, member: ProtoBuf.Callable, nameResolver: NameResolver): Boolean {
        if (member.hasExtension(JavaProtoBuf.implClassName)) {
            val shortName = nameResolver.getName(member.getExtension(JavaProtoBuf.implClassName)!!)
            val fqName = container.fqName.child(shortName)
            val internalName = JvmClassName.byFqNameWithoutInnerClasses(fqName).getInternalName()
            return internalName !in packagePartClassNames
        }
        return true
    }
}