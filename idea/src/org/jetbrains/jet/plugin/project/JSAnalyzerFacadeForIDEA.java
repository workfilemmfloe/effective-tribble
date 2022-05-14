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

package org.jetbrains.jet.plugin.project;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzerFacade;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.analyze.AnalyzerFacadeForJS;

import java.util.Collection;

public enum JSAnalyzerFacadeForIDEA implements AnalyzerFacade {

    INSTANCE;

    private JSAnalyzerFacadeForIDEA() {
    }

    @NotNull
    @Override
    public Setup createSetup(@NotNull Project project, @NotNull Collection<JetFile> files) {
        return new BasicSetup(AnalyzerFacadeForJS.getLazyResolveSession(files, new IDEAConfig(project)));
    }
}
