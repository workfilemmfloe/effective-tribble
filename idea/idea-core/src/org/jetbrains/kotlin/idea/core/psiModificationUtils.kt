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

package org.jetbrains.kotlin.idea.core

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getLambdaArgumentName
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.calls.callUtil.getValueArgumentsInParentheses

@Suppress("UNCHECKED_CAST")
inline fun <reified T: PsiElement> PsiElement.replaced(newElement: T): T {
    val result = replace(newElement)
    return if (result is T)
        result
    else
        (result as KtParenthesizedExpression).expression as T
}

@Suppress("UNCHECKED_CAST") fun <T: PsiElement> T.copied(): T = copy() as T

fun KtLambdaArgument.moveInsideParentheses(bindingContext: BindingContext): KtCallExpression {
    return moveInsideParenthesesAndReplaceWith(this.getArgumentExpression(), bindingContext)
}

fun KtLambdaArgument.moveInsideParenthesesAndReplaceWith(
        replacement: KtExpression,
        bindingContext: BindingContext
): KtCallExpression = moveInsideParenthesesAndReplaceWith(replacement, getLambdaArgumentName(bindingContext))

fun KtLambdaArgument.moveInsideParenthesesAndReplaceWith(
        replacement: KtExpression,
        functionLiteralArgumentName: Name?
): KtCallExpression {
    val oldCallExpression = parent as KtCallExpression
    val newCallExpression = oldCallExpression.copy() as KtCallExpression

    val psiFactory = KtPsiFactory(project)
    val argument = if (newCallExpression.getValueArgumentsInParentheses().any { it.isNamed() }) {
        psiFactory.createArgument(replacement, functionLiteralArgumentName)
    }
    else {
        psiFactory.createArgument(replacement)
    }

    val functionLiteralArgument = newCallExpression.lambdaArguments.firstOrNull()!!
    val valueArgumentList = newCallExpression.valueArgumentList ?: psiFactory.createCallArguments("()")

    valueArgumentList.addArgument(argument)

    (functionLiteralArgument.prevSibling as? PsiWhiteSpace)?.delete()
    if (newCallExpression.valueArgumentList != null) {
        functionLiteralArgument.delete()
    }
    else {
        functionLiteralArgument.replace(valueArgumentList)
    }
    return oldCallExpression.replace(newCallExpression) as KtCallExpression
}

fun KtCallExpression.moveFunctionLiteralOutsideParentheses() {
    assert(lambdaArguments.isEmpty())
    val argumentList = valueArgumentList!!
    val argument = argumentList.arguments.last()
    val expression = argument.getArgumentExpression()!!
    assert(expression.unpackFunctionLiteral() != null)

    val dummyCall = KtPsiFactory(this).createExpressionByPattern("foo()$0:'{}'", expression) as KtCallExpression
    val functionLiteralArgument = dummyCall.lambdaArguments.single()
    this.add(functionLiteralArgument)
    if (argumentList.arguments.size > 1) {
        argumentList.removeArgument(argument)
    }
    else {
        argumentList.delete()
    }
}

fun KtBlockExpression.appendElement(element: KtElement, addNewLine: Boolean = false): KtElement {
    val rBrace = rBrace
    val newLine = KtPsiFactory(this).createNewLine()
    val anchor = if (rBrace == null) {
        val lastChild = lastChild
        if (lastChild !is PsiWhiteSpace) addAfter(newLine, lastChild)!! else lastChild
    }
    else {
        rBrace.prevSibling!!
    }
    val addedElement = addAfter(element, anchor)!! as KtElement
    if (addNewLine) {
        addAfter(newLine, addedElement)
    }
    return addedElement
}

//TODO: git rid of this method
fun PsiElement.deleteElementAndCleanParent() {
    val parent = parent

    deleteElementWithDelimiters(this)
    deleteChildlessElement(parent, this.javaClass)
}

// Delete element if it doesn't contain children of a given type
private fun <T : PsiElement> deleteChildlessElement(element: PsiElement, childClass: Class<T>) {
    if (PsiTreeUtil.getChildrenOfType<T>(element, childClass) == null) {
        element.delete()
    }
}

// Delete given element and all the elements separating it from the neighboring elements of the same class
private fun deleteElementWithDelimiters(element: PsiElement) {
    val paramBefore = PsiTreeUtil.getPrevSiblingOfType<PsiElement>(element, element.javaClass)

    val from: PsiElement
    val to: PsiElement
    if (paramBefore != null) {
        from = paramBefore.nextSibling
        to = element
    }
    else {
        val paramAfter = PsiTreeUtil.getNextSiblingOfType<PsiElement>(element, element.javaClass)

        from = element
        to = if (paramAfter != null) paramAfter.prevSibling else element
    }

    val parent = element.parent

    parent.deleteChildRange(from, to)
}

fun PsiElement.deleteSingle() {
    CodeEditUtil.removeChild(parent?.node ?: return, node ?: return)
}

fun KtClass.getOrCreateCompanionObject() : KtObjectDeclaration {
    getCompanionObjects().firstOrNull()?.let { return it }
    return addDeclaration(KtPsiFactory(this).createCompanionObject()) as KtObjectDeclaration
}

//TODO: code style option whether to insert redundant 'public' keyword or not
fun KtDeclaration.setVisibility(visibilityModifier: KtModifierKeywordToken) {
    val defaultVisibilityKeyword = implicitVisibility()

    if (visibilityModifier == defaultVisibilityKeyword) {
        this.visibilityModifierType()?.let { removeModifier(it) }
        return
    }

    addModifier(visibilityModifier)
}

fun KtDeclaration.implicitVisibility(): KtModifierKeywordToken? {
    val defaultVisibilityKeyword = if (hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
        (resolveToDescriptor() as? CallableMemberDescriptor)
                ?.overriddenDescriptors
                ?.let { OverridingUtil.findMaxVisibility(it) }
                ?.toKeywordToken()
    }
    else {
        KtTokens.DEFAULT_VISIBILITY_KEYWORD
    }
    return defaultVisibilityKeyword
}

fun KtSecondaryConstructor.getOrCreateBody(): KtBlockExpression {
    bodyExpression?.let { return it }

    val delegationCall = getDelegationCall()
    val anchor = if (delegationCall.isImplicit) valueParameterList else delegationCall
    val newBody = KtPsiFactory(this).createEmptyBody()
    return addAfter(newBody, anchor) as KtBlockExpression
}

fun KtParameter.dropDefaultValue() {
    val from = equalsToken ?: return
    val to = defaultValue ?: from
    deleteChildRange(from, to)
}

fun dropEnclosingParenthesesIfPossible(expression: KtExpression): KtExpression {
    val parent = expression.parent as? KtParenthesizedExpression ?: return expression
    if (!KtPsiUtil.areParenthesesUseless(parent)) return expression
    return parent.replaced(expression)
}