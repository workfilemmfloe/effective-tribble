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

package org.jetbrains.kotlin.idea.configuration

import com.intellij.framework.FrameworkTypeEx
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.versions.MAVEN_JS_STDLIB_ID
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion
import org.jetbrains.plugins.gradle.frameworkSupport.BuildScriptDataBuilder
import org.jetbrains.plugins.gradle.frameworkSupport.GradleFrameworkSupportProvider
import javax.swing.Icon

abstract class GradleKotlinFrameworkSupportProvider(val frameworkTypeId: String,
                                                    val displayName: String) : GradleFrameworkSupportProvider() {
    override fun getFrameworkType(): FrameworkTypeEx = object : FrameworkTypeEx(frameworkTypeId) {
        override fun getIcon(): Icon = KotlinIcons.SMALL_LOGO

        override fun getPresentableName(): String = displayName

        override fun createProvider(): FrameworkSupportInModuleProvider = this@GradleKotlinFrameworkSupportProvider
    }

    override fun addSupport(module: Module,
                            rootModel: ModifiableRootModel,
                            modifiableModelsProvider: ModifiableModelsProvider,
                            buildScriptData: BuildScriptDataBuilder) {
        var kotlinVersion = bundledRuntimeVersion()

        val additionalRepository: String? = when {
            kotlinVersion == "@snapshot@" -> {
                kotlinVersion = "1.1-SNAPSHOT"
                KotlinWithGradleConfigurator.SNAPSHOT_REPOSITORY_SNIPPET
            }
            useEap11Repository(kotlinVersion) -> {
                KotlinWithGradleConfigurator.EAP_11_REPOSITORY_SNIPPET
            }
            isEap(kotlinVersion) -> {
                KotlinWithGradleConfigurator.EAP_REPOSITORY_SNIPPET
            }
            else -> {
                null
            }
        }

        if (additionalRepository != null) {
            val oneLineRepository = additionalRepository.replace('\n', ' ')
            buildScriptData.addBuildscriptRepositoriesDefinition(oneLineRepository)

            buildScriptData.addRepositoriesDefinition("mavenCentral()")
            buildScriptData.addRepositoriesDefinition(oneLineRepository)
        }

        buildScriptData
                .addPluginDefinition(getPluginDefinition())

                .addBuildscriptRepositoriesDefinition("mavenCentral()")
                .addRepositoriesDefinition("mavenCentral()")

                .addBuildscriptPropertyDefinition("ext.kotlin_version = '$kotlinVersion'")
                .addDependencyNotation(getRuntimeLibrary(rootModel))
                .addBuildscriptDependencyNotation(KotlinWithGradleConfigurator.CLASSPATH)
    }

    protected abstract fun getRuntimeLibrary(rootModel: ModifiableRootModel): String

    protected abstract fun getPluginDefinition(): String
}

class GradleKotlinJavaFrameworkSupportProvider : GradleKotlinFrameworkSupportProvider("KOTLIN", "Kotlin (Java)") {
    override fun getPluginDefinition() = KotlinGradleModuleConfigurator.APPLY_KOTLIN

    override fun getRuntimeLibrary(rootModel: ModifiableRootModel) =
            KotlinWithGradleConfigurator.getRuntimeLibraryForSdk(rootModel.sdk, bundledRuntimeVersion())
}

class GradleKotlinJSFrameworkSupportProvider : GradleKotlinFrameworkSupportProvider("KOTLIN_JS", "Kotlin (JavaScript)") {
    override fun getPluginDefinition(): String {
        return KotlinJsGradleModuleConfigurator.APPLY_KOTLIN_JS
    }

    override fun getRuntimeLibrary(rootModel: ModifiableRootModel) =
            KotlinWithGradleConfigurator.getDependencySnippet(MAVEN_JS_STDLIB_ID)
}
