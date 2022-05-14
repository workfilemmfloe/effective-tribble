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

package org.jetbrains.kotlin.js.translate.declaration

import org.jetbrains.kotlin.js.backend.ast.JsInvocation
import org.jetbrains.kotlin.js.backend.ast.JsName
import org.jetbrains.kotlin.js.backend.ast.JsNameRef
import org.jetbrains.kotlin.backend.common.bridges.generateBridgesForFunctionDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.translate.context.StaticContext
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils.isNativeObject
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.prototypeOf
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.pureFqn
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.identity

class InterfaceFunctionCopier(val context: StaticContext) {
    private val classModels = mutableMapOf<ClassDescriptor, ClassModel>()

    fun copyInterfaceFunctions(classes: Collection<ClassDescriptor>) {
        val orderedClasses = DFS.topologicalOrder(classes) { current -> DescriptorUtils.getSuperclassDescriptors(current) }.asReversed()

        for (classDescriptor in orderedClasses) {
            addInterfaceDefaultMembers(classDescriptor, context)
        }
    }

    private fun addInterfaceDefaultMembers(descriptor: ClassDescriptor, context: StaticContext) {
        // optimization: don't do anything for native declarations
        if (isNativeObject(descriptor)) return

        val classModel = ClassModel(descriptor)
        classModels[descriptor] = classModel

        val members = descriptor.unsubstitutedMemberScope
                .getContributedDescriptors(DescriptorKindFilter.FUNCTIONS)
                .mapNotNull { it as? CallableMemberDescriptor }

        for (member in members.filter { it.modality != Modality.ABSTRACT && it.kind.isReal }) {
            val names = generateAllNames(member, context)
            val identifiers = names.map { it.ident }
            when (member) {
                is FunctionDescriptor -> classModel.copiedFunctions += identifiers
                is PropertyDescriptor -> classModel.copiedProperties += identifiers
            }
        }

        val superModels = DescriptorUtils.getSuperclassDescriptors(descriptor)
                .filter { it.kind == ClassKind.INTERFACE && !isNativeObject(it) }
                .map { classModels[it]!! }
        for (superModel in superModels) {
            for (name in superModel.copiedFunctions) {
                if (classModel.copiedFunctions.add(name)) {
                    addDefaultMethodFromInterface(name, superModel.descriptor, descriptor, context)
                }
            }
            for (name in superModel.copiedProperties) {
                if (classModel.copiedProperties.add(name)) {
                    addDefaultPropertyFromInterface(name, superModel.descriptor, descriptor, context)
                }
            }
        }
    }

    private fun addDefaultMethodFromInterface(
            name: String,
            sourceDescriptor: ClassDescriptor,
            targetDescriptor: ClassDescriptor,
            context: StaticContext
    ) {
        if (targetDescriptor.module != context.currentModule) return

        val targetPrototype = prototypeOf(pureFqn(context.getInnerNameForDescriptor(targetDescriptor), null))
        val sourcePrototype = prototypeOf(pureFqn(context.getInnerNameForDescriptor(sourceDescriptor), null))
        val targetFunction = JsNameRef(name, targetPrototype)
        val sourceFunction = JsNameRef(name, sourcePrototype)
        context.declarationStatements += JsAstUtils.assignment(targetFunction, sourceFunction).makeStmt()
    }

    private fun addDefaultPropertyFromInterface(
            name: String,
            sourceDescriptor: ClassDescriptor,
            targetDescriptor: ClassDescriptor,
            context: StaticContext
    ) {
        if (targetDescriptor.module != context.currentModule) return

        val targetPrototype = prototypeOf(pureFqn(context.getInnerNameForDescriptor(targetDescriptor), null))
        val sourcePrototype = prototypeOf(pureFqn(context.getInnerNameForDescriptor(sourceDescriptor), null))
        val nameLiteral = context.program.getStringLiteral(name)

        val getPropertyDescriptor = JsInvocation(JsNameRef("getOwnPropertyDescriptor", "Object"), sourcePrototype, nameLiteral)
        val defineProperty = JsAstUtils.defineProperty(targetPrototype, name, getPropertyDescriptor, context.program)

        context.declarationStatements += defineProperty.makeStmt()
    }

    private fun generateAllNames(member: CallableMemberDescriptor, context: StaticContext): Sequence<JsName> {
        return (generateBridges(member) + member).map { context.getNameForDescriptor(it) }.distinctBy { it.ident }
    }

    private fun generateBridges(member: CallableMemberDescriptor): Sequence<CallableMemberDescriptor> = when (member) {
        is FunctionDescriptor -> {
            generateBridgesForFunctionDescriptor(member, identity()) { false }
                    .map { it.from }
                    .asSequence()
        }
        is PropertyDescriptor -> generateBridgesForFunctionDescriptor(member.getter!!, identity()) { false }
                .map { it.from }
                .map { (it as PropertyAccessorDescriptor).correspondingProperty }
                .asSequence()
        else -> error("Expected either be function or property: $member")
    }

    private class ClassModel(val descriptor: ClassDescriptor) {
        val copiedFunctions = mutableSetOf<String>()
        val copiedProperties = mutableSetOf<String>()
    }
}