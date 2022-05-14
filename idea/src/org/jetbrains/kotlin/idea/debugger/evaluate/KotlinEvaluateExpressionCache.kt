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

package org.jetbrains.kotlin.idea.debugger.evaluate

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.MultiMap
import org.apache.log4j.Logger
import org.jetbrains.eval4j.Value
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

class KotlinEvaluateExpressionCache(val project: Project) {

    private val cachedCompiledData = CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result<MultiMap<String, CompiledDataDescriptor>>(
                        MultiMap.create(), PsiModificationTracker.MODIFICATION_COUNT)
            }, false)

    companion object {
        private val LOG = Logger.getLogger(javaClass<KotlinEvaluateExpressionCache>())!!

        fun getInstance(project: Project) = ServiceManager.getService(project, javaClass<KotlinEvaluateExpressionCache>())!!

        fun getOrCreateCompiledData(
                codeFragment: KtCodeFragment,
                sourcePosition: SourcePosition,
                evaluationContext: EvaluationContextImpl,
                create: (KtCodeFragment, SourcePosition) -> CompiledDataDescriptor
        ): CompiledDataDescriptor {
            val evaluateExpressionCache = getInstance(codeFragment.getProject())

            return synchronized<CompiledDataDescriptor>(evaluateExpressionCache.cachedCompiledData) {
                val cache = evaluateExpressionCache.cachedCompiledData.getValue()!!
                val text = "${codeFragment.importsToString()}\n${codeFragment.getText()}"

                val answer = cache[text].firstOrNull {
                    it.sourcePosition == sourcePosition || evaluateExpressionCache.canBeEvaluatedInThisContext(it, evaluationContext)
                }
                if (answer != null) return@synchronized answer

                val newCompiledData = create(codeFragment, sourcePosition)
                LOG.debug("Compile bytecode for ${codeFragment.getText()}")

                cache.putValue(text, newCompiledData)
                return@synchronized newCompiledData
            }
        }
    }

    private fun canBeEvaluatedInThisContext(compiledData: CompiledDataDescriptor, context: EvaluationContextImpl): Boolean {
        val frameVisitor = FrameVisitor(context)
        return compiledData.parameters.all { p ->
            val (name, jetType) = p
            val value = frameVisitor.findValue(name, asmType = null, checkType = false, failIfNotFound = false)
            if (value == null) return@all false

            val thisDescriptor = value.asmType.getClassDescriptor(project)
            val superClassDescriptor = jetType.getConstructor().getDeclarationDescriptor() as? ClassDescriptor
            return@all thisDescriptor != null && superClassDescriptor != null && runReadAction { DescriptorUtils.isSubclass(thisDescriptor, superClassDescriptor) }
        }
    }

    data class CompiledDataDescriptor(
            val bytecodes: ByteArray,
            val additionalClasses: List<Pair<String, ByteArray>>,
            val sourcePosition: SourcePosition,
            val parameters: ParametersDescriptor
    )

    class ParametersDescriptor : Iterable<Parameter> {
        private val list = ArrayList<Parameter>()

        fun add(name: String, jetType: KotlinType, value: Value? = null) {
            list.add(Parameter(name, jetType, value))
        }

        override fun iterator() = list.iterator()
    }

    data class Parameter(val callText: String, val type: KotlinType, val value: Value? = null)
}
