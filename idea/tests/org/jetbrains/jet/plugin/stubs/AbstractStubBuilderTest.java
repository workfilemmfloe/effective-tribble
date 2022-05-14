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

package org.jetbrains.jet.plugin.stubs;

import com.intellij.lang.FileASTNode;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.stubs.StubElement;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.stubs.elements.JetFileStubBuilder;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils;

import java.io.File;

import static org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils.NO_NAME_FOR_LAZY_RESOLVE;

public abstract class AbstractStubBuilderTest extends LightCodeInsightFixtureTestCase {
    protected void doTest(@NotNull String sourcePath) {
        JetFile file = (JetFile) myFixture.configureByFile(sourcePath);
        JetFileStubBuilder jetStubBuilder = new JetFileStubBuilder();
        StubElement lighterTree = jetStubBuilder.buildStubTree(file);
        String stubTree = DebugUtil.stubTreeToString(lighterTree).replace(NO_NAME_FOR_LAZY_RESOLVE.asString(), "<no name>");
        String expectedFile = sourcePath.replace(".kt", ".expected");
        JetTestUtils.assertEqualsToFile(new File(expectedFile), stubTree);
    }
}
