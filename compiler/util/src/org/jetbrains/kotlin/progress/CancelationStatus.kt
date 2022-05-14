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

package org.jetbrains.kotlin.progress

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicatorProvider
import kotlin.platform.platformStatic

public class CompilationCanceledException : ProcessCanceledException()

public interface CompilationCanceledStatus {
    fun checkCanceled(): Unit
}

public object ProgressIndicatorAndCompilationCanceledStatus {
    private var canceledStatus: CompilationCanceledStatus? = null

    platformStatic
    synchronized public fun setCompilationCanceledStatus(newCanceledStatus: CompilationCanceledStatus?): Unit {
        canceledStatus = newCanceledStatus
    }

    platformStatic
    public fun checkCanceled(): Unit {
        ProgressIndicatorProvider.checkCanceled()
        canceledStatus?.checkCanceled()
    }
}