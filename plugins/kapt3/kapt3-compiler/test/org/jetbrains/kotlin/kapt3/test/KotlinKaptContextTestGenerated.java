/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.test;

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
@TestMetadata("plugins/kapt3/kapt3-compiler/testData/kotlinRunner")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class KotlinKaptContextTestGenerated extends AbstractKotlinKaptContextTest {
    public void testAllFilesPresentInKotlinRunner() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("plugins/kapt3/kapt3-compiler/testData/kotlinRunner"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
    }

    @TestMetadata("NestedClasses.kt")
    public void testNestedClasses() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("plugins/kapt3/kapt3-compiler/testData/kotlinRunner/NestedClasses.kt");
        doTest(fileName);
    }

    @TestMetadata("Overloads.kt")
    public void testOverloads() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("plugins/kapt3/kapt3-compiler/testData/kotlinRunner/Overloads.kt");
        doTest(fileName);
    }

    @TestMetadata("Simple.kt")
    public void testSimple() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("plugins/kapt3/kapt3-compiler/testData/kotlinRunner/Simple.kt");
        doTest(fileName);
    }
}
