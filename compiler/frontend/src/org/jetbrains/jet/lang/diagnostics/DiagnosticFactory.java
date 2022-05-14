/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.diagnostics;

import org.jetbrains.annotations.NotNull;

public abstract class DiagnosticFactory {

    private String name = null;
    private final Severity severity;

    protected DiagnosticFactory(@NotNull Severity severity) {
        this.severity = severity;
    }

    /*package*/ void setName(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public Severity getSeverity() {
        return severity;
    }

    @Override
    public String toString() {
        return getName();
    }
}
