/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.debugger

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.MethodFilter
import org.jetbrains.jet.plugin.util.application.runReadAction
import com.intellij.debugger.impl.PositionUtil
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.roots.libraries.LibraryUtil
import org.jetbrains.jet.plugin.JetJdkAndLibraryProjectDescriptor

abstract class KotlinDebuggerTestBase : KotlinDebuggerTestCase() {

    protected val dp: DebugProcessImpl
        get() = getDebugProcess() ?: throw AssertionError("createLocalProcess() should be called before getDebugProcess()")

    protected fun onBreakpoint(doOnBreakpoint: SuspendContextImpl.() -> Unit) {
        super.onBreakpoint {
            it.printContext()
            it.doOnBreakpoint()
        }
    }

    protected fun SuspendContextImpl.stepInto() {
        this.stepInto(false, null)
    }

    protected fun SuspendContextImpl.stepInto(ignoreFilters: Boolean, smartStepFilter: MethodFilter?) {
        dp.getManagerThread()!!.schedule(dp.createStepIntoCommand(this, ignoreFilters, smartStepFilter))
    }

    protected fun SuspendContextImpl.printContext() {
        runReadAction {(): Unit ->
            if (this.getFrameProxy() == null) {
                return@runReadAction println("Context thread is null", ProcessOutputTypes.SYSTEM)
            }

            val sourcePosition = PositionUtil.getSourcePosition(this)
            if (sourcePosition == null) {
                return@runReadAction println("SourcePosition is null", ProcessOutputTypes.SYSTEM)
            }

            val virtualFile = sourcePosition.getFile().getVirtualFile()
            if (virtualFile == null) {
                return@runReadAction println("VirtualFile for position is null", ProcessOutputTypes.SYSTEM)
            }

            val isInStdlib = LibraryUtil.findLibraryEntry(virtualFile, getProject())?.getPresentableName() == JetJdkAndLibraryProjectDescriptor.LIBRARY_NAME
            if (isInStdlib) {
                return@runReadAction println(virtualFile.getName(), ProcessOutputTypes.SYSTEM)
            }

            println(virtualFile.getName() + ":" + sourcePosition.getLine(), ProcessOutputTypes.SYSTEM)
        }
    }

    protected fun finish() {
        onBreakpoint {
            resume(this)
        }
    }
}