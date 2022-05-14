/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.facet

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModel
import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.idea.compiler.configuration.Kotlin2JsCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCompilerSettings
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCompilerWorkspaceSettings
import org.jetbrains.kotlin.idea.framework.JSLibraryStdPresentationProvider
import org.jetbrains.kotlin.idea.framework.JavaRuntimePresentationProvider
import org.jetbrains.kotlin.idea.framework.getLibraryProperties
import org.jetbrains.kotlin.idea.maven.configuration.KotlinJavaMavenConfigurator
import org.jetbrains.kotlin.idea.maven.configuration.KotlinJavascriptMavenConfigurator
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion

private fun getRuntimeLibraryVersions(
        module: Module,
        rootModel: ModuleRootModel?,
        targetPlatform: TargetPlatformKind<*>
): Collection<String> {
    val presentationProvider = when (targetPlatform) {
        is JSPlatform -> JSLibraryStdPresentationProvider.getInstance()
        is JVMPlatform -> JavaRuntimePresentationProvider.getInstance()
    }

    KotlinVersionInfoProvider.EP_NAME
            .extensions
            .map { it.getLibraryVersions(module, targetPlatform) }
            .firstOrNull { it.isNotEmpty() }
            ?.let { return it }

    return (rootModel ?: ModuleRootManager.getInstance(module))
            .orderEntries
            .asSequence()
            .filterIsInstance<LibraryOrderEntry>()
            .mapNotNull { it.library?.let { getLibraryProperties(presentationProvider, it) }?.versionString }
            .toList()
}

private fun getDefaultTargetPlatform(module: Module, rootModel: ModuleRootModel?): TargetPlatformKind<*> {
    if (getRuntimeLibraryVersions(module, rootModel, JSPlatform).any()) {
        return JSPlatform
    }

    val sdk = ((rootModel ?: ModuleRootManager.getInstance(module))).sdk
    val sdkVersion = (sdk?.sdkType as? JavaSdk)?.getVersion(sdk!!)
    return when {
        sdkVersion != null && sdkVersion <= JavaSdkVersion.JDK_1_6 -> JVMPlatform[JVMVersion.JVM_1_6]
        else -> JVMPlatform[JVMVersion.JVM_1_8]
    }
}

private fun getDefaultLanguageLevel(
        module: Module,
        explicitVersion: String? = null
): LanguageLevel {
    val libVersion = explicitVersion
                     ?: KotlinVersionInfoProvider.EP_NAME.extensions
                             .mapNotNull { it.getCompilerVersion(module) }
                             .minWith(VersionComparatorUtil.COMPARATOR)
                     ?: bundledRuntimeVersion()
    return when {
        libVersion.startsWith("1.0") -> LanguageLevel.KOTLIN_1_0
        else -> LanguageLevel.KOTLIN_1_1
    }
}

internal fun getLibraryLanguageLevel(
        module: Module,
        rootModel: ModuleRootModel?,
        targetPlatform: TargetPlatformKind<*>?
): LanguageLevel {
    val minVersion = getRuntimeLibraryVersions(module, rootModel, targetPlatform ?: JVMPlatform[JVMVersion.JVM_1_8])
            .minWith(VersionComparatorUtil.COMPARATOR)
    return getDefaultLanguageLevel(module, minVersion)
}

internal fun KotlinFacetConfiguration.Settings.initializeIfNeeded(module: Module, rootModel: ModuleRootModel?) {
    val project = module.project

    with(versionInfo) {
        if (targetPlatformKindKind == null) {
            targetPlatformKindKind = getDefaultTargetPlatform(module, rootModel)
        }

        if (languageLevel == null) {
            languageLevel = getDefaultLanguageLevel(module)
        }

        if (apiLevel == null) {
            apiLevel = languageLevel!!.coerceAtMost(getLibraryLanguageLevel(module, rootModel, targetPlatformKindKind!!))
        }
    }

    with(compilerInfo) {
        if (commonCompilerArguments == null) {
            commonCompilerArguments = KotlinCommonCompilerArgumentsHolder.getInstance(project).settings.copy()
        }

        if (compilerSettings == null) {
            compilerSettings = CompilerSettings(KotlinCompilerSettings.getInstance(project).settings)
        }

        if (k2jsCompilerArguments == null) {
            k2jsCompilerArguments = Kotlin2JsCompilerArgumentsHolder.getInstance (project).settings.copy()
        }
    }
}

internal fun Module.getKotlinSettings(rootModel: ModuleRootModel? = null): KotlinFacetConfiguration.Settings {
    val settings = KotlinFacet.get(this)?.configuration?.state ?: KotlinFacetConfiguration.Settings()
    settings.initializeIfNeeded(this, rootModel)
    return settings
}

val TargetPlatformKind<*>.mavenLibraryId: String
    get() {
        return when (this) {
            is JVMPlatform -> KotlinJavaMavenConfigurator.STD_LIB_ID
            is JSPlatform -> KotlinJavascriptMavenConfigurator.STD_LIB_ID
        }
    }