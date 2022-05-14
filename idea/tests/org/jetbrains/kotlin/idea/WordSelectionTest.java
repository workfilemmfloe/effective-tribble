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

package org.jetbrains.kotlin.idea;

import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.CodeInsightTestUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class WordSelectionTest extends JetLightCodeInsightFixtureTestCase {
    public void testStatements() { doTest(); }

    public void testWhenEntries() { doTest(); }

    public void testTypeArguments() { doTest(); }

    public void testValueArguments() { doTest(); }

    public void testTypeParameters() { doTest(); }

    public void testValueParameters() { doTest(); }

    public void testDocComment() { doTest(); }

    public void testFunctionWithLineCommentBefore() { doTest(); }

    public void testFunctionWithLineCommentAfter() { doTest(); }

    public void testLineComment() { doTest(); }

    public void testSimpleComment() { doTest(); }

    public void testIfBody() { doTest(); }

    public void testCommentForStatements() { doTest(); }

    public void testSimpleStringLiteral() { doTest(); }
    public void testSimpleStringLiteral2() { doTest(); }

    public void testTemplateStringLiteral1() { doTest(); }
    public void testTemplateStringLiteral2() { doTest(); }
    public void testTemplateStringLiteral3() { doTest(); }

    public void testForRange() { doTest(); }

    public void testIfCondition() { doTest(); }

    private void doTest() {
        String dirName = getTestName(false);

        File dir = new File(myFixture.getTestDataPath() + dirName);
        int filesCount = dir.listFiles().length;
        String[] afterFiles = new String[filesCount - 1];
        for (int i = 1; i < filesCount; i++) {
            afterFiles[i - 1] = dirName + File.separator + i + ".kt";
        }

        CodeInsightTestUtil.doWordSelectionTest(myFixture, dirName + File.separator + "0.kt", afterFiles);
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JAVA_LATEST;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        String testRelativeDir = "wordSelection";
        myFixture.setTestDataPath(new File(PluginTestCaseBase.getTestDataPathBase(), testRelativeDir).getPath() +
                                  File.separator);
    }

}
