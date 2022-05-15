/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.conversion.copy;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("idea/testData/copyPaste/plainTextLiteral")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class LiteralTextToKotlinCopyPasteTestGenerated extends AbstractLiteralTextToKotlinCopyPasteTest {
    public void testAllFilesPresentInPlainTextLiteral() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("idea/testData/copyPaste/plainTextLiteral"), Pattern.compile("^([^\\.]+)\\.txt$"), TargetBackend.ANY, true);
    }

    @TestMetadata("BrokenEntries.txt")
    public void testBrokenEntries() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/plainTextLiteral/BrokenEntries.txt");
        doTest(fileName);
    }

    @TestMetadata("MultiLine.txt")
    public void testMultiLine() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/plainTextLiteral/MultiLine.txt");
        doTest(fileName);
    }

    @TestMetadata("MultiLineToTripleQuotes.txt")
    public void testMultiLineToTripleQuotes() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/plainTextLiteral/MultiLineToTripleQuotes.txt");
        doTest(fileName);
    }

    @TestMetadata("MultiQuotesToTripleQuotes.txt")
    public void testMultiQuotesToTripleQuotes() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/plainTextLiteral/MultiQuotesToTripleQuotes.txt");
        doTest(fileName);
    }

    @TestMetadata("NoSpecialCharsToSingleQuote.txt")
    public void testNoSpecialCharsToSingleQuote() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/plainTextLiteral/NoSpecialCharsToSingleQuote.txt");
        doTest(fileName);
    }

    @TestMetadata("TrailingLines.txt")
    public void testTrailingLines() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/plainTextLiteral/TrailingLines.txt");
        doTest(fileName);
    }

    @TestMetadata("WithBackslashes.txt")
    public void testWithBackslashes() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/plainTextLiteral/WithBackslashes.txt");
        doTest(fileName);
    }

    @TestMetadata("WithDollarSignToTripleQuotes.txt")
    public void testWithDollarSignToTripleQuotes() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/plainTextLiteral/WithDollarSignToTripleQuotes.txt");
        doTest(fileName);
    }

    @TestMetadata("WithEntries.txt")
    public void testWithEntries() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/plainTextLiteral/WithEntries.txt");
        doTest(fileName);
    }

    @TestMetadata("WithQuotes.txt")
    public void testWithQuotes() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/copyPaste/plainTextLiteral/WithQuotes.txt");
        doTest(fileName);
    }
}
