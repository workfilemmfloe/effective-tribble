/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.core.script

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.ex.PathUtilEx
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.addToStdlib.flattenTo
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.script.dependencies.Environment
import kotlin.script.dependencies.ScriptContents
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.dependencies.ScriptDependencies
import kotlin.script.experimental.dependencies.asSuccess
import kotlin.script.templates.standard.ScriptTemplateWithArgs

class ScriptDefinitionsManager(private val project: Project) {
    private val lock = ReentrantReadWriteLock()
    private var definitionsByContributor = mutableMapOf<ScriptDefinitionContributor, List<KotlinScriptDefinition>>()
    private var definitions: List<KotlinScriptDefinition> = emptyList()

    fun reloadDefinitionsBy(contributor: ScriptDefinitionContributor) = lock.write {
        if (contributor !in definitionsByContributor) error("Unknown contributor: ${contributor.id}")
        definitionsByContributor[contributor] = contributor.getDefinitions()
        updateDefinitions()
    }

    fun getDefinitions(): List<KotlinScriptDefinition> = lock.read {
        if (definitions.isNotEmpty()) return definitions

        else {
            reloadScriptDefinitions()
            return definitions
        }
    }

    private fun getContributors(): List<ScriptDefinitionContributor> =
            Extensions.getArea(project).getExtensionPoint(ScriptDefinitionContributor.EP_NAME).extensions.toList()

    fun reloadScriptDefinitions() = lock.write {
        definitionsByContributor = getContributors().associateByTo(mutableMapOf(), { it }, { it.getDefinitions() })
        updateDefinitions()
    }

    private fun updateDefinitions() {
        definitions = definitionsByContributor.values.flattenTo(mutableListOf())
    }
}


abstract class TemplateBasedScriptDefinitionContributor: ScriptDefinitionContributor {
    abstract val templateClassNames: Iterable<String>

    abstract val templateClasspath: List<File>

    // TODO: need to provide a way to specify this in compiler/repl .. etc
    /*
     * Allows to specify additional jars needed for DependenciesResolver (and not script template).
     * Script template dependencies naturally become (part of) dependencies of the script which is not always desired for resolver dependencies.
     * i.e. gradle resolver may depend on some jars that 'built.gradle.kts' files should not depend on.
     */
    open val additionalResolverClasspath: List<File> get() = emptyList()

    abstract val environment: Map<String, Any?>?

    override fun getDefinitions() = try {
        val loader = createClassLoader()
        templateClassNames.map {
            KotlinScriptDefinitionFromAnnotatedTemplate(
                    loader.loadClass(it).kotlin,
                    environment,
                    templateClasspath
            )
        }
    }
    catch (ex: Throwable) {
        LOG.info("Templates provider ${id} is invalid: ${ex.message}")
        emptyList<KotlinScriptDefinition>()
    }

    private fun createClassLoader(): ClassLoader {
        val classpath = templateClasspath + additionalResolverClasspath
        LOG.info("[kts] loading script definitions ${templateClassNames} using cp: ${classpath.joinToString(File.pathSeparator)}")
        val baseLoader = TemplateBasedScriptDefinitionContributor::class.java.classLoader
        return if (classpath.isEmpty()) baseLoader else URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), baseLoader)
    }

    private val LOG = Logger.getInstance("ScriptTemplatesProviders")
}

interface ScriptDefinitionContributor {
    // for resolving ambiguities
    val id: String

    fun getDefinitions(): List<KotlinScriptDefinition>

    companion object {
        val EP_NAME: ExtensionPointName<ScriptDefinitionContributor> =
                ExtensionPointName.create<ScriptDefinitionContributor>("org.jetbrains.kotlin.scriptDefinitionContributor")
    }

}

class StandardScriptDefinitionContributor(private val project: Project) : ScriptDefinitionContributor {
    override fun getDefinitions() = listOf(StandardIdeScriptDefinition(project))

    override val id: String = "StandardKotlinScript"
}


class StandardIdeScriptDefinition(project: Project) : KotlinScriptDefinition(ScriptTemplateWithArgs::class) {
    override val dependencyResolver = BundledKotlinScriptDependenciesResolver(project)
}

class BundledKotlinScriptDependenciesResolver(private val project: Project) : DependenciesResolver {
    override fun resolve(
            scriptContents: ScriptContents,
            environment: Environment
    ): DependenciesResolver.ResolveResult {
        val javaHome = getScriptSDK(project)
        val dependencies = ScriptDependencies(
                javaHome = javaHome?.let(::File),
                classpath = with(PathUtil.kotlinPathsForIdeaPlugin) {
                    listOf(
                            reflectPath,
                            stdlibPath,
                            scriptRuntimePath
                    )
                }
        )
        return dependencies.asSuccess()
    }

    private fun getScriptSDK(project: Project): String? {
        val jdk = ProjectJdkTable.getInstance().allJdks.firstOrNull { sdk -> sdk.sdkType is JavaSdk } ?: PathUtilEx.getAnyJdk(project)
        return jdk?.homePath
    }
}

