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

package org.jetbrains.kotlin.idea.highlighter.renderersUtil

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.NameShortness
import org.jetbrains.kotlin.renderer.RenderingFormat
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.hasTypeMismatchErrorOnParameter
import org.jetbrains.kotlin.resolve.calls.callUtil.hasUnmappedArguments
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.types.ErrorUtils

private val RED_TEMPLATE = "<font color=red><b>%s</b></font>"
private val STRONG_TEMPLATE = "<b>%s</b>"

fun renderStrong(o: Any): String = STRONG_TEMPLATE.format(o)

fun renderError(o: Any): String = RED_TEMPLATE.format(o)

fun renderStrong(o: Any, error: Boolean): String = (if (error) RED_TEMPLATE else STRONG_TEMPLATE).format(o)

private val HTML_FOR_UNINFERRED_TYPE_PARAMS: DescriptorRenderer = DescriptorRenderer.withOptions {
    uninferredTypeParameterAsName = true
    modifiers = emptySet()
    nameShortness = NameShortness.SHORT
    textFormat = RenderingFormat.HTML
}

fun <D : CallableDescriptor> renderResolvedCall(resolvedCall: ResolvedCall<D>): String {
    val htmlRenderer = HTML_FOR_UNINFERRED_TYPE_PARAMS
    val stringBuilder = StringBuilder("")
    val indent = "&nbsp;&nbsp;"

    fun append(any: Any): StringBuilder = stringBuilder.append(any)

    fun renderParameter(parameter: ValueParameterDescriptor): String {
        val varargElementType = parameter.varargElementType
        val parameterType = varargElementType ?: parameter.type
        val renderedParameter =
                (if (varargElementType != null) "<b>vararg</b> " else "") +
                htmlRenderer.renderType(parameterType) +
                if (parameter.hasDefaultValue()) " = ..." else ""
        if (resolvedCall.hasTypeMismatchErrorOnParameter(parameter)) {
            return renderError(renderedParameter)
        }
        return renderedParameter
    }

    fun appendTypeParametersSubstitution() {
        val parametersToArgumentsMap = resolvedCall.typeArguments
        fun TypeParameterDescriptor.isInferred(): Boolean {
            val typeArgument = parametersToArgumentsMap[this]
            if (typeArgument == null) return false
            return !ErrorUtils.isUninferredParameter(typeArgument)
        }

        val typeParameters = resolvedCall.candidateDescriptor.typeParameters
        val (inferredTypeParameters, notInferredTypeParameters) = typeParameters.partition { parameter -> parameter.isInferred() }

        append("<br/>$indent<i>where</i> ")
        if (!notInferredTypeParameters.isEmpty()) {
            append(notInferredTypeParameters.map { typeParameter -> renderError(typeParameter.name) }.joinToString())
            append("<i> cannot be inferred</i>")
            if (!inferredTypeParameters.isEmpty()) {
                append("; ")
            }
        }

        val typeParameterToTypeArgumentMap = resolvedCall.typeArguments
        if (!inferredTypeParameters.isEmpty()) {
            append(inferredTypeParameters.map { typeParameter ->
                    "${typeParameter.name} = ${htmlRenderer.renderType(typeParameterToTypeArgumentMap[typeParameter]!!)}"
                }.joinToString())
        }
    }

    val resultingDescriptor = resolvedCall.resultingDescriptor
    val receiverParameter = resultingDescriptor.extensionReceiverParameter
    if (receiverParameter != null) {
        append(htmlRenderer.renderType(receiverParameter.type)).append(".")
    }
    append(resultingDescriptor.name).append("(")
    append(resultingDescriptor.valueParameters.map { parameter -> renderParameter(parameter) }.joinToString())
    append(if (resolvedCall.hasUnmappedArguments()) renderError(")") else ")")

    if (!resolvedCall.candidateDescriptor.typeParameters.isEmpty()) {
        appendTypeParametersSubstitution()
        append("<i> for </i><br/>$indent")
        append(htmlRenderer.render(resolvedCall.candidateDescriptor))
    }
    else {
        append(" <i>defined in</i> ")
        val containingDeclaration = resultingDescriptor.containingDeclaration
        val fqName = DescriptorUtils.getFqName(containingDeclaration)
        append(if (fqName.isRoot) "root package" else fqName.asString())
    }
    return stringBuilder.toString()
}

