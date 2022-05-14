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

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.idea.completion.smart.ExpectedInfoMatch
import org.jetbrains.kotlin.idea.completion.smart.SMART_COMPLETION_ITEM_PRIORITY_KEY
import org.jetbrains.kotlin.idea.completion.smart.SmartCompletion
import org.jetbrains.kotlin.idea.completion.smart.SmartCompletionItemPriority
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.*

class BasicCompletionSession(configuration: CompletionSessionConfiguration,
                             parameters: CompletionParameters,
                             resultSet: CompletionResultSet)
: CompletionSession(configuration, parameters, resultSet) {

    private enum class CompletionKind(
            val descriptorKindFilter: DescriptorKindFilter?,
            val classKindFilter: ((ClassKind) -> Boolean)?
    ) {
        ALL(
                descriptorKindFilter = DescriptorKindFilter(DescriptorKindFilter.ALL_KINDS_MASK),
                classKindFilter = { it != ClassKind.ENUM_ENTRY }
        ),

        TYPES(
                descriptorKindFilter = DescriptorKindFilter(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK) exclude DescriptorKindExclude.EnumEntry,
                classKindFilter = { it != ClassKind.ENUM_ENTRY }
        ),

        KEYWORDS_ONLY(descriptorKindFilter = null, classKindFilter = null),

        NAMED_ARGUMENTS_ONLY(descriptorKindFilter = null, classKindFilter = null),

        PARAMETER_NAME(descriptorKindFilter = null, classKindFilter = null),

        SUPER_QUALIFIER(descriptorKindFilter = DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS, classKindFilter = null)
    }

    private val completionKind = calcCompletionKind()

    override val descriptorKindFilter = if (isNoQualifierContext()) {
        // it's an optimization because obtaining top-level packages from scope is very slow, we obtains them in other way
        completionKind.descriptorKindFilter?.exclude(DescriptorKindExclude.TopLevelPackages)
    }
    else {
        completionKind.descriptorKindFilter
    }

    private val parameterNameAndTypeCompletion = if (shouldCompleteParameterNameAndType())
        ParameterNameAndTypeCompletion(collector, lookupElementFactory, prefixMatcher, resolutionFacade)
    else
        null

    private val smartCompletion = expression?.let {
        SmartCompletion(
                it, resolutionFacade, bindingContext, moduleDescriptor, isVisibleFilter, prefixMatcher,
                GlobalSearchScope.EMPTY_SCOPE, toFromOriginalFileMapper, lookupElementFactory, callTypeAndReceiver,
                isJvmModule, forBasicCompletion = true
        )
    }

    override val expectedInfos: Collection<ExpectedInfo>
        get() = smartCompletion?.expectedInfos ?: emptyList()

    private fun calcCompletionKind(): CompletionKind {
        if (nameExpression != null && NamedArgumentCompletion.isOnlyNamedArgumentExpected(nameExpression)) {
            return CompletionKind.NAMED_ARGUMENTS_ONLY
        }

        if (nameExpression == null) {
            val parameter = position.getParent() as? KtParameter
            return if (parameter != null && position == parameter.getNameIdentifier())
                CompletionKind.PARAMETER_NAME
            else
                CompletionKind.KEYWORDS_ONLY
        }

        // Check that completion in the type annotation context and if there's a qualified
        // expression we are at first of it
        val typeReference = position.getStrictParentOfType<KtTypeReference>()
        if (typeReference != null) {
            if (typeReference.parent is KtSuperExpression) {
                return CompletionKind.SUPER_QUALIFIER
            }

            val firstPartReference = PsiTreeUtil.findChildOfType(typeReference, javaClass<KtSimpleNameExpression>())
            if (firstPartReference == nameExpression) {
                return CompletionKind.TYPES
            }
        }

        return CompletionKind.ALL
    }

    private fun shouldCompleteParameterNameAndType(): Boolean {
        if (completionKind != CompletionKind.PARAMETER_NAME) return false

        val parameter = position.getNonStrictParentOfType<KtParameter>()!!
        val list = parameter.parent as? KtParameterList ?: return false
        val owner = list.parent
        return when (owner) {
            is KtCatchClause, is KtPropertyAccessor, is KtFunctionLiteral -> false
            is KtNamedFunction -> owner.nameIdentifier != null
            is KtPrimaryConstructor -> !owner.getContainingClassOrObject().isAnnotation()
            else -> true
        }
    }

    public fun shouldDisableAutoPopup(): Boolean {
        if (completionKind == CompletionKind.PARAMETER_NAME) {
            if (!shouldCompleteParameterNameAndType() || TemplateManager.getInstance(project).getActiveTemplate(parameters.editor) != null) {
                return true
            }

            if (LookupCancelWatcher.getInstance(project).wasAutoPopupRecentlyCancelled(parameters.editor, position.startOffset)) {
                return true
            }
        }

        return false
    }

    override fun doComplete() {
        assert(parameters.getCompletionType() == CompletionType.BASIC)

        if (parameters.isAutoPopup) {
            collector.addLookupElementPostProcessor { lookupElement ->
                lookupElement.putUserData(LookupCancelWatcher.AUTO_POPUP_AT, position.startOffset)
                lookupElement
            }
        }

        if (completionKind == CompletionKind.SUPER_QUALIFIER) {
            completeSuperQualifier()
            return
        }

        // if we are typing parameter name, restart completion each time we type an upper case letter because new suggestions will appear (previous words can be used as user prefix)
        if (parameterNameAndTypeCompletion != null) {
            val prefixPattern = StandardPatterns.string().with(object : PatternCondition<String>("Prefix ends with uppercase letter") {
                override fun accepts(prefix: String, context: ProcessingContext?) = prefix.isNotEmpty() && prefix.last().isUpperCase()
            })
            collector.restartCompletionOnPrefixChange(prefixPattern)

            collector.addLookupElementPostProcessor { lookupElement ->
                lookupElement.putUserData(KotlinCompletionCharFilter.SUPPRESS_ITEM_SELECTION_BY_CHARS_ON_TYPING, Unit)
                lookupElement.putUserData(KotlinCompletionCharFilter.HIDE_LOOKUP_ON_COLON, Unit)
                lookupElement
            }

            parameterNameAndTypeCompletion.addFromParametersInFile(position, resolutionFacade, isVisibleFilter)
            flushToResultSet()

            parameterNameAndTypeCompletion.addFromImportedClasses(position, bindingContext, isVisibleFilter)
            flushToResultSet()
        }

        if (completionKind != CompletionKind.NAMED_ARGUMENTS_ONLY) {
            if (smartCompletion != null) {
                val (additionalItems, @Suppress("UNUSED_VARIABLE") inheritanceSearcher) = smartCompletion.additionalItems()

                // all additional items should have SMART_COMPLETION_ITEM_PRIORITY_KEY to be recognized by SmartCompletionInBasicWeigher
                for (item in additionalItems) {
                    if (item.getUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY) == null) {
                        item.putUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY, SmartCompletionItemPriority.DEFAULT)
                    }
                }

                collector.addElements(additionalItems)
            }

            collector.addDescriptorElements(referenceVariants)

            completeKeywords()

            // getting root packages from scope is very slow so we do this in alternative way
            if (isNoQualifierContext() && (descriptorKindFilter?.kindMask ?: 0).and(DescriptorKindFilter.PACKAGES_MASK) != 0) {
                //TODO: move this code somewhere else?
                val packageNames = PackageIndexUtil.getSubPackageFqNames(FqName.ROOT, originalSearchScope, project, prefixMatcher.asNameFilter())
                        .toMutableSet()

                if (!ProjectStructureUtil.isJsKotlinModule(parameters.getOriginalFile() as KtFile)) {
                    JavaPsiFacade.getInstance(project).findPackage("")?.getSubPackages(originalSearchScope)?.forEach { psiPackage ->
                        val name = psiPackage.getName()
                        if (Name.isValidIdentifier(name!!)) {
                            packageNames.add(FqName(name))
                        }
                    }
                }

                packageNames.forEach { collector.addElement(lookupElementFactory.createLookupElementForPackage(it)) }
            }

            if (completionKind != CompletionKind.KEYWORDS_ONLY) {
                flushToResultSet()

                if (!configuration.completeNonImportedDeclarations && isNoQualifierContext()) {
                    collector.advertiseSecondCompletion()
                }

                completeNonImported()

                if (position.getContainingFile() is KtCodeFragment) {
                    flushToResultSet()
                    collector.addDescriptorElements(getRuntimeReceiverTypeReferenceVariants(), withReceiverCast = true)
                }
            }
        }

        NamedArgumentCompletion.complete(collector, expectedInfos)
    }

    private fun completeKeywords() {
        val keywordsToSkip = HashSet<String>()

        val keywordValueConsumer = object : KeywordValues.Consumer {
            override fun consume(lookupString: String, expectedInfoMatcher: (ExpectedInfo) -> ExpectedInfoMatch, priority: SmartCompletionItemPriority, factory: () -> LookupElement) {
                keywordsToSkip.add(lookupString)
                val lookupElement = factory()
                val matched = expectedInfos.any {
                    val match = expectedInfoMatcher(it)
                    assert(!match.makeNotNullable) { "Nullable keyword values not supported" }
                    match.isMatch()
                }
                if (matched) {
                    lookupElement.putUserData(SmartCompletionInBasicWeigher.KEYWORD_VALUE_MATCHED_KEY, Unit)
                    lookupElement.putUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY, priority)
                }
                collector.addElement(lookupElement)
            }
        }
        KeywordValues.process(keywordValueConsumer, callTypeAndReceiver, bindingContext, resolutionFacade, moduleDescriptor, isJvmModule)

        val keywordsPrefix = prefix.substringBefore('@') // if there is '@' in the prefix - use shorter prefix to not loose 'this' etc
        KeywordCompletion.complete(expression ?: parameters.getPosition(), keywordsPrefix, isJvmModule) { lookupElement ->
            val keyword = lookupElement.lookupString
            if (keyword in keywordsToSkip) return@complete

            when (keyword) {
            // if "this" is parsed correctly in the current context - insert it and all this@xxx items
                "this" -> {
                    if (expression != null) {
                        collector.addElements(thisExpressionItems(bindingContext, expression, prefix, resolutionFacade).map { it.createLookupElement() })
                    }
                    else {
                        // for completion in secondary constructor delegation call
                        collector.addElement(lookupElement)
                    }
                }

            // if "return" is parsed correctly in the current context - insert it and all return@xxx items
                "return" -> {
                    if (expression != null) {
                        collector.addElements(returnExpressionItems(bindingContext, expression))
                    }
                }

                "break", "continue" -> {
                    if (expression != null) {
                        collector.addElements(breakOrContinueExpressionItems(expression, keyword))
                    }
                }

                "override" -> {
                    collector.addElement(lookupElement)

                    OverridesCompletion(collector, lookupElementFactory).complete(position)
                }

                "class" -> {
                    if (callTypeAndReceiver !is CallTypeAndReceiver.CALLABLE_REFERENCE) { // otherwise it should be handled by KeywordValues
                        collector.addElement(lookupElement)
                    }
                }

                else -> collector.addElement(lookupElement)
            }
        }
    }

    private fun completeNonImported() {
        if (completionKind == CompletionKind.ALL) {
            collector.addDescriptorElements(getTopLevelExtensions(), notImported = true)
        }

        flushToResultSet()

        if (shouldRunTopLevelCompletion()) {
            completionKind.classKindFilter?.let { addAllClasses(it) }

            if (completionKind == CompletionKind.ALL) {
                collector.addDescriptorElements(getTopLevelCallables(), notImported = true)
            }
        }

        parameterNameAndTypeCompletion?.addFromAllClasses(parameters, indicesHelper)
    }

    private fun completeSuperQualifier() {
        val classOrObject = position.parents.firstIsInstanceOrNull<KtClassOrObject>() ?: return
        val classDescriptor = resolutionFacade.resolveToDescriptor(classOrObject) as ClassDescriptor
        var superClasses = classDescriptor.defaultType.constructor.supertypes
                .map { it.constructor.declarationDescriptor as? ClassDescriptor }
                .filterNotNull()

        //TODO: IMO it's not good that Any is to be added manually
        if (superClasses.all { it.kind == ClassKind.INTERFACE }) {
            superClasses += classDescriptor.builtIns.any
        }

        if (!isNoQualifierContext()) {
            val referenceVariantsSet = referenceVariants.toSet()
            superClasses = superClasses.filter { it in referenceVariantsSet }
        }

        superClasses
                .map { lookupElementFactory.createLookupElement(it, useReceiverTypes = false, qualifyNestedClasses = true, includeClassTypeArguments = false) }
                .forEach { collector.addElement(it) }
    }

    override fun createSorter(): CompletionSorter {
        var sorter = super.createSorter()

        if (shouldCompleteParameterNameAndType()) {
            sorter = sorter.weighBefore(DeprecatedWeigher.toString(), ParameterNameAndTypeCompletion.Weigher)
        }

        if (smartCompletion != null) {
            val smartCompletionInBasicWeigher = SmartCompletionInBasicWeigher(smartCompletion, callTypeAndReceiver.callType, resolutionFacade)
            sorter = sorter.weighBefore(KindWeigher.toString(), smartCompletionInBasicWeigher, SmartCompletionPriorityWeigher)
        }

        return sorter
    }
}