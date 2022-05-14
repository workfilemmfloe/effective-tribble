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

package org.jetbrains.kotlin.resolve.calls.callUtil

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getTextWithLocation
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.CALL
import org.jetbrains.kotlin.resolve.BindingContext.RESOLVED_CALL
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.sure

// resolved call

public fun <D : CallableDescriptor> ResolvedCall<D>.noErrorsInValueArguments(): Boolean {
    return getCall().getValueArguments().all { argument -> !getArgumentMapping(argument!!).isError() }
}

public fun <D : CallableDescriptor> ResolvedCall<D>.hasUnmappedArguments(): Boolean {
    return getCall().getValueArguments().any { argument -> getArgumentMapping(argument!!) == ArgumentUnmapped }
}

public fun <D : CallableDescriptor> ResolvedCall<D>.hasUnmappedParameters(): Boolean {
    val parameterToArgumentMap = getValueArguments()
    return !parameterToArgumentMap.keySet().containsAll(getResultingDescriptor().getValueParameters())
}

public fun <D : CallableDescriptor> ResolvedCall<D>.allArgumentsMapped()
        = call.valueArguments.all { argument -> getArgumentMapping(argument) is ArgumentMatch }

public fun <D : CallableDescriptor> ResolvedCall<D>.hasTypeMismatchErrorOnParameter(parameter: ValueParameterDescriptor): Boolean {
    val resolvedValueArgument = getValueArguments()[parameter]
    if (resolvedValueArgument == null) return true

    return resolvedValueArgument.getArguments().any { argument ->
        val argumentMapping = getArgumentMapping(argument)
        argumentMapping is ArgumentMatch && argumentMapping.status == ArgumentMatchStatus.TYPE_MISMATCH
    }
}

public fun <D : CallableDescriptor> ResolvedCall<D>.getParameterForArgument(valueArgument: ValueArgument?): ValueParameterDescriptor? {
    return (valueArgument?.let { getArgumentMapping(it) } as? ArgumentMatch)?.valueParameter
}

fun <D : CallableDescriptor> ResolvedCall<D>.usesDefaultArguments(): Boolean {
    return valueArgumentsByIndex?.any { it is DefaultValueArgument } ?: false
}


// call

public fun <C: ResolutionContext<C>> Call.hasUnresolvedArguments(context: ResolutionContext<C>): Boolean {
    val arguments = getValueArguments().map { it.getArgumentExpression() }
    return arguments.any (fun (argument: KtExpression?): Boolean {
        if (argument == null || ArgumentTypeResolver.isFunctionLiteralArgument(argument, context)) return false

        val resolvedCall = argument.getResolvedCall(context.trace.getBindingContext()) as MutableResolvedCall<*>?
        if (resolvedCall != null && !resolvedCall.hasInferredReturnType()) return false

        val expressionType = context.trace.getBindingContext().getType(argument)
        return expressionType == null || expressionType.isError()
    })
}

public fun Call.getValueArgumentsInParentheses(): List<ValueArgument> = getValueArguments().filterArgsInParentheses()

public fun KtCallElement.getValueArgumentsInParentheses(): List<ValueArgument> = getValueArguments().filterArgsInParentheses()

public fun Call.getValueArgumentListOrElement(): KtElement = getValueArgumentList() ?: getCalleeExpression() ?: getCallElement()

@Suppress("UNCHECKED_CAST")
private fun List<ValueArgument?>.filterArgsInParentheses() = filter { it !is KtLambdaArgument } as List<ValueArgument>

public fun Call.getValueArgumentForExpression(expression: KtExpression): ValueArgument? {
    fun KtElement.deparenthesizeStructurally(): KtElement? {
        val deparenthesized = if (this is KtExpression) KtPsiUtil.deparenthesizeOnce(this) else this
        return when {
            deparenthesized != this -> deparenthesized
            this is KtLambdaExpression -> this.getFunctionLiteral()
            this is KtFunctionLiteral -> this.getBodyExpression()
            else -> null
        }
    }
    fun KtElement.isParenthesizedExpression() = sequence(this) { it.deparenthesizeStructurally() }.any { it == expression }
    return getValueArguments().firstOrNull { it?.getArgumentExpression()?.isParenthesizedExpression() ?: false }
}

// Get call / resolved call from binding context

public fun KtElement?.getCalleeExpressionIfAny(): KtExpression? {
    val element = if (this is KtExpression) KtPsiUtil.deparenthesize(this) else this
    return when (element) {
        is KtSimpleNameExpression -> element
        is KtCallElement -> element.getCalleeExpression()
        is KtQualifiedExpression -> element.getSelectorExpression().getCalleeExpressionIfAny()
        is KtOperationExpression -> element.getOperationReference()
        else -> null
    }
}

/**
 *  For expressions like <code>a(), a[i], a.b.c(), +a, a + b, (a()), a(): Int, @label a()</code>
 *  returns a corresponding call.
 *
 *  Note: special construction like <code>a!!, a ?: b, if (c) a else b</code> are resolved as calls,
 *  so there is a corresponding call for them.
 */
public fun KtElement.getCall(context: BindingContext): Call? {
    val element = if (this is KtExpression) KtPsiUtil.deparenthesize(this) else this
    if (element == null) return null

    // Do not use Call bound to outer call expression (if any) to prevent stack overflow during analysis
    if (element is KtCallElement && element.calleeExpression == null) return null

    val parent = element.getParent()
    val reference: KtExpression? = when {
        parent is KtInstanceExpressionWithLabel -> parent
        parent is KtUserType -> parent.getParent()?.getParent() as? KtConstructorCalleeExpression
        else -> element.getCalleeExpressionIfAny()
    }
    if (reference != null) {
        return context[CALL, reference]
    }
    return context[CALL, element]
}

public fun KtElement.getParentCall(context: BindingContext, strict: Boolean = true): Call? {
    val callExpressionTypes = arrayOf<Class<out KtElement>?>(
            javaClass<KtSimpleNameExpression>(), javaClass<KtCallElement>(), javaClass<KtBinaryExpression>(),
            javaClass<KtUnaryExpression>(), javaClass<KtArrayAccessExpression>())

    val parent = if (strict) {
        PsiTreeUtil.getParentOfType(this, *callExpressionTypes)
    } else {
        PsiTreeUtil.getNonStrictParentOfType(this, *callExpressionTypes)
    }
    return parent?.getCall(context)
}

public fun Call?.getResolvedCall(context: BindingContext): ResolvedCall<out CallableDescriptor>? {
    return context[RESOLVED_CALL, this]
}

public fun KtElement?.getResolvedCall(context: BindingContext): ResolvedCall<out CallableDescriptor>? {
    return this?.getCall(context)?.getResolvedCall(context)
}

public fun KtElement?.getParentResolvedCall(context: BindingContext, strict: Boolean = true): ResolvedCall<out CallableDescriptor>? {
    return this?.getParentCall(context, strict)?.getResolvedCall(context)
}

public fun KtElement.getCallWithAssert(context: BindingContext): Call {
    return getCall(context).sure { "No call for ${this.getTextWithLocation()}" }
}

public fun KtElement.getResolvedCallWithAssert(context: BindingContext): ResolvedCall<out CallableDescriptor> {
    return getResolvedCall(context).sure { "No resolved call for ${this.getTextWithLocation()}" }
}

public fun Call.getResolvedCallWithAssert(context: BindingContext): ResolvedCall<out CallableDescriptor> {
    return getResolvedCall(context).sure { "No resolved call for ${this.getCallElement().getTextWithLocation()}" }
}

public fun KtExpression.getFunctionResolvedCallWithAssert(context: BindingContext): ResolvedCall<out FunctionDescriptor> {
    val resolvedCall = getResolvedCallWithAssert(context)
    assert(resolvedCall.getResultingDescriptor() is FunctionDescriptor) {
        "ResolvedCall for this expression must be ResolvedCall<? extends FunctionDescriptor>: ${this.getTextWithLocation()}"
    }
    @Suppress("UNCHECKED_CAST")
    return resolvedCall as ResolvedCall<out FunctionDescriptor>
}

public fun Call.isSafeCall(): Boolean {
    if (this is CallTransformer.CallForImplicitInvoke) {
        //implicit safe 'invoke'
        if (getOuterCall().isExplicitSafeCall()) {
            return true
        }
    }
    return isExplicitSafeCall()
}

public fun Call.isExplicitSafeCall(): Boolean = getCallOperationNode()?.getElementType() == KtTokens.SAFE_ACCESS

public fun Call.createLookupLocation() = KotlinLookupLocation(run {
    calleeExpression?.let {
        // Can't use getContainingJetFile() because we can get from IDE an element with JavaDummyHolder as containing file
        if ((it.containingFile as? KtFile)?.doNotAnalyze == null) it else null
    }
    ?: callElement
})
