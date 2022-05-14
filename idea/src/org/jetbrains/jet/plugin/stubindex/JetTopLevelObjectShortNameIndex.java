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

package org.jetbrains.jet.plugin.stubindex;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;

import java.util.Collection;

public class JetTopLevelObjectShortNameIndex extends StringStubIndexExtension<JetObjectDeclaration> {
    private static final StubIndexKey<String, JetObjectDeclaration> KEY = KotlinIndexUtil.createIndexKey(JetTopLevelObjectShortNameIndex.class);

    private static final JetTopLevelObjectShortNameIndex ourInstance = new JetTopLevelObjectShortNameIndex();

    public static JetTopLevelObjectShortNameIndex getInstance() {
        return ourInstance;
    }

    private JetTopLevelObjectShortNameIndex() {}

    @NotNull
    @Override
    public StubIndexKey<String, JetObjectDeclaration> getKey() {
        return KEY;
    }

    @Override
    public Collection<JetObjectDeclaration> get(String s, Project project, @NotNull GlobalSearchScope scope) {
        return super.get(s, project, JetSourceFilterScope.kotlinSourcesAndLibraries(scope));
    }
}
