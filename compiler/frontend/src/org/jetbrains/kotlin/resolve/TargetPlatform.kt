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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap
import org.jetbrains.kotlin.resolve.calls.checkers.*
import org.jetbrains.kotlin.resolve.checkers.ApiVersionClassifierUsageChecker
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.DynamicTypesSettings

abstract class TargetPlatform(val platformName: String) {
    override fun toString() = platformName

    abstract val platformConfigurator: PlatformConfigurator
    abstract val builtIns: KotlinBuiltIns
    abstract val defaultImports: List<ImportPath>

    object Default : TargetPlatform("Default") {
        override val builtIns: KotlinBuiltIns
            get() = DefaultBuiltIns.Instance
        override val defaultImports = emptyList<ImportPath>()
        override val platformConfigurator =
                object : PlatformConfigurator(DynamicTypesSettings(), listOf(), listOf(), listOf(), listOf(), listOf(),
                                              IdentifierChecker.DEFAULT, OverloadFilter.DEFAULT, PlatformToKotlinClassMap.EMPTY) {
                    override fun configure(container: StorageComponentContainer) {
                        super.configure(container)
                        container.useInstance(SyntheticScopes.Empty)
                    }
                }
    }
}

private val DEFAULT_DECLARATION_CHECKERS = listOf(
        DataClassDeclarationChecker(),
        ConstModifierChecker,
        UnderscoreChecker,
        InlineParameterChecker,
        OperatorModifierChecker(),
        InfixModifierChecker())

private val DEFAULT_CALL_CHECKERS = listOf(
        CapturingInClosureChecker(), InlineCheckerWrapper(), ReifiedTypeParameterSubstitutionChecker(), SafeCallChecker(),
        DeprecatedCallChecker, CallReturnsArrayOfNothingChecker(), InfixCallChecker(), OperatorCallChecker(),
        ConstructorHeaderCallChecker, ProtectedConstructorCallChecker, ApiVersionCallChecker
)
private val DEFAULT_TYPE_CHECKERS = emptyList<AdditionalTypeChecker>()
private val DEFAULT_CLASSIFIER_USAGE_CHECKERS = listOf(DeprecatedClassifierUsageChecker(), ApiVersionClassifierUsageChecker)


abstract class PlatformConfigurator(
        private val dynamicTypesSettings: DynamicTypesSettings,
        additionalDeclarationCheckers: List<DeclarationChecker>,
        additionalCallCheckers: List<CallChecker>,
        additionalTypeCheckers: List<AdditionalTypeChecker>,
        additionalClassifierUsageCheckers: List<ClassifierUsageChecker>,
        private val additionalAnnotationCheckers: List<AdditionalAnnotationChecker>,
        private val identifierChecker: IdentifierChecker,
        private val overloadFilter: OverloadFilter,
        private val platformToKotlinClassMap: PlatformToKotlinClassMap
) {
    private val declarationCheckers: List<DeclarationChecker> = DEFAULT_DECLARATION_CHECKERS + additionalDeclarationCheckers
    private val callCheckers: List<CallChecker> = DEFAULT_CALL_CHECKERS + additionalCallCheckers
    private val typeCheckers: List<AdditionalTypeChecker> = DEFAULT_TYPE_CHECKERS + additionalTypeCheckers
    private val classifierUsageCheckers: List<ClassifierUsageChecker> = DEFAULT_CLASSIFIER_USAGE_CHECKERS + additionalClassifierUsageCheckers

    open fun configure(container: StorageComponentContainer) {
        with (container) {
            useInstance(dynamicTypesSettings)
            declarationCheckers.forEach { useInstance(it) }
            callCheckers.forEach { useInstance(it) }
            typeCheckers.forEach { useInstance(it) }
            classifierUsageCheckers.forEach { useInstance(it) }
            additionalAnnotationCheckers.forEach { useInstance(it) }
            useInstance(identifierChecker)
            useInstance(overloadFilter)
            useInstance(platformToKotlinClassMap)
        }
    }
}

@JvmOverloads
fun TargetPlatform.createModule(
        name: Name,
        storageManager: StorageManager,
        capabilities: Map<ModuleDescriptor.Capability<*>, Any?> = emptyMap()
) = ModuleDescriptorImpl(name, storageManager, defaultImports, builtIns, capabilities)
