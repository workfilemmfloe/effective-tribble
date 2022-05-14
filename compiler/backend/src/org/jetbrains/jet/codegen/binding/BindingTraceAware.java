/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.binding;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.BindingTrace;

/**
 * @author alex.tkachman
 */
public class BindingTraceAware extends BindingContextAware {
    @NotNull protected final BindingTrace bindingTrace;

    public BindingTraceAware(@NotNull BindingTrace bindingTrace) {
        super(bindingTrace.getBindingContext());
        this.bindingTrace = bindingTrace;
    }

    @NotNull public final BindingTrace getBindingTrace() {
        return bindingTrace;
    }
}
