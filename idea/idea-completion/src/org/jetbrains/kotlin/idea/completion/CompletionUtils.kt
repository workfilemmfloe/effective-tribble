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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.*
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Key
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.idea.JetIcons
import org.jetbrains.kotlin.idea.completion.handlers.CastReceiverInsertHandler
import org.jetbrains.kotlin.idea.completion.handlers.WithTailInsertHandler
import org.jetbrains.kotlin.idea.core.getResolutionScope
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.idea.util.findLabelAndCall
import org.jetbrains.kotlin.idea.util.getImplicitReceiversWithInstanceToExpression
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.scopes.utils.asJetScope
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.nullability
import java.util.*

tailrec fun <T : Any> LookupElement.putUserDataDeep(key: Key<T>, value: T?) {
    if (this is LookupElementDecorator<*>) {
        getDelegate().putUserDataDeep(key, value)
    }
    else {
        putUserData(key, value)
    }
}

tailrec fun <T : Any> LookupElement.getUserDataDeep(key: Key<T>): T? {
    if (this is LookupElementDecorator<*>) {
        return getDelegate().getUserDataDeep(key)
    }
    else {
        return getUserData(key)
    }
}

enum class ItemPriority {
    DEFAULT,
    BACKING_FIELD,
    NAMED_PARAMETER
}

val ITEM_PRIORITY_KEY = Key<ItemPriority>("ITEM_PRIORITY_KEY")

fun LookupElement.assignPriority(priority: ItemPriority): LookupElement {
    putUserData(ITEM_PRIORITY_KEY, priority)
    return this
}

val STATISTICS_INFO_CONTEXT_KEY = Key<String>("STATISTICS_INFO_CONTEXT_KEY")

val NOT_IMPORTED_KEY = Key<Unit>("NOT_IMPORTED_KEY")

fun LookupElement.suppressAutoInsertion() = AutoCompletionPolicy.NEVER_AUTOCOMPLETE.applyPolicy(this)

fun LookupElement.withReceiverCast(): LookupElement {
    return object: LookupElementDecorator<LookupElement>(this) {
        override fun handleInsert(context: InsertionContext) {
            super.handleInsert(context)
            CastReceiverInsertHandler.handleInsert(context, getDelegate())
        }
    }
}

fun LookupElement.withBracesSurrounding(): LookupElement {
    return object: LookupElementDecorator<LookupElement>(this) {
        override fun handleInsert(context: InsertionContext) {
            val startOffset = context.getStartOffset()
            context.getDocument().insertString(startOffset, "{")
            context.getOffsetMap().addOffset(CompletionInitializationContext.START_OFFSET, startOffset + 1)

            val tailOffset = context.getTailOffset()
            context.getDocument().insertString(tailOffset, "}")
            context.setTailOffset(tailOffset)

            super.handleInsert(context)
        }
    }
}

val KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY = Key<Unit>("KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY")

fun LookupElement.keepOldArgumentListOnTab(): LookupElement {
    putUserData(KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY, Unit)
    return this
}

fun rethrowWithCancelIndicator(exception: ProcessCanceledException): ProcessCanceledException {
    val indicator = CompletionService.getCompletionService().getCurrentCompletion() as CompletionProgressIndicator

    // Force cancel to avoid deadlock in CompletionThreading.delegateWeighing()
    if (!indicator.isCanceled()) {
        indicator.cancel()
    }

    return exception
}

fun PrefixMatcher.asNameFilter() = { name: Name ->
    if (name.isSpecial()) {
        false
    }
    else {
        val identifier = name.getIdentifier()
        if (getPrefix().startsWith("$")) { // we need properties from scope for backing field completion
            prefixMatches("$" + identifier)
        }
        else {
            prefixMatches(identifier)
        }
    }
}

fun LookupElementPresentation.prependTailText(text: String, grayed: Boolean) {
    val tails = getTailFragments()
    clearTail()
    appendTailText(text, grayed)
    tails.forEach { appendTailText(it.text, it.isGrayed()) }
}

enum class CallableWeight {
    local, // local non-extension
    thisClassMember,
    baseClassMember,
    thisTypeExtension,
    baseTypeExtension,
    globalOrStatic, // global non-extension
    typeParameterExtension,
    receiverCastRequired
}

val CALLABLE_WEIGHT_KEY = Key<CallableWeight>("CALLABLE_WEIGHT_KEY")

fun InsertionContext.isAfterDot(): Boolean {
    var offset = getStartOffset()
    val chars = getDocument().getCharsSequence()
    while (offset > 0) {
        offset--
        val c = chars.charAt(offset)
        if (!Character.isWhitespace(c)) {
            return c == '.'
        }
    }
    return false
}

// do not complete this items by prefix like "is"
fun shouldCompleteThisItems(prefixMatcher: PrefixMatcher): Boolean {
    val prefix = prefixMatcher.getPrefix()
    val s = "this@"
    return prefix.startsWith(s) || s.startsWith(prefix)
}

class ThisItemLookupObject(val receiverParameter: ReceiverParameterDescriptor, val labelName: Name?) : KeywordLookupObject()

fun ThisItemLookupObject.createLookupElement() = createKeywordElement("this", labelName.labelNameToTail(), lookupObject = this)
        .withTypeText(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(receiverParameter.type))

fun thisExpressionItems(bindingContext: BindingContext, position: JetExpression, prefix: String, resolutionFacade: ResolutionFacade): Collection<ThisItemLookupObject> {
    val scope = position.getResolutionScope(bindingContext, resolutionFacade)

    val psiFactory = JetPsiFactory(position)

    val result = ArrayList<ThisItemLookupObject>()
    for ((receiver, expressionFactory) in scope.asJetScope().getImplicitReceiversWithInstanceToExpression()) {
        if (expressionFactory == null) continue
        // if prefix does not start with "this@" do not include immediate this in the form with label
        val expression = expressionFactory.createExpression(psiFactory, shortThis = !prefix.startsWith("this@")) as? JetThisExpression ?: continue
        result.add(ThisItemLookupObject(receiver, expression.getLabelNameAsName()))
    }
    return result
}

fun returnExpressionItems(bindingContext: BindingContext, position: JetElement): Collection<LookupElement> {
    val result = ArrayList<LookupElement>()
    for (parent in position.parentsWithSelf) {
        if (parent is JetDeclarationWithBody) {
            val returnType = parent.returnType(bindingContext)
            val isUnit = returnType == null || KotlinBuiltIns.isUnit(returnType)
            if (parent is JetFunctionLiteral) {
                val (label, call) = parent.findLabelAndCall()
                if (label != null) {
                    result.add(createKeywordElementWithSpace("return", tail = label.labelNameToTail(), addSpaceAfter = !isUnit))
                }

                // check if the current function literal is inlined and stop processing outer declarations if it's not
                val callee = call?.getCalleeExpression() as? JetReferenceExpression ?: break // not inlined
                if (!InlineUtil.isInline(bindingContext[BindingContext.REFERENCE_TARGET, callee])) break // not inlined
            }
            else {
                if (parent.hasBlockBody()) {
                    result.add(createKeywordElementWithSpace("return", addSpaceAfter = !isUnit))

                    if (returnType != null) {
                        if (returnType.nullability() == TypeNullability.NULLABLE) {
                            result.add(createKeywordElement("return null"))
                        }

                        if (KotlinBuiltIns.isBooleanOrNullableBoolean(returnType)) {
                            result.add(createKeywordElement("return true"))
                            result.add(createKeywordElement("return false"))
                        }
                        else if (KotlinBuiltIns.isCollectionOrNullableCollection(returnType) || KotlinBuiltIns.isListOrNullableList(returnType) || KotlinBuiltIns.isIterableOrNullableIterable(returnType)) {
                            result.add(createKeywordElement("return", tail = " emptyList()"))
                        }
                        else if (KotlinBuiltIns.isSetOrNullableSet(returnType)) {
                            result.add(createKeywordElement("return", tail = " emptySet()"))
                        }
                    }
                }
                break
            }
        }
    }
    return result
}

private fun JetDeclarationWithBody.returnType(bindingContext: BindingContext): JetType? {
    val callable = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? CallableDescriptor ?: return null
    return callable.getReturnType()
}

private fun Name?.labelNameToTail(): String = if (this != null) "@" + render() else ""

private fun createKeywordElementWithSpace(
        keyword: String,
        tail: String = "",
        addSpaceAfter: Boolean = false,
        lookupObject: KeywordLookupObject = KeywordLookupObject()
): LookupElement {
    val element = createKeywordElement(keyword, tail, lookupObject)
    return if (addSpaceAfter) {
        object: LookupElementDecorator<LookupElement>(element) {
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler.spaceTail().handleInsert(context, getDelegate())
            }
        }
    }
    else {
        element
    }
}

private fun createKeywordElement(
        keyword: String,
        tail: String = "",
        lookupObject: KeywordLookupObject = KeywordLookupObject()
): LookupElementBuilder {
    var element = LookupElementBuilder.create(lookupObject, keyword + tail)
    element = element.withPresentableText(keyword)
    element = element.withBoldness(true)
    if (tail.isNotEmpty()) {
        element = element.withTailText(tail, false)
    }
    return element
}

fun breakOrContinueExpressionItems(position: JetElement, breakOrContinue: String): Collection<LookupElement> {
    val result = ArrayList<LookupElement>()

    parentsLoop@
    for (parent in position.parentsWithSelf) {
        when (parent) {
            is JetLoopExpression -> {
                if (result.isEmpty()) {
                    result.add(createKeywordElement(breakOrContinue))
                }

                val label = (parent.getParent() as? JetLabeledExpression)?.getLabelNameAsName()
                if (label != null) {
                    result.add(createKeywordElement(breakOrContinue, tail = label.labelNameToTail()))
                }
            }

            is JetDeclarationWithBody -> break@parentsLoop //TODO: support non-local break's&continue's when they are supported by compiler
        }
    }
    return result
}

fun LookupElementFactory.createLookupElementForType(type: JetType): LookupElement? {
    if (type.isError()) return null

    if (KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(type)) {
        val text = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type)
        val baseLookupElement = LookupElementBuilder.create(text).withIcon(JetIcons.LAMBDA)
        return BaseTypeLookupElement(type, baseLookupElement)
    }
    else {
        val classifier = type.getConstructor().getDeclarationDescriptor() ?: return null
        val baseLookupElement = createLookupElement(classifier, useReceiverTypes = false, qualifyNestedClasses = true, includeClassTypeArguments = false)

        val itemText = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type)

        val typeLookupElement = object : BaseTypeLookupElement(type, baseLookupElement) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)
                presentation.setItemText(itemText)
            }
        }

        // if type is simply classifier without anything else, use classifier's lookup element to avoid duplicates (works after "as" in basic completion)
        return if (typeLookupElement.fullText == IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(classifier))
            baseLookupElement
        else
            typeLookupElement
    }
}

private open class BaseTypeLookupElement(type: JetType, baseLookupElement: LookupElement) : LookupElementDecorator<LookupElement>(baseLookupElement) {
    val fullText = IdeDescriptorRenderers.SOURCE_CODE.renderType(type)

    override fun equals(other: Any?) = other is BaseTypeLookupElement && fullText == other.fullText
    override fun hashCode() = fullText.hashCode()

    override fun renderElement(presentation: LookupElementPresentation) {
        getDelegate().renderElement(presentation)
    }

    override fun handleInsert(context: InsertionContext) {
        context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), fullText)
        context.setTailOffset(context.getStartOffset() + fullText.length())
        shortenReferences(context, context.getStartOffset(), context.getTailOffset())
    }
}

fun shortenReferences(context: InsertionContext, startOffset: Int, endOffset: Int) {
    PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments();
    ShortenReferences.DEFAULT.process(context.getFile() as JetFile, startOffset, endOffset)
}

fun <T> ElementPattern<T>.and(rhs: ElementPattern<T>) = StandardPatterns.and(this, rhs)
fun <T> ElementPattern<T>.andNot(rhs: ElementPattern<T>) = StandardPatterns.and(this, StandardPatterns.not(rhs))
fun <T> ElementPattern<T>.or(rhs: ElementPattern<T>) = StandardPatterns.or(this, rhs)

fun singleCharPattern(char: Char) = StandardPatterns.character().equalTo(char)
