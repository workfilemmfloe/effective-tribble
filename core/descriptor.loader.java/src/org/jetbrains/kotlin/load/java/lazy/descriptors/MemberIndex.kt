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

package org.jetbrains.kotlin.load.java.lazy.descriptors

import org.jetbrains.kotlin.load.java.components.DescriptorResolverUtils
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaField
import org.jetbrains.kotlin.load.java.structure.JavaMember
import org.jetbrains.kotlin.load.java.structure.JavaMethod
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.util.*

interface MemberIndex {
    fun findMethodsByName(name: Name): Collection<JavaMethod>
    fun getMethodNames(nameFilter: (Name) -> Boolean): Collection<Name>

    fun findFieldByName(name: Name): JavaField?
    fun getAllFieldNames(): Collection<Name>
}

object EMPTY_MEMBER_INDEX : MemberIndex {
    override fun findMethodsByName(name: Name) = listOf<JavaMethod>()
    override fun getMethodNames(nameFilter: (Name) -> Boolean) = listOf<Name>()

    override fun findFieldByName(name: Name): JavaField? = null
    override fun getAllFieldNames() = listOf<Name>()
}

private val ADDITIONAL_MEMBER_NAMES_MAP = mapOf(
        FqName("java.util.List") to listOf(Name.identifier("removeAt")),
        FqName("java.lang.CharSequence") to listOf(Name.identifier("get")),
        FqName("java.util.Map") to listOf(Name.identifier("keys"), Name.identifier("entries")),
        FqName("java.lang.Number") to listOf(
                Name.identifier("toByte"), Name.identifier("toShort"), Name.identifier("toInt"), Name.identifier("toLong"),
                Name.identifier("toFloat"), Name.identifier("toDouble")
        )
)

open class ClassMemberIndex(val jClass: JavaClass, val memberFilter: (JavaMember) -> Boolean) : MemberIndex {
    private val methodFilter = {
        m: JavaMethod ->
        memberFilter(m) && !DescriptorResolverUtils.isObjectMethodInInterface(m)
    }

    private val methods = jClass.methods.asSequence().filter(methodFilter).groupBy { m -> m.name }
    private val fields = jClass.fields.asSequence().filter(memberFilter).associateBy { m -> m.name }

    override fun findMethodsByName(name: Name): Collection<JavaMethod> = methods[name] ?: listOf()
    override fun getMethodNames(nameFilter: (Name) -> Boolean): Collection<Name> =
            jClass.getAllMemberNames(methodFilter) { methods }

    override fun findFieldByName(name: Name): JavaField? = fields[name]
    override fun getAllFieldNames(): Collection<Name> = jClass.getAllMemberNames(memberFilter) { fields }
}

private fun JavaClass.getNonDeclaredMethodNames(): List<Name> {
    return ADDITIONAL_MEMBER_NAMES_MAP[this.fqName].orEmpty()
}

private fun <M : JavaMember> JavaClass.getAllMemberNames(filter: (M) -> Boolean, getMembers: JavaClass.() -> Collection<M>): Set<Name> {
    val result = HashSet<Name>()
    val visitedSuperClasses = HashSet<JavaClass>()

    fun JavaClass.visit(): Unit {
        if (!visitedSuperClasses.add(this)) return

        for (member in getMembers()) {
            if (filter(member)) {
                result.add(member.name)
            }
        }

        for (supertype in supertypes) {
            val classifier = supertype.classifier
            if (classifier is JavaClass) {
                result.addAll(classifier.getNonDeclaredMethodNames())
                classifier.visit()
            }
        }
    }

    this.visit()
    return result
}
