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

import org.jetbrains.jet.renderer.DescriptorRenderer
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import java.util.HashMap
import org.jetbrains.jet.lang.psi.JetTreeVisitorVoid
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.lang.psi.JetExpression
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.JetPsiFactory.CallableBuilder
import org.jetbrains.jet.lang.psi.JetPsiFactory.CallableBuilder.Target
import org.jetbrains.jet.lang.psi.JetNamedDeclaration
import org.jetbrains.jet.lang.psi.JetPsiFactory
import java.util.LinkedHashMap
import java.util.Collections
import org.jetbrains.jet.lang.psi.psiUtil.isFunctionLiteralOutsideParentheses
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.JetFunctionLiteralArgument
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.plugin.util.psiModificationUtil.moveInsideParenthesesAndReplaceWith
import org.jetbrains.jet.lang.psi.psiUtil.prependElement
import org.jetbrains.jet.lang.psi.psiUtil.appendElement
import org.jetbrains.jet.lang.psi.psiUtil.replaced
import org.jetbrains.jet.plugin.intentions.declarations.DeclarationUtils
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.jetbrains.jet.plugin.refactoring.extractFunction.OutputValue.ParameterUpdate
import org.jetbrains.jet.plugin.refactoring.extractFunction.OutputValue.Jump
import org.jetbrains.jet.plugin.refactoring.extractFunction.OutputValue.Initializer
import org.jetbrains.jet.plugin.refactoring.extractFunction.OutputValue.ExpressionValue
import org.jetbrains.jet.lang.psi.JetReturnExpression
import org.jetbrains.jet.plugin.refactoring.JetNameValidatorImpl
import org.jetbrains.jet.plugin.refactoring.JetNameSuggester
import org.jetbrains.jet.plugin.refactoring.isMultiLine
import org.jetbrains.jet.plugin.refactoring.extractFunction.OutputValueBoxer.AsTuple
import org.jetbrains.jet.plugin.util.psi.patternMatching.JetPsiUnifier
import org.jetbrains.jet.plugin.util.psi.patternMatching.UnifierParameter
import org.jetbrains.jet.plugin.util.psi.patternMatching.toRange
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences
import org.jetbrains.jet.plugin.util.psi.patternMatching.JetPsiRange
import org.jetbrains.jet.plugin.util.psi.patternMatching.UnificationResult
import org.jetbrains.jet.plugin.util.psi.patternMatching.JetPsiRange.Match
import org.jetbrains.jet.plugin.util.psi.patternMatching.UnificationResult.Status
import org.jetbrains.jet.plugin.util.psi.patternMatching.UnificationResult.WeaklyMatched
import org.jetbrains.jet.plugin.util.psi.patternMatching.UnificationResult.StronglyMatched

fun ExtractableCodeDescriptor.getDeclarationText(
        options: ExtractionGeneratorOptions = ExtractionGeneratorOptions.DEFAULT,
        withBody: Boolean = true,
        descriptorRenderer: DescriptorRenderer = DescriptorRenderer.FQ_NAMES_IN_TYPES
): String {
    if (!canGenerateProperty() && options.extractAsProperty) {
        throw IllegalArgumentException("Can't generate property: ${extractionData.getCodeFragmentText()}")
    }

    val builderTarget = if (options.extractAsProperty) Target.READ_ONLY_PROPERTY else Target.FUNCTION
    return CallableBuilder(builderTarget).let { builder ->
        builder.modifier(visibility)

        builder.typeParams(typeParameters.map { it.originalDeclaration.getText()!! })

        receiverParameter?.let { builder.receiver(descriptorRenderer.renderType(it.parameterType)) }

        builder.name(name)

        parameters.forEach { parameter ->
            builder.param(parameter.name, descriptorRenderer.renderType(parameter.parameterType))
        }

        with(controlFlow.outputValueBoxer.returnType) {
            if (isDefault() || isError()) builder.noReturnType() else builder.returnType(descriptorRenderer.renderType(this))
        }

        builder.typeConstraints(typeParameters.flatMap { it.originalConstraints }.map { it.getText()!! })

        if (withBody) {
            builder.blockBody(extractionData.getCodeFragmentText())
        }

        builder.asString()
    }
}

fun createNameCounterpartMap(from: JetElement, to: JetElement): Map<JetSimpleNameExpression, JetSimpleNameExpression> {
    val map = HashMap<JetSimpleNameExpression, JetSimpleNameExpression>()

    val fromOffset = from.getTextRange()!!.getStartOffset()
    from.accept(
            object: JetTreeVisitorVoid() {
                override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                    val offset = expression.getTextRange()!!.getStartOffset() - fromOffset
                    val newExpression = to.findElementAt(offset)?.getParentByType(javaClass<JetSimpleNameExpression>())
                    assert(newExpression!= null, "Couldn't find expression at $offset in '${to.getText()}'")

                    map[expression] = newExpression!!
                }
            }
    )

    return map
}

class DuplicateInfo(
        val range: JetPsiRange,
        val controlFlow: ControlFlow,
        val arguments: List<String>
)

fun ExtractableCodeDescriptor.findDuplicates(): List<DuplicateInfo> {
    fun processWeakMatch(match: Match, newControlFlow: ControlFlow): Boolean {
        val valueCount = controlFlow.outputValues.size

        val weakMatches = HashMap((match.result as WeaklyMatched).weakMatches)
        val currentValuesToNew = HashMap<OutputValue, OutputValue>()

        fun matchValues(currentValue: OutputValue, newValue: OutputValue): Boolean {
            if ((currentValue is Jump) != (newValue is Jump)) return false
            if (currentValue.originalExpressions.zip(newValue.originalExpressions).all { weakMatches[it.first] == it.second }) {
                currentValuesToNew[currentValue] = newValue
                weakMatches.keySet().removeAll(currentValue.originalExpressions)
                return true
            }
            return false
        }

        if (valueCount == 1) {
            matchValues(controlFlow.outputValues.first(), newControlFlow.outputValues.first())
        } else {
            @outer
            for (currentValue in controlFlow.outputValues)
                for (newValue in newControlFlow.outputValues) {
                    if ((currentValue is ExpressionValue) != (newValue is ExpressionValue)) continue
                    if (matchValues(currentValue, newValue)) continue @outer
                }
        }

        return currentValuesToNew.size == valueCount && weakMatches.isEmpty()
    }

    fun getControlFlowIfMatched(match: Match): ControlFlow? {
        val analysisResult = extractionData.copy(originalRange = match.range).performAnalysis()
        if (analysisResult.status != AnalysisResult.Status.SUCCESS) return null

        val newControlFlow = analysisResult.descriptor!!.controlFlow
        if (newControlFlow.outputValues.isEmpty()) return newControlFlow
        if (controlFlow.outputValues.size != newControlFlow.outputValues.size) return null

        val matched = when (match.result) {
            is StronglyMatched -> true
            is WeaklyMatched -> processWeakMatch(match, newControlFlow)
            else -> throw AssertionError("Unexpected unification result: ${match.result}")
        }

        return if (matched) newControlFlow else null
    }

    val unifierParameters = parameters.map { UnifierParameter(it.originalDescriptor, it.parameterType) }

    val unifier = JetPsiUnifier(unifierParameters, true)

    val scopeElement = extractionData.targetSibling.getParent() ?: return Collections.emptyList()
    val originalTextRange = extractionData.originalRange.getTextRange()
    return extractionData
            .originalRange
            .match(scopeElement, unifier)
            .stream()
            .filter { !(it.range.getTextRange() intersects originalTextRange) }
            .map { match ->
                val controlFlow = getControlFlowIfMatched(match)
                controlFlow?.let { DuplicateInfo(match.range, it, unifierParameters.map { match.result.substitution[it]!!.getText()!! }) }
            }
            .filterNotNull()
            .toList()
}

private fun makeCall(
        extractableDescriptor: ExtractableCodeDescriptor,
        declaration: JetNamedDeclaration,
        controlFlow: ControlFlow,
        rangeToReplace: JetPsiRange,
        arguments: List<String>) {
    fun insertCall(anchor: PsiElement, wrappedCall: JetExpression) {
        val firstExpression = rangeToReplace.elements.firstOrNull { it is JetExpression } as? JetExpression
        if (firstExpression?.isFunctionLiteralOutsideParentheses() ?: false) {
            val functionLiteralArgument = PsiTreeUtil.getParentOfType(firstExpression, javaClass<JetFunctionLiteralArgument>())!!
            functionLiteralArgument.moveInsideParenthesesAndReplaceWith(wrappedCall, extractableDescriptor.originalContext)
            return
        }
        anchor.replace(wrappedCall)
    }

    if (rangeToReplace !is JetPsiRange.ListRange) return

    val anchor = rangeToReplace.startElement
    val anchorParent = anchor.getParent()!!

    anchor.getNextSibling()?.let { from ->
        val to = rangeToReplace.endElement
        if (to != anchor) {
            anchorParent.deleteChildRange(from, to);
        }
    }

    val callText = when (declaration) {
        is JetNamedFunction ->
            arguments.joinToString(separator = ", ", prefix = "${extractableDescriptor.name}(", postfix = ")")
        else -> extractableDescriptor.name
    }

    val anchorInBlock = stream(anchor) { it.getParent() }.firstOrNull { it.getParent() is JetBlockExpression }
    val block = (anchorInBlock?.getParent() as? JetBlockExpression) ?: anchorParent

    val psiFactory = JetPsiFactory(anchor.getProject())
    val newLine = psiFactory.createNewLine()

    if (controlFlow.outputValueBoxer is AsTuple && controlFlow.outputValues.size > 1 && controlFlow.outputValues.all { it is Initializer }) {
        val declarationsToMerge = controlFlow.outputValues.map { (it as Initializer).initializedDeclaration }
        val isVar = declarationsToMerge.first().isVar()
        if (declarationsToMerge.all { it.isVar() == isVar }) {
            controlFlow.declarationsToCopy.subtract(declarationsToMerge).forEach {
                block.addBefore(psiFactory.createDeclaration<JetDeclaration>(it.getText()!!), anchorInBlock) as JetDeclaration
                block.addBefore(newLine, anchorInBlock)
            }

            val entries = declarationsToMerge.map { p -> p.getName() + (p.getTypeRef()?.let { ": ${it.getText()}" } ?: "") }
            anchorInBlock?.replace(
                    psiFactory.createDeclaration("${if (isVar) "var" else "val"} (${entries.joinToString()}) = $callText")
            )

            return
        }
    }

    val inlinableCall = controlFlow.outputValues.size <= 1
    val unboxingExpressions =
            if (inlinableCall) {
                controlFlow.outputValueBoxer.getUnboxingExpressions(callText)
            }
            else {
                val varNameValidator = JetNameValidatorImpl(block, anchorInBlock, JetNameValidatorImpl.Target.PROPERTIES)
                val resultVal = JetNameSuggester.suggestNames(controlFlow.outputValueBoxer.returnType, varNameValidator, null).first()
                block.addBefore(psiFactory.createDeclaration("val $resultVal = $callText"), anchorInBlock)
                block.addBefore(newLine, anchorInBlock)
                controlFlow.outputValueBoxer.getUnboxingExpressions(resultVal)
            }

    val copiedDeclarations = HashMap<JetDeclaration, JetDeclaration>()
    for (decl in controlFlow.declarationsToCopy) {
        val declCopy = psiFactory.createDeclaration<JetDeclaration>(decl.getText()!!)
        copiedDeclarations[decl] = block.addBefore(declCopy, anchorInBlock) as JetDeclaration
        block.addBefore(newLine, anchorInBlock)
    }

    if (controlFlow.outputValues.isEmpty()) {
        anchor.replace(psiFactory.createExpression(callText))
        return
    }

    fun wrapCall(outputValue: OutputValue, callText: String): List<PsiElement> {
        return when (outputValue) {
            is OutputValue.ExpressionValue ->
                Collections.singletonList(
                        if (outputValue.callSiteReturn) psiFactory.createReturn(callText) else psiFactory.createExpression(callText)
                )

            is ParameterUpdate ->
                Collections.singletonList(
                        psiFactory.createExpression("${outputValue.parameter.argumentText} = $callText")
                )

            is Jump -> {
                if (outputValue.conditional) {
                    Collections.singletonList(
                            psiFactory.createExpression("if ($callText) ${outputValue.elementToInsertAfterCall.getText()}")
                    )
                }
                else {
                    listOf(
                            psiFactory.createExpression(callText),
                            newLine,
                            psiFactory.createExpression(outputValue.elementToInsertAfterCall.getText()!!)
                    )
                }
            }

            is Initializer -> {
                val newProperty = copiedDeclarations[outputValue.initializedDeclaration] as JetProperty
                newProperty.replace(DeclarationUtils.changePropertyInitializer(newProperty, psiFactory.createExpression(callText)))
                Collections.emptyList()
            }

            else -> throw IllegalArgumentException("Unknown output value: $outputValue")
        }
    }

    val defaultValue = controlFlow.defaultOutputValue

    controlFlow.outputValues
            .filter { it != defaultValue }
            .flatMap { wrapCall(it, unboxingExpressions[it]!!) }
            .withIndices()
            .forEach {
                val (i, e) = it

                if (i > 0) {
                    block.addBefore(newLine, anchorInBlock)
                }
                block.addBefore(e, anchorInBlock)
            }

    defaultValue?.let {
        if (!inlinableCall) {
            block.addBefore(newLine, anchorInBlock)
        }
        insertCall(anchor, wrapCall(it, unboxingExpressions[it]!!).first() as JetExpression)
    }

    if (anchor.isValid()) {
        anchor.delete()
    }
}

fun ExtractableCodeDescriptor.generateDeclaration(options: ExtractionGeneratorOptions): ExtractionResult{
    val psiFactory = JetPsiFactory(extractionData.originalFile)
    val nameByOffset = HashMap<Int, JetElement>()

    fun createDeclaration(): JetNamedDeclaration {
        return with(extractionData) {
            if (options.inTempFile) {
                createTemporaryDeclaration("${getDeclarationText()}\n")
            }
            else {
                psiFactory.createDeclaration(getDeclarationText(options))
            }
        }
    }

    fun getReturnArguments(resultExpression: JetExpression?): List<String> {
        return controlFlow.outputValues
                .map {
                    when (it) {
                        is ExpressionValue -> resultExpression?.getText()
                        is Jump -> if (it.conditional) "false" else null
                        is ParameterUpdate -> it.parameter.nameForRef
                        is Initializer -> it.initializedDeclaration.getName()
                        else -> throw IllegalArgumentException("Unknown output value: $it")
                    }
                }
                .filterNotNull()
    }

    fun replaceWithReturn(
            originalExpression: JetExpression,
            replacingExpression: JetReturnExpression,
            expressionToUnifyWith: JetExpression?
    ) {
        val currentResultExpression =
                if (originalExpression is JetReturnExpression) originalExpression.getReturnedExpression() else originalExpression
        if (currentResultExpression == null) return

        val newResultExpression = controlFlow.defaultOutputValue?.let {
            val boxedExpression = originalExpression.replaced(replacingExpression).getReturnedExpression()!!
            controlFlow.outputValueBoxer.extractExpressionByValue(boxedExpression, it)
        }
        if (newResultExpression == null) {
            throw AssertionError("Can' replace '${originalExpression.getText()}' with '${replacingExpression.getText()}'")
        }

        val counterpartMap = createNameCounterpartMap(currentResultExpression, expressionToUnifyWith ?: newResultExpression)
        nameByOffset.entrySet().forEach { e -> counterpartMap[e.getValue()]?.let { e.setValue(it) } }
    }

    fun adjustDeclarationBody(declaration: JetNamedDeclaration) {
        val body = declaration.getGeneratedBlockBody()

        val exprReplacementMap = HashMap<JetElement, (JetElement) -> JetElement>()
        val originalOffsetByExpr = LinkedHashMap<JetElement, Int>()

        val bodyOffset = body.getBlockContentOffset()
        val file = body.getContainingFile()!!

        /*
         * Sort by descending position so that internals of value/type arguments in calls and qualified types are replaced
         * before calls/types themselves
         */
        for ((offsetInBody, resolveResult) in extractionData.refOffsetToDeclaration.entrySet().sortDescendingBy { it.key }) {
            val expr = file.findElementAt(bodyOffset + offsetInBody)?.getParentByType(javaClass<JetSimpleNameExpression>())
            assert(expr != null, "Couldn't find expression at $offsetInBody in '${body.getText()}'")

            originalOffsetByExpr[expr!!] = offsetInBody

            replacementMap[offsetInBody]?.let { replacement ->
                if (replacement !is ParameterReplacement || replacement.parameter != receiverParameter) {
                    exprReplacementMap[expr] = replacement
                }
            }
        }

        val replacingReturn: JetExpression?
        val expressionsToReplaceWithReturn: List<JetElement>

        val jumpValue = controlFlow.jumpOutputValue
        if (jumpValue != null) {
            replacingReturn = psiFactory.createExpression(if (jumpValue.conditional) "return true" else "return")
            expressionsToReplaceWithReturn = jumpValue.elementsToReplace.map { jumpElement ->
                val offsetInBody = jumpElement.getTextRange()!!.getStartOffset() - extractionData.originalStartOffset!!
                val expr = file.findElementAt(bodyOffset + offsetInBody)?.getParentByType(jumpElement.javaClass)
                assert(expr != null, "Couldn't find expression at $offsetInBody in '${body.getText()}'")

                expr!!
            }
        }
        else {
            replacingReturn = null
            expressionsToReplaceWithReturn = Collections.emptyList()
        }

        if (replacingReturn != null) {
            for (expr in expressionsToReplaceWithReturn) {
                expr.replace(replacingReturn)
            }
        }

        for ((expr, originalOffset) in originalOffsetByExpr) {
            if (expr.isValid()) {
                nameByOffset.put(originalOffset, exprReplacementMap[expr]?.invoke(expr) ?: expr)
            }
        }

        val firstExpression = body.getStatements().firstOrNull()
        if (firstExpression != null) {
            for (param in parameters) {
                param.mirrorVarName?.let { varName ->
                    body.addBefore(psiFactory.createProperty(varName, null, true, param.name), firstExpression)
                    body.addBefore(psiFactory.createNewLine(), firstExpression)
                }
            }
        }

        val defaultValue = controlFlow.defaultOutputValue

        val lastExpression = body.getStatements().lastOrNull() as? JetExpression
        if (lastExpression is JetReturnExpression) return

        val (defaultExpression, expressionToUnifyWith) =
                if (!options.inTempFile && defaultValue != null && controlFlow.outputValueBoxer.boxingRequired && lastExpression!!.isMultiLine()) {
                    val varNameValidator = JetNameValidatorImpl(body, lastExpression, JetNameValidatorImpl.Target.PROPERTIES)
                    val resultVal = JetNameSuggester.suggestNames(defaultValue.valueType, varNameValidator, null).first()
                    val newDecl = body.addBefore(psiFactory.createDeclaration("val $resultVal = ${lastExpression!!.getText()}"), lastExpression) as JetProperty
                    body.addBefore(psiFactory.createNewLine(), lastExpression)
                    psiFactory.createExpression(resultVal) to newDecl.getInitializer()!!
                }
                else {
                    lastExpression to null
                }

        val returnExpression = controlFlow.outputValueBoxer.getReturnExpression(getReturnArguments(defaultExpression), psiFactory)
        if (returnExpression == null) return

        when {
            defaultValue == null ->
                body.appendElement(returnExpression)

            !defaultValue.callSiteReturn ->
                replaceWithReturn(lastExpression!!, returnExpression, expressionToUnifyWith)
        }
    }

    fun insertDeclaration(declaration: JetNamedDeclaration): JetNamedDeclaration {
        return with(extractionData) {
            val targetContainer = targetSibling.getParent()!!
            val emptyLines = psiFactory.createWhiteSpace("\n\n")
            if (insertBefore) {
                val declarationInFile = targetContainer.addBefore(declaration, targetSibling) as JetNamedDeclaration
                targetContainer.addBefore(emptyLines, targetSibling)

                declarationInFile
            }
            else {
                val declarationInFile = targetContainer.addAfter(declaration, targetSibling) as JetNamedDeclaration
                targetContainer.addAfter(emptyLines, targetSibling)

                declarationInFile
            }
        }
    }

    val duplicates = if (options.inTempFile) Collections.emptyList() else findDuplicates()

    val declaration = createDeclaration().let { if (options.inTempFile) it else insertDeclaration(it) }
    adjustDeclarationBody(declaration)

    if (options.inTempFile) return ExtractionResult(declaration, Collections.emptyMap(), nameByOffset)

    makeCall(this, declaration, controlFlow, extractionData.originalRange, parameters.map { it.argumentText })
    ShortenReferences.process(declaration)    

    val duplicateReplacers = duplicates.map { it.range to { makeCall(this, declaration, it.controlFlow, it.range, it.arguments) } }.toMap()
    return ExtractionResult(declaration, duplicateReplacers, nameByOffset)
}