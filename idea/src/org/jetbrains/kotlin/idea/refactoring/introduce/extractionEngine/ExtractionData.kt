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

package org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.codeInsight.JetFileReferencesResolver
import org.jetbrains.kotlin.idea.core.compareDescriptors
import org.jetbrains.kotlin.idea.core.refactoring.getContextForContainingDeclarationBody
import org.jetbrains.kotlin.idea.util.psi.patternMatching.JetPsiRange
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isInsideOf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getImplicitReceiverValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.isSynthesizedInvoke
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.JetType
import java.util.*

data class ExtractionOptions(
        val inferUnitTypeForUnusedValues: Boolean = true,
        val enableListBoxing: Boolean = false,
        val extractAsProperty: Boolean = false,
        val allowSpecialClassNames: Boolean = false,
        val captureLocalFunctions: Boolean = false
) {
    companion object {
        val DEFAULT = ExtractionOptions()
    }
}

data class ResolveResult(
        val originalRefExpr: JetSimpleNameExpression,
        val declaration: PsiNameIdentifierOwner,
        val descriptor: DeclarationDescriptor,
        val resolvedCall: ResolvedCall<*>?
)

data class ResolvedReferenceInfo(
        val refExpr: JetSimpleNameExpression,
        val offsetInBody: Int,
        val resolveResult: ResolveResult,
        val smartCast: JetType?,
        val possibleTypes: Set<JetType>
)

data class ExtractionData(
        val originalFile: JetFile,
        val originalRange: JetPsiRange,
        val targetSibling: PsiElement,
        val duplicateContainer: PsiElement? = null,
        val options: ExtractionOptions = ExtractionOptions.DEFAULT
) {
    val project: Project = originalFile.getProject()
    val originalElements: List<PsiElement> = originalRange.elements

    val insertBefore: Boolean = options.extractAsProperty
                                || targetSibling.getStrictParentOfType<JetDeclaration>()?.let {
                                    it is JetDeclarationWithBody || it is JetClassInitializer
                                } ?: false

    fun getExpressions(): List<JetExpression> = originalElements.filterIsInstance<JetExpression>()

    private fun getCodeFragmentTextRange(): TextRange? {
        val originalElements = originalElements
        return when (originalElements.size()) {
            0 -> null
            1 -> originalElements.first().getTextRange()
            else -> {
                val from = originalElements.first().getTextRange()!!.getStartOffset()
                val to = originalElements.last().getTextRange()!!.getEndOffset()
                TextRange(from, to)
            }
        }
    }

    val codeFragmentText: String by lazy {
        getCodeFragmentTextRange()?.let { originalFile.getText()?.substring(it.getStartOffset(), it.getEndOffset()) } ?: ""
    }

    val originalStartOffset: Int?
        get() = originalElements.firstOrNull()?.let { e -> e.getTextRange()!!.getStartOffset() }

    val commonParent = PsiTreeUtil.findCommonParent(originalElements) as JetElement

    val bindingContext: BindingContext? by lazy { commonParent.getContextForContainingDeclarationBody() }

    private val itFakeDeclaration by lazy { JetPsiFactory(originalFile).createParameter("it: Any?") }
    private val synthesizedInvokeDeclaration by lazy { JetPsiFactory(originalFile).createFunction("fun invoke() {}") }

    val refOffsetToDeclaration by lazy {
        fun isExtractableIt(descriptor: DeclarationDescriptor, context: BindingContext): Boolean {
            if (!(descriptor is ValueParameterDescriptor && (context[BindingContext.AUTO_CREATED_IT, descriptor] ?: false))) return false
            val function = DescriptorToSourceUtils.descriptorToDeclaration(descriptor.getContainingDeclaration()) as? JetFunctionLiteral
            return function == null || !function.isInsideOf(originalElements)
        }

        fun getSyntheticPropertyAccessorDeclarationIfAny(descriptor: DeclarationDescriptor): PsiNameIdentifierOwner? {
            return (descriptor as? SyntheticJavaPropertyDescriptor ?: return null).getMethod.source.getPsi() as? PsiNameIdentifierOwner
        }

        fun getDeclaration(descriptor: DeclarationDescriptor, context: BindingContext): PsiNameIdentifierOwner? {
            (DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor) as? PsiNameIdentifierOwner)?.let { return it }

            return when {
                isExtractableIt(descriptor, context) -> itFakeDeclaration
                isSynthesizedInvoke(descriptor) -> synthesizedInvokeDeclaration
                else -> getSyntheticPropertyAccessorDeclarationIfAny(descriptor)
            }
        }

        val originalStartOffset = originalStartOffset
        val context = bindingContext

        if (originalStartOffset != null && context != null) {
            val resultMap = HashMap<Int, ResolveResult>()

            val visitor = object: JetTreeVisitorVoid() {
                override fun visitQualifiedExpression(expression: JetQualifiedExpression) {
                    if (context[BindingContext.SMARTCAST, expression] != null) {
                        expression.getSelectorExpression()?.accept(this)
                        return
                    }

                    super.visitQualifiedExpression(expression)
                }

                override fun visitSimpleNameExpression(ref: JetSimpleNameExpression) {
                    if (ref !is JetSimpleNameExpression) return
                    if (ref.getParent() is JetValueArgumentName) return

                    val resolvedCall = ref.getResolvedCall(context)
                    val descriptor = context[BindingContext.REFERENCE_TARGET, ref] ?: return
                    val declaration = getDeclaration(descriptor, context) ?: return

                    val offset = ref.getTextRange()!!.getStartOffset() - originalStartOffset
                    resultMap[offset] = ResolveResult(ref, declaration, descriptor, resolvedCall)
                }
            }
            getExpressions().forEach { it.accept(visitor) }

            resultMap
        }
        else Collections.emptyMap<Int, ResolveResult>()
    }

    fun getPossibleTypes(expression: JetExpression, resolvedCall: ResolvedCall<*>?, context: BindingContext): Set<JetType> {
        val typeInfo = context[BindingContext.EXPRESSION_TYPE_INFO, expression] ?: return emptySet()

        (resolvedCall?.getImplicitReceiverValue() as? ThisReceiver)?.let {
            return typeInfo.dataFlowInfo.getPossibleTypes(DataFlowValueFactory.createDataFlowValue(it))
        }

        val type = resolvedCall?.resultingDescriptor?.returnType ?: return emptySet()
        val containingDescriptor = context[BindingContext.LEXICAL_SCOPE, expression]?.ownerDescriptor ?: return emptySet()
        val dataFlowValue = DataFlowValueFactory.createDataFlowValue(expression, type, context, containingDescriptor)
        return typeInfo.dataFlowInfo.getPossibleTypes(dataFlowValue)
    }

    fun getBrokenReferencesInfo(body: JetBlockExpression): List<ResolvedReferenceInfo> {
        val originalContext = bindingContext ?: return listOf()

        val startOffset = body.getBlockContentOffset()

        val referencesInfo = ArrayList<ResolvedReferenceInfo>()
        val refToContextMap = JetFileReferencesResolver.resolve(body)
        for ((ref, context) in refToContextMap) {
            if (ref !is JetSimpleNameExpression) continue

            val offset = ref.getTextRange()!!.getStartOffset() - startOffset
            val originalResolveResult = refOffsetToDeclaration[offset] ?: continue

            val smartCast: JetType?
            val possibleTypes: Set<JetType>

            // Qualified property reference: a.b
            val qualifiedExpression = ref.getQualifiedExpressionForSelector()
            if (qualifiedExpression != null) {
                val smartCastTarget = originalResolveResult.originalRefExpr.getParent() as JetExpression
                smartCast = originalContext[BindingContext.SMARTCAST, smartCastTarget]
                possibleTypes = getPossibleTypes(smartCastTarget, originalResolveResult.resolvedCall, originalContext)
                val receiverDescriptor =
                        (originalResolveResult.resolvedCall?.getDispatchReceiver() as? ThisReceiver)?.getDeclarationDescriptor()
                if (smartCast == null
                    && !DescriptorUtils.isCompanionObject(receiverDescriptor)
                    && qualifiedExpression.getReceiverExpression() !is JetSuperExpression) continue
            }
            else {
                smartCast = originalContext[BindingContext.SMARTCAST, originalResolveResult.originalRefExpr]
                possibleTypes = getPossibleTypes(originalResolveResult.originalRefExpr, originalResolveResult.resolvedCall, originalContext)
            }

            val parent = ref.getParent()

            // Skip P in type references like 'P.Q'
            if (parent is JetUserType && (parent.getParent() as? JetUserType)?.getQualifier() == parent) continue

            val descriptor = context[BindingContext.REFERENCE_TARGET, ref]
            val isBadRef = !(compareDescriptors(project, originalResolveResult.descriptor, descriptor)
                             && originalContext.diagnostics.forElement(originalResolveResult.originalRefExpr) == context.diagnostics.forElement(ref))
                           || smartCast != null
            if (isBadRef && !originalResolveResult.declaration.isInsideOf(originalElements)) {
                val originalResolvedCall = originalResolveResult.resolvedCall as? VariableAsFunctionResolvedCall
                val originalFunctionCall = originalResolvedCall?.functionCall
                val originalVariableCall = originalResolvedCall?.variableCall
                val invokeDescriptor = originalFunctionCall?.getResultingDescriptor()
                if (invokeDescriptor != null && isSynthesizedInvoke(invokeDescriptor) && invokeDescriptor.isExtension) {
                    val variableResolveResult = originalResolveResult.copy(resolvedCall = originalVariableCall!!,
                                                                           descriptor = originalVariableCall.getResultingDescriptor())
                    val functionResolveResult = originalResolveResult.copy(resolvedCall = originalFunctionCall!!,
                                                                           descriptor = originalFunctionCall.getResultingDescriptor(),
                                                                           declaration = synthesizedInvokeDeclaration)
                    referencesInfo.add(ResolvedReferenceInfo(ref, offset, variableResolveResult, smartCast, possibleTypes))
                    referencesInfo.add(ResolvedReferenceInfo(ref, offset, functionResolveResult, smartCast, possibleTypes))
                }
                else {
                    referencesInfo.add(ResolvedReferenceInfo(ref, offset, originalResolveResult, smartCast, possibleTypes))
                }
            }
        }

        return referencesInfo
    }
}

// Hack:
// we can't get first element offset through getStatement()/getChildren() since they skip comments and whitespaces
// So we take offset of the left brace instead and increase it by 2 (which is length of "{\n" separating block start and its first element)
private fun JetExpression.getBlockContentOffset(): Int {
    (this as? JetBlockExpression)?.getLBrace()?.let {
        return it.getTextRange()!!.getStartOffset() + 2
    }
    return getTextRange()!!.getStartOffset()
}
