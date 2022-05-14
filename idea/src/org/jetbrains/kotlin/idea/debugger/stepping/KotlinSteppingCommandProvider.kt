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

package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.impl.JvmSteppingCommandProvider
import com.intellij.psi.PsiElement
import com.intellij.xdebugger.impl.XSourcePositionImpl
import com.sun.jdi.Location
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.refactoring.getLineEndOffset
import org.jetbrains.kotlin.idea.core.refactoring.getLineNumber
import org.jetbrains.kotlin.idea.core.refactoring.getLineStartOffset
import org.jetbrains.kotlin.idea.util.DebuggerUtils
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

public class KotlinSteppingCommandProvider: JvmSteppingCommandProvider() {
    override fun getStepOverCommand(
            suspendContext: SuspendContextImpl?,
            ignoreBreakpoints: Boolean,
            stepSize: Int
    ): DebugProcessImpl.ResumeCommand? {
        if (suspendContext == null || suspendContext.isResumed) return null

        val sourcePosition = suspendContext.debugProcess.debuggerContext.sourcePosition
        return getStepOverCommand(suspendContext, ignoreBreakpoints, sourcePosition)
    }

    @TestOnly
    fun getStepOverCommand(
            suspendContext: SuspendContextImpl,
            ignoreBreakpoints: Boolean,
            debuggerContext: DebuggerContextImpl
    ): DebugProcessImpl.ResumeCommand? {
        return getStepOverCommand(suspendContext, ignoreBreakpoints, debuggerContext.sourcePosition)
    }

    fun getStepOverCommand(suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean, sourcePosition: SourcePosition): DebugProcessImpl.ResumeCommand? {
        val file = sourcePosition.file as? JetFile ?: return null
        if (sourcePosition.line < 0) return null

        val containingFunction = sourcePosition.elementAt.getParentOfType<JetNamedFunction>(false) ?: return null

        val startLineNumber = containingFunction.getLineNumber(true)
        val endLineNumber = containingFunction.getLineNumber(false)
        if (startLineNumber > endLineNumber) return null

        val linesRange = startLineNumber + 1..endLineNumber + 1

        val inlineFunctionCalls = getInlineFunctionCallsIfAny(sourcePosition)
        if (inlineFunctionCalls.isEmpty()) return null

        val inlineArguments = getInlineArgumentsIfAny(inlineFunctionCalls)

        if (inlineArguments.any { it.shouldNotUseStepOver(sourcePosition.elementAt) }) {
            return null
        }

        val additionalElementsToSkip = sourcePosition.elementAt.getAdditionalElementsToSkip()

        return DebuggerSteppingHelper.createStepOverCommand(suspendContext, ignoreBreakpoints, file, linesRange, inlineArguments, additionalElementsToSkip)
    }

    private fun PsiElement.getAdditionalElementsToSkip(): List<PsiElement> {
        val result = arrayListOf<PsiElement>()
        val ifParent = getParentOfType<JetIfExpression>(false)
        if (ifParent != null) {
            if (ifParent.then.contains(this)) {
                ifParent.elseKeyword?.let { result.add(it) }
                ifParent.`else`?.let { result.add(it) }
            }
        }
        val tryParent = getParentOfType<JetTryExpression>(false)
        if (tryParent != null) {
            val catchClause = getParentOfType<JetCatchClause>(false)
            if (catchClause != null) {
                result.addAll(tryParent.catchClauses.filter { it != catchClause })
            }
        }

        val whenEntry = getParentOfType<JetWhenEntry>(false)
        if (whenEntry != null) {
            if (whenEntry.expression.contains(this)) {
                val whenParent = whenEntry.getParentOfType<JetWhenExpression>(false)
                if (whenParent != null) {
                    result.addAll(whenParent.entries.filter { it != whenEntry })
                }
            }
        }

        return result
    }

    private fun PsiElement.shouldNotUseStepOver(elementAt: PsiElement): Boolean {
        val ifParent = getParentOfType<JetIfExpression>(false)
        if (ifParent != null) {
            // if (inlineFunCall()) {...}
            if (ifParent.condition.contains(this)) {
                return true
            }

            /*
            <caret>if (...) inlineFunCall()
                       else inlineFunCall()
             */
            val ifParentElementAt = elementAt.getParentOfType<JetIfExpression>(false)
            if (ifParentElementAt == null) {
                if (ifParent.then.contains(this)) {
                    return true
                }
                if (ifParent.`else`.contains(this)) {
                    return true
                }
            }
        }

        val tryParent = getParentOfType<JetTryExpression>(false)
        if (tryParent != null) {
            /* try { inlineFunCall() }
               catch()...
             */
            if (tryParent.tryBlock.contains(this)) {
                return true
            }
        }

        val whenEntry = getParentOfType<JetWhenEntry>(false)
        if (whenEntry != null) {
            // <caret>inlineFunCall -> ...
            if (whenEntry.conditions.any { it.contains(this) } ) {
                return true
            }

            // <caret>1 == 2 -> inlineFunCall()
            if (whenEntry.expression.contains(this)) {
                val parentEntryElementAt = elementAt.getParentOfType<JetWhenEntry>(false) ?: return true
                return parentEntryElementAt == whenEntry &&
                        whenEntry.conditions.any { it.contains(elementAt) }
            }
        }

        return false
    }

    private fun PsiElement?.contains(element: PsiElement): Boolean {
        return this?.textRange?.contains(element.textRange) ?: false
    }

    @TestOnly
    fun getStepOutCommand(suspendContext: SuspendContextImpl, debugContext: DebuggerContextImpl): DebugProcessImpl.ResumeCommand? {
        return getStepOutCommand(suspendContext, debugContext.sourcePosition)
    }

    override fun getStepOutCommand(suspendContext: SuspendContextImpl?, stepSize: Int): DebugProcessImpl.ResumeCommand? {
        if (suspendContext == null || suspendContext.isResumed) return null

        val sourcePosition = suspendContext.debugProcess.debuggerContext.sourcePosition ?: return null
        return getStepOutCommand(suspendContext, sourcePosition)
    }

    private fun getStepOutCommand(suspendContext: SuspendContextImpl, sourcePosition: SourcePosition): DebugProcessImpl.ResumeCommand? {
        val file = sourcePosition.file as? JetFile ?: return null
        if (sourcePosition.line < 0) return null

        val lineStartOffset = file.getLineStartOffset(sourcePosition.line) ?: return null

        val inlineFunctions = getInlineFunctionsIfAny(file, lineStartOffset)
        val inlinedArgument = getInlineArgumentIfAny(file, lineStartOffset)

        if (inlineFunctions.isEmpty() && inlinedArgument == null) return null

        return DebuggerSteppingHelper.createStepOutCommand(suspendContext, true, inlineFunctions, inlinedArgument)
    }

    private fun getInlineFunctionsIfAny(file: JetFile, offset: Int): List<JetNamedFunction> {
        val elementAt = file.findElementAt(offset) ?: return emptyList()
        val containingFunction = elementAt.getParentOfType<JetNamedFunction>(false) ?: return emptyList()

        val descriptor = containingFunction.resolveToDescriptor()
        if (!InlineUtil.isInline(descriptor)) return emptyList()

        val inlineFunctionsCalls = DebuggerUtils.analyzeElementWithInline(
                containingFunction.getResolutionFacade(),
                containingFunction.analyzeFully(),
                containingFunction,
                false
        ).filterIsInstance<JetNamedFunction>()

        return inlineFunctionsCalls
    }

    private fun getInlineArgumentsIfAny(inlineFunctionCalls: List<JetCallExpression>): List<JetFunction> {
        return inlineFunctionCalls.flatMap {
            it.valueArguments
                    .map { it.getArgumentExpression()  }
                    .filterIsInstance<JetFunctionLiteralExpression>()
                    .map { it.functionLiteral }
        }
    }

    private fun getInlineFunctionCallsIfAny(sourcePosition: SourcePosition): List<JetCallExpression> {
        val file = sourcePosition.file as? JetFile ?: return emptyList()
        val lineNumber = sourcePosition.line
        var elementAt = sourcePosition.elementAt

        var startOffset = file.getLineStartOffset(lineNumber) ?: elementAt.startOffset
        var endOffset = file.getLineEndOffset(lineNumber) ?: elementAt.endOffset

        var topMostElement: PsiElement? = null
        while (topMostElement !is JetElement && startOffset < endOffset) {
            elementAt = file.findElementAt(startOffset)
            if (elementAt != null) {
                topMostElement = CodeInsightUtils.getTopmostElementAtOffset(elementAt, startOffset)
            }
            startOffset++
        }

        if (topMostElement !is JetElement) return emptyList()

        val start = topMostElement.startOffset
        val end = topMostElement.endOffset

        fun isInlineCall(expr: JetExpression): Boolean {
            val context = expr.analyze(BodyResolveMode.PARTIAL)
            val resolvedCall = expr.getResolvedCall(context) ?: return false
            return InlineUtil.isInline(resolvedCall.resultingDescriptor)
        }

        val allInlineFunctionCalls = CodeInsightUtils.
                findElementsOfClassInRange(file, start, end, JetExpression::class.java)
                .map { JetPsiUtil.getParentCallIfPresent(it as JetExpression) }
                .filterIsInstance<JetCallExpression>()
                .filter { isInlineCall(it) }
                .toSet()

        // It is necessary to check range because of multiline assign
        var linesRange = lineNumber..lineNumber
        return allInlineFunctionCalls.filter {
            val shouldInclude = it.getLineNumber() in linesRange
            if (shouldInclude) {
                linesRange = Math.min(linesRange.start, it.getLineNumber())..Math.max(linesRange.end, it.getLineNumber(false))
            }
            shouldInclude
        }
    }
}

fun getStepOutPosition(
        location: Location,
        file: JetFile,
        range: Range<Int>,
        inlinedArguments: List<JetElement>,
        elementsToSkip: List<PsiElement>
): XSourcePositionImpl? {
    val computedReferenceType = location.declaringType() ?: return null

    val locations = computedReferenceType.allLineLocations()
            .dropWhile { it != location }
            .drop(1)
            .filter { it.method() == location.method() && it.lineNumber() in range }
            .dropWhile { it.lineNumber() == location.lineNumber() }

    for (locationAtLine in locations) {
        val lineNumber = locationAtLine.lineNumber()
        val lineStartOffset = file.getLineStartOffset(lineNumber - 1) ?: continue
        if (inlinedArguments.any { it.textRange.contains(lineStartOffset) }) continue
        if (elementsToSkip.any { it.textRange.contains(lineStartOffset) }) continue

        val elementAt = file.findElementAt(lineStartOffset) ?: continue

        return XSourcePositionImpl.createByElement(elementAt) ?: return null
    }

    return null
}

fun getStepOverPosition(
        location: Location,
        suspendContext: SuspendContextImpl,
        inlineFunctions: List<JetNamedFunction>,
        inlinedArgument: JetFunctionLiteral?
): XSourcePositionImpl? {
    val computedReferenceType = location.declaringType() ?: return null

    val locations = computedReferenceType.allLineLocations()
    val nextLineLocations = locations.dropWhile { it.lineNumber() != location.lineNumber() }.filter { it.method() == location.method() }

    if (inlineFunctions.isNotEmpty()) {
        return suspendContext.getXPositionForStepOutFromInlineFunction(nextLineLocations, inlineFunctions) ?: return null
    }

    if (inlinedArgument != null) {
        return suspendContext.getXPositionForStepOutFromInlinedArgument(nextLineLocations, inlinedArgument) ?: return null
    }

    return null
}

private fun SuspendContextImpl.getXPositionForStepOutFromInlineFunction(
        locations: List<Location>,
        inlineFunctionsToSkip: List<JetNamedFunction>
): XSourcePositionImpl? {
    return getNextPositionWithFilter(locations) {
        file, offset ->
        if (inlineFunctionsToSkip.any { it.textRange.contains(offset) }) {
            return@getNextPositionWithFilter true
        }

        val inlinedArgument = getInlineArgumentIfAny(file, offset)
        inlinedArgument != null && inlinedArgument.textRange.contains(offset)
    }
}

private fun SuspendContextImpl.getXPositionForStepOutFromInlinedArgument(
        locations: List<Location>,
        inlinedArgumentToSkip: JetFunctionLiteral
): XSourcePositionImpl? {
    return getNextPositionWithFilter(locations) {
        file, offset ->
        inlinedArgumentToSkip.textRange.contains(offset)
    }
}

private fun SuspendContextImpl.getNextPositionWithFilter(
        locations: List<Location>,
        skip: (JetFile, Int) -> Boolean
): XSourcePositionImpl? {
    for (location in locations) {
        val file = this.debugProcess.positionManager.getSourcePosition(location)?.file as? JetFile ?: continue
        val currentLine = location.lineNumber() - 1
        val lineStartOffset = file.getLineStartOffset(currentLine) ?: continue
        if (skip(file, lineStartOffset)) continue

        val elementAt = file.findElementAt(lineStartOffset) ?: continue
        return XSourcePositionImpl.createByElement(elementAt)
    }

    return null
}

private fun getInlineArgumentIfAny(file: JetFile, offset: Int): JetFunctionLiteral? {
    val elementAt = file.findElementAt(offset) ?: return null
    val functionLiteralExpression = elementAt.getParentOfType<JetFunctionLiteralExpression>(false) ?: return null

    val context = functionLiteralExpression.analyze(BodyResolveMode.PARTIAL)
    if (!InlineUtil.isInlinedArgument(functionLiteralExpression.functionLiteral, context, false)) return null

    return functionLiteralExpression.functionLiteral
}
