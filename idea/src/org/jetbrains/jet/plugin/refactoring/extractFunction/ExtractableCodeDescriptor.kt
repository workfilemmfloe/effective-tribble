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

package org.jetbrains.jet.plugin.refactoring.extractFunction

import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import com.intellij.util.containers.MultiMap
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetThisExpression
import org.jetbrains.jet.plugin.references.JetSimpleNameReference
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.plugin.references.JetSimpleNameReference.ShorteningMode
import org.jetbrains.jet.lang.psi.psiUtil.replaced
import org.jetbrains.jet.lang.psi.JetQualifiedExpression
import org.jetbrains.jet.lang.psi.JetTypeParameter
import org.jetbrains.jet.lang.psi.JetTypeConstraint
import org.jetbrains.jet.plugin.refactoring.extractFunction.AnalysisResult.Status
import org.jetbrains.jet.plugin.refactoring.JetRefactoringBundle
import org.jetbrains.jet.plugin.refactoring.extractFunction.AnalysisResult.ErrorMessage
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.JetDeclaration
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.jet.lang.psi.JetClassBody
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.psi.JetNamedDeclaration
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.types.CommonSupertypes
import org.jetbrains.jet.lang.types.TypeSubstitutor
import org.jetbrains.jet.lang.types.JetTypeImpl
import org.jetbrains.jet.lang.descriptors.annotations.Annotations
import java.util.Collections
import org.jetbrains.jet.lang.types.TypeProjectionImpl
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.types.TypeProjection
import org.jetbrains.jet.lang.types.TypeConstructor
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.plugin.refactoring.extractFunction.OutputValue.ExpressionValue
import org.jetbrains.jet.lang.psi.JetReturnExpression
import org.jetbrains.jet.plugin.refactoring.extractFunction.OutputValue.Jump
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils
import org.jetbrains.jet.lang.types.TypeUtils
import kotlin.properties.Delegates
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.jet.lang.psi.JetCallElement
import org.jetbrains.jet.lang.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.jet.plugin.util.psi.patternMatching.JetPsiRange
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.plugin.util.isUnit

trait Parameter {
    val argumentText: String
    val originalDescriptor: DeclarationDescriptor
    val name: String
    val mirrorVarName: String?
    val parameterType: JetType
    val parameterTypeCandidates: List<JetType>
    val receiverCandidate: Boolean

    fun copy(name: String, parameterType: JetType): Parameter
}

val Parameter.nameForRef: String get() = mirrorVarName ?: name

data class TypeParameter(
        val originalDeclaration: JetTypeParameter,
        val originalConstraints: List<JetTypeConstraint>
)

trait Replacement: Function1<JetElement, JetElement>

trait ParameterReplacement : Replacement {
    val parameter: Parameter
    fun copy(parameter: Parameter): ParameterReplacement
}

class RenameReplacement(override val parameter: Parameter): ParameterReplacement {
    override fun copy(parameter: Parameter) = RenameReplacement(parameter)

    [suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")]
    override fun invoke(e: JetElement): JetElement {
        val thisExpr = e.getParent() as? JetThisExpression
        return (thisExpr ?: e).replaced(JetPsiFactory(e).createSimpleName(parameter.nameForRef))
    }
}

class AddPrefixReplacement(override val parameter: Parameter): ParameterReplacement {
    override fun copy(parameter: Parameter) = AddPrefixReplacement(parameter)

    [suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")]
    override fun invoke(e: JetElement): JetElement {
        val selector = (e.getParent() as? JetCallExpression) ?: e
        val newExpr = selector.replace(JetPsiFactory(e).createExpression("${parameter.nameForRef}.${selector.getText()}")
        ) as JetQualifiedExpression

        return with(newExpr.getSelectorExpression()!!) { if (this is JetCallExpression) getCalleeExpression()!! else this }
    }
}

class FqNameReplacement(val fqName: FqName): Replacement {
    [suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")]
    override fun invoke(e: JetElement): JetElement {
        val newExpr = (e.getReference() as? JetSimpleNameReference)?.bindToFqName(fqName, ShorteningMode.NO_SHORTENING) as JetElement
        return if (newExpr is JetQualifiedExpression) newExpr.getSelectorExpression()!! else newExpr
    }
}

trait OutputValue {
    val originalExpressions: List<JetExpression>
    val valueType: JetType

    class ExpressionValue(
            val callSiteReturn: Boolean,
            override val originalExpressions: List<JetExpression>,
            override val valueType: JetType
    ): OutputValue

    class Jump(
            val elementsToReplace: List<JetExpression>,
            val elementToInsertAfterCall: JetElement,
            val conditional: Boolean
    ): OutputValue {
        override val originalExpressions: List<JetExpression> get() = elementsToReplace
        override val valueType: JetType = with(KotlinBuiltIns.getInstance()) { if (conditional) getBooleanType() else getUnitType() }
    }

    class ParameterUpdate(
            val parameter: Parameter,
            override val originalExpressions: List<JetExpression>
    ): OutputValue {
        override val valueType: JetType get() = parameter.parameterType
    }

    class Initializer(
            val initializedDeclaration: JetProperty,
            override val valueType: JetType
    ): OutputValue {
        override val originalExpressions: List<JetExpression> get() = Collections.singletonList(initializedDeclaration)
    }
}

abstract class OutputValueBoxer(val outputValues: List<OutputValue>) {
    val outputValueTypes: List<JetType> get() = outputValues.map { it.valueType }

    abstract val returnType: JetType

    protected abstract fun getBoxingExpressionText(arguments: List<String>): String?

    abstract val boxingRequired: Boolean

    fun getReturnExpression(arguments: List<String>, psiFactory: JetPsiFactory): JetReturnExpression? {
        return getBoxingExpressionText(arguments)?.let { psiFactory.createReturn(it) }
    }

    protected abstract fun extractExpressionByIndex(boxedExpression: JetExpression, index: Int): JetExpression?

    protected fun extractArgumentExpressionByIndex(boxedExpression: JetExpression, index: Int): JetExpression? {
        val call: JetCallExpression? = when (boxedExpression) {
            is JetCallExpression -> boxedExpression
            is JetQualifiedExpression -> boxedExpression.getSelectorExpression() as? JetCallExpression
            else -> null
        }
        val arguments = call?.getValueArguments()
        if (arguments == null || arguments.size <= index) return null

        return arguments[index].getArgumentExpression()
    }

    fun extractExpressionByValue(boxedExpression: JetExpression, value: OutputValue): JetExpression? {
        val index = outputValues.indexOf(value)
        if (index < 0) return null

        return extractExpressionByIndex(boxedExpression, index)
    }

    abstract fun getUnboxingExpressions(boxedText: String): Map<OutputValue, String>

    class AsTuple(
            outputValues: List<OutputValue>,
            val module: ModuleDescriptor
    ) : OutputValueBoxer(outputValues) {
        {
            assert(outputValues.size <= 3, "At most 3 output values are supported")
        }

        class object {
            private val selectors = array("first", "second", "third")
        }

        override val returnType: JetType by Delegates.lazy {
            fun getType(): JetType {
                val boxingClass = when (outputValues.size) {
                    1 -> return outputValues.first().valueType
                    2 -> ResolveSessionUtils.getClassDescriptorsByFqName(module, FqName("kotlin.Pair")).first()
                    3 -> ResolveSessionUtils.getClassDescriptorsByFqName(module, FqName("kotlin.Triple")).first()
                    else -> return DEFAULT_RETURN_TYPE
                }
                return TypeUtils.substituteParameters(boxingClass, outputValueTypes)
            }

            getType()
        }

        override val boxingRequired: Boolean = outputValues.size > 1

        override fun getBoxingExpressionText(arguments: List<String>): String? {
            return when (arguments.size) {
                0 -> null
                1 -> arguments.first()
                else -> {
                    val constructorName = DescriptorUtils.getFqName(returnType.getConstructor().getDeclarationDescriptor()!!).asString()
                    return arguments.joinToString(prefix = "$constructorName(", separator = ", ", postfix = ")")
                }
            }
        }

        override fun extractExpressionByIndex(boxedExpression: JetExpression, index: Int): JetExpression? {
            if (outputValues.size() == 1) return boxedExpression
            return extractArgumentExpressionByIndex(boxedExpression, index)
        }

        override fun getUnboxingExpressions(boxedText: String): Map<OutputValue, String> {
            return when (outputValues.size) {
                0 -> Collections.emptyMap()
                1 -> Collections.singletonMap(outputValues.first(), boxedText)
                else -> {
                    var i = 0
                    ContainerUtil.newMapFromKeys(outputValues.iterator()) { "$boxedText.${selectors[i++]}" }
                }
            }
        }
    }

    class AsList(outputValues: List<OutputValue>): OutputValueBoxer(outputValues) {
        override val returnType: JetType by Delegates.lazy {
            if (outputValues.isEmpty()) DEFAULT_RETURN_TYPE
            else TypeUtils.substituteParameters(
                    KotlinBuiltIns.getInstance().getList(),
                    Collections.singletonList(CommonSupertypes.commonSupertype(outputValues.map { it.valueType }))
            )
        }

        override val boxingRequired: Boolean = outputValues.size > 0

        override fun getBoxingExpressionText(arguments: List<String>): String? {
            if (arguments.isEmpty()) return null
            return arguments.joinToString(prefix = "kotlin.listOf(", separator = ", ", postfix = ")")
        }

        override fun extractExpressionByIndex(boxedExpression: JetExpression, index: Int): JetExpression? {
            return extractArgumentExpressionByIndex(boxedExpression, index)
        }

        override fun getUnboxingExpressions(boxedText: String): Map<OutputValue, String> {
            var i = 0
            return ContainerUtil.newMapFromKeys(outputValues.iterator()) { "$boxedText[${i++}]" }
        }
    }
}

data class ControlFlow(
        val outputValues: List<OutputValue>,
        val boxerFactory: (List<OutputValue>) -> OutputValueBoxer,
        val declarationsToCopy: List<JetDeclaration>
) {
    val outputValueBoxer = boxerFactory(outputValues)

    val defaultOutputValue: ExpressionValue? = with(outputValues.filterIsInstance(javaClass<ExpressionValue>())) {
        if (size > 1) throw IllegalArgumentException("Multiple expression values: ${outputValues.joinToString()}") else firstOrNull()
    }

    val jumpOutputValue: Jump? = with(outputValues.filterIsInstance(javaClass<Jump>())) {
        when {
            isEmpty() ->
                null
            outputValues.size > size || size > 1 ->
                throw IllegalArgumentException("Jump values must be the only value if it's present: ${outputValues.joinToString()}")
            else ->
                first()
        }
    }
}

fun ControlFlow.toDefault(): ControlFlow =
        copy(outputValues = outputValues.filterNot { it is OutputValue.Jump || it is OutputValue.ExpressionValue })

data class ExtractableCodeDescriptor(
        val extractionData: ExtractionData,
        val originalContext: BindingContext,
        val name: String,
        val visibility: String,
        val parameters: List<Parameter>,
        val receiverParameter: Parameter?,
        val typeParameters: List<TypeParameter>,
        val replacementMap: Map<Int, Replacement>,
        val controlFlow: ControlFlow
)

data class ExtractionGeneratorOptions(
        val inTempFile: Boolean = false,
        val extractAsProperty: Boolean = false,
        val flexibleTypesAllowed: Boolean = false
) {
    class object {
        val DEFAULT = ExtractionGeneratorOptions()
    }
}

data class ExtractionResult(
        val declaration: JetNamedDeclaration,
        val duplicateReplacers: Map<JetPsiRange, () -> Unit>,
        val nameByOffset: Map<Int, JetElement>
)

class AnalysisResult (
        val descriptor: ExtractableCodeDescriptor?,
        val status: Status,
        val messages: List<ErrorMessage>
) {
    enum class Status {
        SUCCESS
        NON_CRITICAL_ERROR
        CRITICAL_ERROR
    }

    enum class ErrorMessage {
        NO_EXPRESSION
        NO_CONTAINER
        SUPER_CALL
        DENOTABLE_TYPES
        ERROR_TYPES
        MULTIPLE_OUTPUT
        OUTPUT_AND_EXIT_POINT
        MULTIPLE_EXIT_POINTS
        DECLARATIONS_ARE_USED_OUTSIDE
        DECLARATIONS_OUT_OF_SCOPE

        var additionalInfo: List<String>? = null

        fun addAdditionalInfo(info: List<String>): ErrorMessage {
            additionalInfo = info
            return this
        }

        fun renderMessage(): String {
            val message = JetRefactoringBundle.message(
                    when (this) {
                        NO_EXPRESSION -> "cannot.refactor.no.expression"
                        NO_CONTAINER -> "cannot.refactor.no.container"
                        SUPER_CALL -> "cannot.extract.super.call"
                        DENOTABLE_TYPES -> "parameter.types.are.not.denotable"
                        ERROR_TYPES -> "error.types.in.generated.function"
                        MULTIPLE_OUTPUT -> "selected.code.fragment.has.multiple.output.values"
                        OUTPUT_AND_EXIT_POINT -> "selected.code.fragment.has.output.values.and.exit.points"
                        MULTIPLE_EXIT_POINTS -> "selected.code.fragment.has.multiple.exit.points"
                        DECLARATIONS_ARE_USED_OUTSIDE -> "declarations.are.used.outside.of.selected.code.fragment"
                        DECLARATIONS_OUT_OF_SCOPE -> "declarations.will.move.out.of.scope"
                    }
            )

            return additionalInfo?.let { "$message\n\n${it.map { StringUtil.htmlEmphasize(it) }.joinToString("\n")}" } ?: message
        }
    }
}

class ExtractableCodeDescriptorWithConflicts(
        val descriptor: ExtractableCodeDescriptor,
        val conflicts: MultiMap<PsiElement, String>
)

fun ExtractableCodeDescriptor.canGenerateProperty(): Boolean {
    if (!parameters.empty) return false
    if (controlFlow.outputValueBoxer.returnType.isUnit()) return false

    val parent = extractionData.targetSibling.getParent()
    return parent is JetFile || parent is JetClassBody
}