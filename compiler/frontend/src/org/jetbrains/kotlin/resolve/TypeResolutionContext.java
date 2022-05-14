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

package org.jetbrains.kotlin.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.resolve.scopes.JetScope;

public class TypeResolutionContext {
    public final JetScope scope;
    public final BindingTrace trace;
    public final boolean checkBounds;
    public final boolean allowBareTypes;

    public TypeResolutionContext(@NotNull JetScope scope, @NotNull BindingTrace trace, boolean checkBounds, boolean allowBareTypes) {
        this.scope = scope;
        this.trace = trace;
        this.checkBounds = checkBounds;
        this.allowBareTypes = allowBareTypes;
    }

    public TypeResolutionContext noBareTypes() {
        return new TypeResolutionContext(scope, trace, checkBounds, false);
    }
}
