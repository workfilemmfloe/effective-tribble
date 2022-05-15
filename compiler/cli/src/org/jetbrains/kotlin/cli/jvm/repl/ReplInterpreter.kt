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

package org.jetbrains.kotlin.cli.jvm.repl

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.Disposable
import com.intellij.psi.PsiCompiledElement
import org.jetbrains.kotlin.backend.jvm.descriptors.JvmDescriptorWithExtraFlags
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmModulePathRoot
import org.jetbrains.kotlin.cli.jvm.repl.codeinsight.ReplCodeInsightFacadeImpl
import org.jetbrains.kotlin.cli.jvm.repl.configuration.ReplConfiguration
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.ImportingScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.collectDescriptorsFiltered
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import java.io.File
import java.io.PrintWriter
import java.util.concurrent.atomic.AtomicInteger

class ReplInterpreter(
        disposable: Disposable,
        private val configuration: CompilerConfiguration,
        private val replConfiguration: ReplConfiguration
) {
    private val lineNumber = AtomicInteger()

    private val previousIncompleteLines = arrayListOf<String>()

    private val classpathRoots = configuration.getList(JVMConfigurationKeys.CONTENT_ROOTS).mapNotNull { root ->
        when (root) {
            is JvmModulePathRoot -> root.file // TODO: only add required modules
            is JvmClasspathRoot -> root.file
            else -> null
        }
    }

    private val classpathRootsWithSources by lazy {
        // This is lazy because compiler configuration is not yet set up propertly when the class is initiated
        classpathRoots.let { it + LibrarySourcesSearcher(configuration).findAdditionalSourceJars(it) }
    }

    private val messageCollector = object : MessageCollector {
        private var hasErrors = false
        private val messageRenderer = MessageRenderer.WITHOUT_PATHS

        override fun clear() {
            hasErrors = false
        }

        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
            val msg = messageRenderer.render(severity, message, location).trimEnd()
            with (replConfiguration.writer) {
                when (severity) {
                    CompilerMessageSeverity.EXCEPTION -> sendInternalErrorReport(msg)
                    CompilerMessageSeverity.ERROR -> outputCompileError(msg)
                    CompilerMessageSeverity.STRONG_WARNING -> {} // TODO consider reporting this and two below
                    CompilerMessageSeverity.WARNING -> {}
                    CompilerMessageSeverity.INFO -> {}
                    else -> {}
                }
            }
        }

        override fun hasErrors(): Boolean = hasErrors
    }

    // TODO: add script definition with project-based resolving for IDEA repl
    private val scriptCompiler by lazy {
        GenericReplCompiler(disposable, REPL_LINE_AS_SCRIPT_DEFINITION, configuration, messageCollector)
    }
    private val scriptEvaluator: ReplFullEvaluator by lazy {
        GenericReplCompilingEvaluator(scriptCompiler, classpathRoots, null, null, ReplRepeatingMode.REPEAT_ANY_PREVIOUS)
    }

    private val docProvider by lazy {
        LibraryDocInfoProvider(evalState.asState<GenericReplCompilerState>().analyzerEngine.project)
    }

    private val evalState by lazy { scriptEvaluator.createState() }

    fun eval(line: String): ReplEvalResult {
        val fullText = concatWithPreviousLines(line)

        try {
            val evalRes = scriptEvaluator.compileAndEval(evalState, ReplCodeLine(lineNumber.getAndIncrement(), 0, fullText), null, object : InvokeWrapper {
                override fun <T> invoke(body: () -> T): T = replConfiguration.executionInterceptor.execute(body)
            })

            when {
                evalRes !is ReplEvalResult.Incomplete -> previousIncompleteLines.clear()
                replConfiguration.allowIncompleteLines -> previousIncompleteLines.add(line)
                else -> return ReplEvalResult.Error.CompileTime("incomplete code")
            }
            return evalRes
        }
        catch (e: Throwable) {
            val writer = PrintWriter(System.err)
            scriptEvaluator.classLoader.dumpClasses(writer)
            writer.flush()
            throw e
        }
    }

    fun complete(line: String): Collection<DeclarationDescriptor> {
        val compilerState = evalState.asState<GenericReplCompilerState>()

        val codeFragment = KtPsiFactory(compilerState.analyzerEngine.project).createExpressionCodeFragment(line, null)
        compilerState.analyzerEngine.setDelegateFactory(codeFragment.containingKtFile)

        val targetIdentifier =
            codeFragment.findElementAt(codeFragment.textLength - 1)
                ?.getNonStrictParentOfType(KtSimpleNameExpression::class.java)
                    ?: return emptyList()

        val codeInsightFacade = ReplCodeInsightFacadeImpl.create(compilerState.analyzerEngine)
        return codeInsightFacade.complete(targetIdentifier, "o")
    }

    fun doc(rawFqName: String): String? {
        val compilerState = evalState.asState<GenericReplCompilerState>()

        val bindingContext = compilerState.analyzerEngine.trace.bindingContext
        val moduleDescriptor = compilerState.analyzerEngine.module
        val qualifiedExpressionResolver = compilerState.analyzerEngine.qualifiedExpressionResolver

        val codeFragment = KtPsiFactory(compilerState.analyzerEngine.project).createExpressionCodeFragment(rawFqName, null)
        compilerState.analyzerEngine.setDelegateFactory(codeFragment.containingKtFile)

        val targetExpression = codeFragment.findElementAt(codeFragment.textLength - 1)?.let {
            it.getStrictParentOfType<KtQualifiedExpression>()
                    ?: it.getNonStrictParentOfType(KtSimpleNameExpression::class.java)
        } ?: return null

        val scope = compilerState.analyzerEngine.lastSuccessfulLine()?.scopeForInitializerResolution
                    ?: LexicalScope.Base(ImportingScope.Empty, moduleDescriptor)

        val descriptors = when (targetExpression) {
            is KtDotQualifiedExpression -> {
                val (containingDescriptor, memberName) = qualifiedExpressionResolver
                        .resolveClassOrPackageInQualifiedExpression(targetExpression, scope, bindingContext)

                if (containingDescriptor == null) {
                    return null
                }

                when (memberName) {
                    null -> listOf(containingDescriptor)
                    else -> {
                        val scopes = when (containingDescriptor) {
                            is PackageViewDescriptor -> listOf(containingDescriptor.memberScope)
                            is PackageFragmentDescriptor -> listOf(containingDescriptor.getMemberScope())
                            is ClassDescriptor -> listOf(containingDescriptor.unsubstitutedMemberScope, containingDescriptor.staticScope)
                            is TypeAliasDescriptor -> listOfNotNull(containingDescriptor.classDescriptor?.unsubstitutedMemberScope)
                            else -> null
                        } ?: return null

                        scopes.flatMap { it.getContributedDescriptors(CALLABLES_AND_CLASSIFIERS) { it == memberName } }
                              .filter { it.name == memberName }
                    }
                }
            }
            is KtSimpleNameExpression -> {
                val a = scope.collectDescriptorsFiltered(
                        CALLABLES_AND_CLASSIFIERS,
                        { it.asString() == targetExpression.getReferencedName() })
                a
            }
            else -> return null
        }.takeIf { it.isNotEmpty() } ?: return null

        return descriptors.joinToString(LINE_SEPARATOR + LINE_SEPARATOR) { descriptor ->
            val fqName = descriptor.fqNameSafe
            val documentation = findDocumentation(classpathRootsWithSources, descriptor)
            fqName.asString() + LINE_SEPARATOR + documentation.lines().joinToString(LINE_SEPARATOR) { "    " + it }
        }
    }

    private fun findDocumentation(libraries: List<File>, descriptor: DeclarationDescriptor): String {
        if (descriptor is DeclarationDescriptorWithSource) {
            val psi = descriptor.source.getPsi()

            // REPL declaration
            if (psi != null && psi !is PsiCompiledElement) {
                return psi.getChildrenOfType<KDoc>()
                        .flatMap { it.getChildrenOfType<KDocSection>().asList() }
                        .joinToString(LINE_SEPARATOR + LINE_SEPARATOR) { it.text.trim() }
            }

            // Library declaration
            val (packageName, declarationName) = splitPackageDeclarationNames(descriptor)

            val language = when (descriptor) {
                is JavaClassDescriptor, is JvmDescriptorWithExtraFlags, is JavaCallableMemberDescriptor -> JavaLanguage.INSTANCE
                else -> KotlinLanguage.INSTANCE
            }

            docProvider.getDoc(language, libraries, packageName, declarationName)?.let { return it }
        }

        return "No documentation found."
    }

    private fun splitPackageDeclarationNames(descriptor: DeclarationDescriptor): Pair<String, String> {
        fun getPackageName(descriptor: DeclarationDescriptor): FqName = when (descriptor) {
            is PackageFragmentDescriptor -> descriptor.fqName
            is PackageViewDescriptor -> descriptor.fqName
            else -> getPackageName(descriptor.containingDeclaration ?: error("$descriptor is outside any package"))
        }

        val fqName = descriptor.fqNameSafe.asString()
        val packageName = getPackageName(descriptor).asString()

        if (packageName.isEmpty()) {
            return Pair("", fqName)
        }

        assert(fqName.startsWith(packageName + "."))
        return Pair(packageName, fqName.drop(packageName.length + 1))
    }

    private fun concatWithPreviousLines(line: String) = (previousIncompleteLines + line).joinToString(separator = "\n")

    fun dumpClasses(out: PrintWriter) {
        scriptEvaluator.classLoader.dumpClasses(out)
    }

    companion object {
        private val REPL_LINE_AS_SCRIPT_DEFINITION = object : KotlinScriptDefinition(Any::class) {
            override val name = "Kotlin REPL"
        }

        private val LINE_SEPARATOR = System.getProperty("line.separator")

        private val CALLABLES_AND_CLASSIFIERS = DescriptorKindFilter(
                DescriptorKindFilter.CALLABLES_MASK or DescriptorKindFilter.CLASSIFIERS_MASK)
    }
}
