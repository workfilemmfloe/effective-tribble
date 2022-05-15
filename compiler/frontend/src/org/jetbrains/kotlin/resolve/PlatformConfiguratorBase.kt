/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.builtins.PlatformToKotlinClassMap
import org.jetbrains.kotlin.builtins.PlatformToKotlinClassMapClashesResolver
import org.jetbrains.kotlin.container.*
import org.jetbrains.kotlin.resolve.calls.checkers.*
import org.jetbrains.kotlin.resolve.calls.components.SamConversionTransformerClashesResolver
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparatorClashesResolver
import org.jetbrains.kotlin.resolve.checkers.*
import org.jetbrains.kotlin.resolve.deprecation.CoroutineCompatibilitySupportClashesResolver
import org.jetbrains.kotlin.resolve.lazy.DelegationFilter
import org.jetbrains.kotlin.resolve.lazy.DelegationFiltersClashResolver
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopesClashesResolver
import org.jetbrains.kotlin.types.DynamicTypesSettings
import org.jetbrains.kotlin.types.DynamicTypesSettingsClashesResolver
import org.jetbrains.kotlin.types.expressions.FunctionWithBigAritySupportClashesResolver

private val DEFAULT_DECLARATION_CHECKERS = listOf(
    DataClassDeclarationChecker(),
    ConstModifierChecker,
    UnderscoreChecker,
    InlineParameterChecker,
    InfixModifierChecker(),
    SinceKotlinAnnotationValueChecker,
    RequireKotlinAnnotationValueChecker,
    ReifiedTypeParameterAnnotationChecker(),
    DynamicReceiverChecker,
    DelegationChecker(),
    KClassWithIncorrectTypeArgumentChecker,
    SuspendLimitationsChecker,
    InlineClassDeclarationChecker,
    PropertiesWithBackingFieldsInsideInlineClass(),
    AnnotationClassTargetAndRetentionChecker(),
    ReservedMembersAndConstructsForInlineClass(),
    ResultClassInReturnTypeChecker(),
    LocalVariableTypeParametersChecker()
)

private val DEFAULT_CALL_CHECKERS = listOf(
    CapturingInClosureChecker(), InlineCheckerWrapper(), SafeCallChecker(),
    DeprecatedCallChecker, CallReturnsArrayOfNothingChecker(), InfixCallChecker(), OperatorCallChecker(),
    ConstructorHeaderCallChecker, ProtectedConstructorCallChecker, ApiVersionCallChecker,
    CoroutineSuspendCallChecker, BuilderFunctionsCallChecker, DslScopeViolationCallChecker, MissingDependencyClassChecker,
    CallableReferenceCompatibilityChecker(), LateinitIntrinsicApplicabilityChecker,
    UnderscoreUsageChecker, AssigningNamedArgumentToVarargChecker(), ImplicitNothingAsTypeParameterCallChecker,
    PrimitiveNumericComparisonCallChecker, LambdaWithSuspendModifierCallChecker,
    UselessElvisCallChecker(), ResultTypeWithNullableOperatorsChecker(), NullableVarargArgumentCallChecker,
    NamedFunAsExpressionChecker, ContractNotAllowedCallChecker, ReifiedTypeParameterSubstitutionChecker(), TypeOfChecker
)
private val DEFAULT_TYPE_CHECKERS = emptyList<AdditionalTypeChecker>()
private val DEFAULT_CLASSIFIER_USAGE_CHECKERS = listOf(
    DeprecatedClassifierUsageChecker(), ApiVersionClassifierUsageChecker, MissingDependencyClassChecker.ClassifierUsage,
    OptionalExpectationUsageChecker()
)
private val DEFAULT_ANNOTATION_CHECKERS = listOf<AdditionalAnnotationChecker>()

private val DEFAULT_CLASH_RESOLVERS = listOf<PlatformExtensionsClashResolver<*>>(
    DynamicTypesSettingsClashesResolver(),
    IdentifierCheckerClashesResolver(),
    OverloadFilterClashesResolver(),
    PlatformToKotlinClassMapClashesResolver(),
    DelegationFiltersClashResolver(),
    OverridesBackwardCompatibilityHelperClashesResolver(),
    DeclarationReturnTypeSanitizerClashesResolver(),
    SyntheticScopesClashesResolver(),
    TypeSpecificityComparatorClashesResolver(),
    SamConversionTransformerClashesResolver(),
    FunctionWithBigAritySupportClashesResolver(),
    PlatformDiagnosticSuppressorClashesResolver(),
    CoroutineCompatibilitySupportClashesResolver(),
    ExpectedActualDeclarationChecker.ActualAnnotationArgumentExtractorClashResolver()
)

fun StorageComponentContainer.configureDefaultCheckers() {
    DEFAULT_DECLARATION_CHECKERS.forEach { useInstance(it) }
    DEFAULT_CALL_CHECKERS.forEach { useInstance(it) }
    DEFAULT_TYPE_CHECKERS.forEach { useInstance(it) }
    DEFAULT_CLASSIFIER_USAGE_CHECKERS.forEach { useInstance(it) }
    DEFAULT_ANNOTATION_CHECKERS.forEach { useInstance(it) }
    DEFAULT_CLASH_RESOLVERS.forEach { useClashResolver(it) }
}


abstract class PlatformConfiguratorBase(
    val dynamicTypesSettings: DynamicTypesSettings,
    val additionalDeclarationCheckers: List<DeclarationChecker>,
    val additionalCallCheckers: List<CallChecker>,
    val additionalTypeCheckers: List<AdditionalTypeChecker>,
    val additionalClassifierUsageCheckers: List<ClassifierUsageChecker>,
    val additionalAnnotationCheckers: List<AdditionalAnnotationChecker>,
    val additionalClashResolvers: List<PlatformExtensionsClashResolver<*>>,
    val identifierChecker: IdentifierChecker,
    val overloadFilter: OverloadFilter,
    val platformToKotlinClassMap: PlatformToKotlinClassMap,
    val delegationFilter: DelegationFilter,
    val overridesBackwardCompatibilityHelper: OverridesBackwardCompatibilityHelper,
    val declarationReturnTypeSanitizer: DeclarationReturnTypeSanitizer
) : PlatformConfigurator {
    override val platformSpecificContainer = composeContainer(this::class.java.simpleName) {
        configureDefaultCheckers()
        configureExtensionsAndCheckers(this)
    }

    override fun configureModuleDependentCheckers(container: StorageComponentContainer) {
        container.useImpl<ExperimentalMarkerDeclarationAnnotationChecker>()
        container.useImpl<ExpectedActualDeclarationChecker>()
    }

    fun configureExtensionsAndCheckers(container: StorageComponentContainer) {
        with(container) {
            useInstance(dynamicTypesSettings)
            additionalDeclarationCheckers.forEach { useInstance(it) }
            additionalCallCheckers.forEach { useInstance(it) }
            additionalTypeCheckers.forEach { useInstance(it) }
            additionalClassifierUsageCheckers.forEach { useInstance(it) }
            additionalAnnotationCheckers.forEach { useInstance(it) }
            additionalClashResolvers.forEach { useClashResolver(it) }
            useInstance(identifierChecker)
            useInstance(overloadFilter)
            useInstance(platformToKotlinClassMap)
            useInstance(delegationFilter)
            useInstance(overridesBackwardCompatibilityHelper)
            useInstance(declarationReturnTypeSanitizer)
        }
    }
}

fun createContainer(id: String, compilerServices: PlatformDependentCompilerServices, init: StorageComponentContainer.() -> Unit) =
    composeContainer(id, compilerServices.platformConfigurator.platformSpecificContainer, init)
