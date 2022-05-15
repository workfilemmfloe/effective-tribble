/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight;

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
@TestMetadata("idea/testData/multiFileInspections")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class MultiFileInspectionTestGenerated extends AbstractMultiFileInspectionTest {
    public void testAllFilesPresentInMultiFileInspections() throws Exception {
        KotlinTestUtils.assertAllTestsPresentInSingleGeneratedClass(this.getClass(), new File("idea/testData/multiFileInspections"), Pattern.compile("^(.+)\\.test$"), TargetBackend.ANY);
    }

    @TestMetadata("fakeJvmFieldConstant/fakeJvmFieldConstant.test")
    public void testFakeJvmFieldConstant_FakeJvmFieldConstant() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/multiFileInspections/fakeJvmFieldConstant/fakeJvmFieldConstant.test");
        doTest(fileName);
    }

    @TestMetadata("invalidBundleOrProperty/invalidBundleOrProperty.test")
    public void testInvalidBundleOrProperty_InvalidBundleOrProperty() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/multiFileInspections/invalidBundleOrProperty/invalidBundleOrProperty.test");
        doTest(fileName);
    }

    @TestMetadata("kotlinInternalInJava/kotlinInternalInJava.test")
    public void testKotlinInternalInJava_KotlinInternalInJava() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/multiFileInspections/kotlinInternalInJava/kotlinInternalInJava.test");
        doTest(fileName);
    }

    @TestMetadata("mainInTwoModules/mainInTwoModules.test")
    public void testMainInTwoModules_MainInTwoModules() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/multiFileInspections/mainInTwoModules/mainInTwoModules.test");
        doTest(fileName);
    }

    @TestMetadata("mismatchedProjectAndDirectory/mismatchedProjectAndDirectory.test")
    public void testMismatchedProjectAndDirectory_MismatchedProjectAndDirectory() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/multiFileInspections/mismatchedProjectAndDirectory/mismatchedProjectAndDirectory.test");
        doTest(fileName);
    }

    @TestMetadata("platformExtensionReceiverOfInline/platformExtensionReceiverOfInline.test")
    public void testPlatformExtensionReceiverOfInline_PlatformExtensionReceiverOfInline() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/multiFileInspections/platformExtensionReceiverOfInline/platformExtensionReceiverOfInline.test");
        doTest(fileName);
    }
}
