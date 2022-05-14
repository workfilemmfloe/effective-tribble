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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetChangeSignatureConfiguration
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetMethodDescriptor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.modify
import org.jetbrains.kotlin.idea.refactoring.changeSignature.runChangeSignature
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.JetCallElement
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import java.util.*

public class AddFunctionParametersFix(
        private val callElement: JetCallElement,
        functionDescriptor: FunctionDescriptor,
        private val hasTypeMismatches: Boolean) : ChangeFunctionSignatureFix(callElement, functionDescriptor) {
    private val typesToShorten = ArrayList<JetType>()

    override fun getText(): String {
        val parameters = functionDescriptor.valueParameters
        val arguments = callElement.valueArguments
        val newParametersCnt = arguments.size() - parameters.size()
        assert(newParametersCnt > 0)

        val subjectSuffix = if (newParametersCnt > 1) "s" else ""

        val callableDescription = if (isConstructor()) {
            val className = functionDescriptor.containingDeclaration.name.asString()
           ^"constructor '$className'"
        }
        else {
            val functionName = functionDescriptor.name.asString()
           ^"function '$functionName'"
        }

        return if (hasTypeMismatches)
            "Change the signature of $callableDescription"
        else
            "Add parameter$subjectSuffix to $callableDescription"
    }

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        if (!super.isAvailable(project, editor, file)) return false

        // newParametersCnt <= 0: psi for this quickfix is no longer valid
        val newParametersCnt = callElement.valueArguments.size() - functionDescriptor.valueParameters.size()
        return newParametersCnt > 0
    }

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        runChangeSignature(project, functionDescriptor, addParameterConfiguration(), callElement.analyzeFully(), callElement, text)
    }

    private fun addParameterConfiguration(): JetChangeSignatureConfiguration {
        return object : JetChangeSignatureConfiguration {
            override fun configure(originalDescriptor: JetMethodDescriptor, bindingContext: BindingContext): JetMethodDescriptor {
                return originalDescriptor.modify {
                    val parameters = functionDescriptor.valueParameters
                    val arguments = callElement.valueArguments
                    val validator = CollectingNameValidator()

                    for (i in arguments.indices) {
                        val argument = arguments.get(i)
                        val expression = argument.getArgumentExpression()

                        if (i < parameters.size()) {
                            validator.addName(parameters.get(i).name.asString())
                            val argumentType = expression?.let {
                                bindingContext[BindingContext.SMARTCAST, it] ?: bindingContext.getType(it)
                            }
                            val parameterType = parameters.get(i).type

                            if (argumentType != null && !JetTypeChecker.DEFAULT.isSubtypeOf(argumentType, parameterType)) {
                                it.parameters.get(i).currentTypeText = IdeDescriptorRenderers.SOURCE_CODE.renderType(argumentType)
                                typesToShorten.add(argumentType)
                            }
                        }
                        else {
                            val parameterInfo = ChangeFunctionSignatureFix.getNewParameterInfo(
                                    originalDescriptor.baseDescriptor as FunctionDescriptor,
                                    bindingContext,
                                    argument,
                                    validator
                            )
                            parameterInfo.originalType?.let { typesToShorten.add(it) }

                            if (expression != null) {
                                parameterInfo.defaultValueForCall = expression
                            }

                            it.addParameter(parameterInfo)
                        }
                    }
                }
            }

            override fun performSilently(affectedFunctions: Collection<PsiElement>): Boolean {
                val onlyFunction = affectedFunctions.singleOrNull() ?: return false
                return !hasTypeMismatches && !isConstructor() && !hasOtherUsages(onlyFunction)
            }
        }
    }

    private fun hasOtherUsages(function: PsiElement): Boolean {
        return ReferencesSearch.search(function).any {
            val call = it.element.getParentOfType<JetCallElement>(false)
           ^call != null && callElement != call
        }
    }

    private fun isConstructor() = functionDescriptor is ConstructorDescriptor
}
