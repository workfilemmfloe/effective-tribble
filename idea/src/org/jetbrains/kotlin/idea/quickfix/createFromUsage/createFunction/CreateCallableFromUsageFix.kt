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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createFunction

import org.jetbrains.kotlin.idea.quickfix.createFromUsage.CreateFromUsageFixBase
import org.jetbrains.kotlin.psi.JetFile
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.JetBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.command.CommandProcessor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToDeclarationUtil
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.idea.refactoring.chooseContainerElementIfNecessary
import org.jetbrains.kotlin.psi.JetClassBody
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.*
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.util.HashSet
import org.jetbrains.kotlin.psi.JetElement

public class CreateCallableFromUsageFix(
        originalExpression: JetExpression,
        val callableInfos: List<CallableInfo>) : CreateFromUsageFixBase(originalExpression) {
    {
        if (callableInfos.size > 1) {
            val receiverSet = callableInfos.mapTo(HashSet<TypeInfo>()) { it.receiverTypeInfo }
            if (receiverSet.size > 1) throw AssertionError("All functions must have common receiver: $receiverSet")

            val possibleContainerSet = callableInfos.mapTo(HashSet<List<JetElement>>()) { it.possibleContainers }
            if (possibleContainerSet.size > 1) throw AssertionError("All functions must have common containers: $possibleContainerSet")
        }
    }

    override fun getText(): String {
        val renderedCallables = callableInfos.map {
            val kind = when (it.kind) {
                CallableKind.FUNCTION -> "function"
                CallableKind.PROPERTY -> "property"
                else -> throw AssertionError("Unexpected callable info: $it")
            }
            "$kind '${it.name}'"
        }

        return JetBundle.message("create.0.from.usage", renderedCallables.joinToString())
    }

    override fun invoke(project: Project, editor: Editor?, file: JetFile?) {
        val callableInfo = callableInfos.first()

        val callableBuilder = CallableBuilderConfiguration(callableInfos, element as JetExpression, file!!, editor!!).createBuilder()

        fun runBuilder(placement: CallablePlacement) {
            callableBuilder.placement = placement
            CommandProcessor.getInstance().executeCommand(project, { callableBuilder.build() }, getText(), null)
        }

        val popupTitle = JetBundle.message("choose.target.class.or.trait.title")
        val receiverTypeCandidates = callableBuilder.computeTypeCandidates(callableInfo.receiverTypeInfo)
        if (receiverTypeCandidates.isNotEmpty()) {
            // TODO: Support generation of Java class members
            val containers = receiverTypeCandidates
                    .map { candidate ->
                        val descriptor = candidate.theType.getConstructor().getDeclarationDescriptor()
                        (DescriptorToDeclarationUtil.getDeclaration(file, descriptor) as? JetClassOrObject)?.let { candidate to it }
                    }
                    .filterNotNull()

            chooseContainerElementIfNecessary(containers, editor, popupTitle, false, { it.second }) {
                runBuilder(CallablePlacement.WithReceiver(it.first))
            }
        }
        else {
            assert(callableInfo.receiverTypeInfo is TypeInfo.Empty, "No receiver type candidates: ${element.getText()} in ${file.getText()}")

            chooseContainerElementIfNecessary(callableInfo.possibleContainers, editor, popupTitle, true, { it }) {
                val container = if (it is JetClassBody) it.getParent() as JetClassOrObject else it
                runBuilder(CallablePlacement.NoReceiver(container))
            }
        }
    }
}

public fun CreateCallableFromUsageFix(
        originalExpression: JetExpression,
        callableInfo: CallableInfo
) : CreateCallableFromUsageFix {
    return CreateCallableFromUsageFix(originalExpression, callableInfo.singletonOrEmptyList())
}
