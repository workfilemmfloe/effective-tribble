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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.codeInsight.KotlinFileReferencesResolver
import org.jetbrains.kotlin.idea.core.compareDescriptors
import org.jetbrains.kotlin.idea.core.refactoring.getContextForContainingDeclarationBody
import org.jetbrains.kotlin.idea.refactoring.introduce.ExtractableSubstringInfo
import org.jetbrains.kotlin.idea.refactoring.introduce.extractableSubstringInfo
import org.jetbrains.kotlin.idea.refactoring.introduce.substringContextOrThis
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiRange
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getImplicitReceiverValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.isSynthesizedInvoke
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

data class ExtractionOptions(
        val inferUnitTypeForUnusedValues: Boolean = true,
        val enableListBoxing: Boolean = false,
        val extractAsProperty: Boolean = false,
        val allowSpecialClassNames: Boolean = false,
        val captureLocalFunctions: Boolean = false,
        val canWrapInWith: Boolean = false
) {
    companion object {
        val DEFAULT = ExtractionOptions()
    }
}

data class ResolveResult(
        val originalRefExpr: KtSimpleNameExpression,
        val declaration: PsiNameIdentifierOwner,
        val descriptor: DeclarationDescriptor,
        val resolvedCall: ResolvedCall<*>?
)

data class ResolvedReferenceInfo(
        val refExpr: KtSimpleNameExpression,
        val offsetInBody: Int,
        val resolveResult: ResolveResult,
        val smartCast: KotlinType?,
        val possibleTypes: Set<KotlinType>
)

data class ExtractionData(
        val originalFile: KtFile,
        val originalRange: KotlinPsiRange,
        val targetSibling: PsiElement,
        val duplicateContainer: PsiElement? = null,
        val options: ExtractionOptions = ExtractionOptions.DEFAULT
) {
    val project: Project = originalFile.getProject()
    val originalElements: List<PsiElement> = originalRange.elements
    val physicalElements = originalElements.map { it.substringContextOrThis }

    val substringInfo: ExtractableSubstringInfo?
        get() = (originalElements.singleOrNull() as? KtExpression)?.extractableSubstringInfo

    val insertBefore: Boolean = options.extractAsProperty
                                || targetSibling.getStrictParentOfType<KtDeclaration>()?.let {
                                    it is KtDeclarationWithBody || it is KtAnonymousInitializer
                                } ?: false

    val expressions = originalElements.filterIsInstance<KtExpression>()

    val codeFragmentText: String by lazy {
        val originalElements = originalElements
        when (originalElements.size) {
            0 -> ""
            1 -> originalElements.first().text
            else -> originalFile.text.substring(originalElements.first().startOffset, originalElements.last().endOffset)
        }
    }

    val originalStartOffset: Int?
        get() = originalElements.firstOrNull()?.let { e -> e.getTextRange()!!.getStartOffset() }

    val commonParent = PsiTreeUtil.findCommonParent(physicalElements) as KtElement

    val bindingContext: BindingContext? by lazy { commonParent.getContextForContainingDeclarationBody() }

    private val itFakeDeclaration by lazy { KtPsiFactory(originalFile).createParameter("it: Any?") }
    private val synthesizedInvokeDeclaration by lazy { KtPsiFactory(originalFile).createFunction("fun invoke() {}") }

    val refOffsetToDeclaration by lazy {
        fun isExtractableIt(descriptor: DeclarationDescriptor, context: BindingContext): Boolean {
            if (!(descriptor is ValueParameterDescriptor && (context[BindingContext.AUTO_CREATED_IT, descriptor] ?: false))) return false
            val function = DescriptorToSourceUtils.descriptorToDeclaration(descriptor.getContainingDeclaration()) as? KtFunctionLiteral
            return function == null || !function.isInsideOf(physicalElements)
        }

        tailrec fun getDeclaration(descriptor: DeclarationDescriptor, context: BindingContext): PsiNameIdentifierOwner? {
            (DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor) as? PsiNameIdentifierOwner)?.let { return it }

            return when {
                isExtractableIt(descriptor, context) -> itFakeDeclaration
                isSynthesizedInvoke(descriptor) -> synthesizedInvokeDeclaration
                descriptor is SyntheticJavaPropertyDescriptor -> getDeclaration(descriptor.getMethod, context)
                else -> null
            }
        }

        val originalStartOffset = originalStartOffset
        val context = bindingContext

        if (originalStartOffset != null && context != null) {
            val resultMap = HashMap<Int, ResolveResult>()

            val visitor = object: KtTreeVisitorVoid() {
                override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
                    if (context[BindingContext.SMARTCAST, expression] != null) {
                        expression.getSelectorExpression()?.accept(this)
                        return
                    }

                    super.visitQualifiedExpression(expression)
                }

                override fun visitSimpleNameExpression(ref: KtSimpleNameExpression) {
                    if (ref.getParent() is KtValueArgumentName) return

                    val physicalRef = substringInfo?.let {
                        // If substring contains some references it must be extracted as a string template
                        val physicalExpression = expressions.single() as KtStringTemplateExpression
                        val extractedContentOffset = physicalExpression.getContentRange().startOffset + physicalExpression.startOffset
                        val offsetInExtracted = ref.startOffset - extractedContentOffset
                        val offsetInTemplate = it.relativeContentRange.startOffset + offsetInExtracted
                        it.template.findElementAt(offsetInTemplate)!!.getStrictParentOfType<KtSimpleNameExpression>()
                    } ?: ref

                    val resolvedCall = physicalRef.getResolvedCall(context)
                    val descriptor = context[BindingContext.REFERENCE_TARGET, physicalRef] ?: return
                    val declaration = getDeclaration(descriptor, context) ?: return

                    val offset = ref.getTextRange()!!.getStartOffset() - originalStartOffset
                    resultMap[offset] = ResolveResult(physicalRef, declaration, descriptor, resolvedCall)
                }
            }
            expressions.forEach { it.accept(visitor) }

            resultMap
        }
        else Collections.emptyMap<Int, ResolveResult>()
    }

    fun getPossibleTypes(expression: KtExpression, resolvedCall: ResolvedCall<*>?, context: BindingContext): Set<KotlinType> {
        val dataFlowInfo = context.getDataFlowInfo(expression)

        (resolvedCall?.getImplicitReceiverValue() as? ImplicitReceiver)?.let {
            return dataFlowInfo.getPossibleTypes(DataFlowValueFactory.createDataFlowValueForStableReceiver(it))
        }

        val type = resolvedCall?.resultingDescriptor?.returnType ?: return emptySet()
        val containingDescriptor = expression.getResolutionScope(context, expression.getResolutionFacade()).ownerDescriptor
        val dataFlowValue = DataFlowValueFactory.createDataFlowValue(expression, type, context, containingDescriptor)
        return dataFlowInfo.getPossibleTypes(dataFlowValue)
    }

    fun getBrokenReferencesInfo(body: KtBlockExpression): List<ResolvedReferenceInfo> {
        val originalContext = bindingContext ?: return listOf()

        val startOffset = body.getBlockContentOffset()

        val referencesInfo = ArrayList<ResolvedReferenceInfo>()
        val refToContextMap = KotlinFileReferencesResolver.resolve(body)
        for ((ref, context) in refToContextMap) {
            if (ref !is KtSimpleNameExpression) continue

            val offset = ref.getTextRange()!!.getStartOffset() - startOffset
            val originalResolveResult = refOffsetToDeclaration[offset] ?: continue

            val smartCast: KotlinType?
            val possibleTypes: Set<KotlinType>

            // Qualified property reference: a.b
            val qualifiedExpression = ref.getQualifiedExpressionForSelector()
            if (qualifiedExpression != null) {
                val smartCastTarget = originalResolveResult.originalRefExpr.getParent() as KtExpression
                smartCast = originalContext[BindingContext.SMARTCAST, smartCastTarget]
                possibleTypes = getPossibleTypes(smartCastTarget, originalResolveResult.resolvedCall, originalContext)
                val receiverDescriptor =
                        (originalResolveResult.resolvedCall?.getDispatchReceiver() as? ImplicitReceiver)?.declarationDescriptor
                if (smartCast == null
                    && !DescriptorUtils.isCompanionObject(receiverDescriptor)
                    && qualifiedExpression.getReceiverExpression() !is KtSuperExpression) continue
            }
            else {
                smartCast = originalContext[BindingContext.SMARTCAST, originalResolveResult.originalRefExpr]
                possibleTypes = getPossibleTypes(originalResolveResult.originalRefExpr, originalResolveResult.resolvedCall, originalContext)
            }

            val parent = ref.getParent()

            // Skip P in type references like 'P.Q'
            if (parent is KtUserType && (parent.getParent() as? KtUserType)?.getQualifier() == parent) continue

            val descriptor = context[BindingContext.REFERENCE_TARGET, ref]
            val isBadRef = !(compareDescriptors(project, originalResolveResult.descriptor, descriptor)
                             && originalContext.diagnostics.forElement(originalResolveResult.originalRefExpr) == context.diagnostics.forElement(ref))
                           || smartCast != null
            if (isBadRef && !originalResolveResult.declaration.isInsideOf(physicalElements)) {
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
internal fun KtExpression.getBlockContentOffset(): Int {
    (this as? KtBlockExpression)?.getLBrace()?.let {
        return it.getTextRange()!!.getStartOffset() + 2
    }
    return getTextRange()!!.getStartOffset()
}
