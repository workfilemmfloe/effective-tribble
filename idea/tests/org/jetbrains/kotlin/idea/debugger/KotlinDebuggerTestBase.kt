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

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.MethodFilter
import org.jetbrains.kotlin.idea.util.application.runReadAction
import com.intellij.debugger.impl.PositionUtil
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.roots.libraries.LibraryUtil
import org.jetbrains.kotlin.idea.test.JetJdkAndLibraryProjectDescriptor
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.util.io.FileUtil
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.settings.DebuggerSettings
import kotlin.properties.Delegates
import org.jetbrains.kotlin.test.InTextDirectivesUtils.findStringWithPrefixes
import kotlin.properties.Delegates
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.DebuggerContextImpl

abstract class KotlinDebuggerTestBase : KotlinDebuggerTestCase() {
    private var oldSettings: DebuggerSettings by Delegates.notNull()
    private var oldIsFilterForStdlibAlreadyAdded: Boolean by Delegates.notNull()
    private var oldDisableKotlinInternalClasses: Boolean by Delegates.notNull()
    private var oldRenderDelegatedProperties: Boolean by Delegates.notNull()

    override fun initApplication() {
        super.initApplication()
        saveDefaultSettings()
    }

    override fun tearDown() {
        super.tearDown()
        restoreDefaultSettings()
    }

    protected fun configureSettings(fileText: String) {
        val kotlinSettings = KotlinDebuggerSettings.getInstance()
        kotlinSettings.DEBUG_IS_FILTER_FOR_STDLIB_ALREADY_ADDED = false
        kotlinSettings.DEBUG_DISABLE_KOTLIN_INTERNAL_CLASSES = fileText.getValueForSetting("DISABLE_KOTLIN_INTERNAL_CLASSES", oldDisableKotlinInternalClasses)
        kotlinSettings.DEBUG_RENDER_DELEGATED_PROPERTIES = fileText.getValueForSetting("RENDER_DELEGATED_PROPERTIES", oldRenderDelegatedProperties)

        val debuggerSettings = DebuggerSettings.getInstance()!!
        debuggerSettings.SKIP_SYNTHETIC_METHODS = fileText.getValueForSetting("SKIP_SYNTHETIC_METHODS", oldSettings.SKIP_SYNTHETIC_METHODS)
        debuggerSettings.SKIP_CONSTRUCTORS = fileText.getValueForSetting("SKIP_CONSTRUCTORS", oldSettings.SKIP_CONSTRUCTORS)
        debuggerSettings.SKIP_CLASSLOADERS = fileText.getValueForSetting("SKIP_CLASSLOADERS", oldSettings.SKIP_CLASSLOADERS)
        debuggerSettings.TRACING_FILTERS_ENABLED = fileText.getValueForSetting("TRACING_FILTERS_ENABLED", oldSettings.TRACING_FILTERS_ENABLED)
    }

    private fun String.getValueForSetting(name: String, defaultValue: Boolean): Boolean {
        return findStringWithPrefixes(this, "// $name: ")?.toBoolean() ?: defaultValue
    }

    private fun saveDefaultSettings() {
        oldIsFilterForStdlibAlreadyAdded = KotlinDebuggerSettings.getInstance().DEBUG_IS_FILTER_FOR_STDLIB_ALREADY_ADDED
        oldDisableKotlinInternalClasses = KotlinDebuggerSettings.getInstance().DEBUG_DISABLE_KOTLIN_INTERNAL_CLASSES
        oldRenderDelegatedProperties = KotlinDebuggerSettings.getInstance().DEBUG_RENDER_DELEGATED_PROPERTIES
        oldSettings = DebuggerSettings.getInstance()!!.clone()
    }

    private fun restoreDefaultSettings() {
        KotlinDebuggerSettings.getInstance().DEBUG_IS_FILTER_FOR_STDLIB_ALREADY_ADDED = oldIsFilterForStdlibAlreadyAdded
        KotlinDebuggerSettings.getInstance().DEBUG_DISABLE_KOTLIN_INTERNAL_CLASSES = oldDisableKotlinInternalClasses
        KotlinDebuggerSettings.getInstance().DEBUG_RENDER_DELEGATED_PROPERTIES = oldRenderDelegatedProperties

        val debuggerSettings = DebuggerSettings.getInstance()!!
        debuggerSettings.SKIP_SYNTHETIC_METHODS = oldSettings.SKIP_SYNTHETIC_METHODS
        debuggerSettings.SKIP_CONSTRUCTORS = oldSettings.SKIP_CONSTRUCTORS
        debuggerSettings.SKIP_CLASSLOADERS = oldSettings.SKIP_CLASSLOADERS
        debuggerSettings.TRACING_FILTERS_ENABLED = oldSettings.TRACING_FILTERS_ENABLED
    }

    protected val dp: DebugProcessImpl
        get() = getDebugProcess() ?: throw AssertionError("createLocalProcess() should be called before getDebugProcess()")

    protected fun onBreakpoint(doOnBreakpoint: SuspendContextImpl.() -> Unit) {
        super.onBreakpoint {
            initContexts(it)
            it.printContext()
            it.doOnBreakpoint()
        }
    }

    protected fun SuspendContextImpl.stepInto() {
        this.stepInto(false, null)
    }

    protected var evaluationContext: EvaluationContextImpl by Delegates.notNull()
    protected var debuggerContext: DebuggerContextImpl by Delegates.notNull()

    protected fun initContexts(suspendContext: SuspendContextImpl) {
        evaluationContext = createEvaluationContext(suspendContext)
        debuggerContext = createDebuggerContext(suspendContext)
    }

    protected fun SuspendContextImpl.stepInto(ignoreFilters: Boolean, smartStepFilter: MethodFilter?) {
        dp.getManagerThread()!!.schedule(dp.createStepIntoCommand(this, ignoreFilters, smartStepFilter))
    }

    protected fun SuspendContextImpl.printContext() {
        runReadAction {
            if (this.getFrameProxy() == null) {
                return@runReadAction println("Context thread is null", ProcessOutputTypes.SYSTEM)
            }

            val sourcePosition = PositionUtil.getSourcePosition(this)
            println(renderSourcePosition(sourcePosition), ProcessOutputTypes.SYSTEM)
        }
    }

    protected fun renderSourcePosition(sourcePosition: SourcePosition?): String {
        if (sourcePosition == null) {
            return "null"
        }

        val virtualFile = sourcePosition.getFile().getVirtualFile()
        if (virtualFile == null) {
            return "VirtualFile for position is null"
        }

        val libraryEntry = LibraryUtil.findLibraryEntry(virtualFile, getProject())
        if (libraryEntry != null && (libraryEntry is JdkOrderEntry ||
                                     libraryEntry.getPresentableName() == JetJdkAndLibraryProjectDescriptor.LIBRARY_NAME)) {
            return FileUtil.getNameWithoutExtension(virtualFile.getName()) + ".!EXT!"
        }

        return virtualFile.getName() + ":" + (sourcePosition.getLine() + 1)
    }

    protected fun finish() {
        onBreakpoint {
            resume(this)
        }
    }
}
