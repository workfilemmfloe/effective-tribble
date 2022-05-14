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

package org.jetbrains.kotlin.idea.stubs;

import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.stubs.StubElement;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.stubs.elements.KtFileStubBuilder;
import org.jetbrains.kotlin.test.JetTestUtils;

import java.io.File;

public abstract class AbstractStubBuilderTest extends LightCodeInsightFixtureTestCase {
    protected void doTest(@NotNull String sourcePath) {
        KtFile file = (KtFile) myFixture.configureByFile(sourcePath);
        KtFileStubBuilder jetStubBuilder = new KtFileStubBuilder();
        StubElement lighterTree = jetStubBuilder.buildStubTree(file);
        String stubTree = serializeStubToString(lighterTree);
        String expectedFile = sourcePath.replace(".kt", ".expected");
        JetTestUtils.assertEqualsToFile(new File(expectedFile), stubTree);
    }

    @NotNull
    public static String serializeStubToString(@NotNull StubElement stubElement) {
        return DebugUtil.stubTreeToString(stubElement).replace(SpecialNames.SAFE_IDENTIFIER_FOR_NO_NAME.asString(), "<no name>");
    }
}
