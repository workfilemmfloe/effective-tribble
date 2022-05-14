/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.checkers;

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
@TestMetadata("compiler/testData/foreignAnnotations/tests")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class ForeignAnnotationsTestGenerated extends AbstractForeignAnnotationsTest {
    public void testAllFilesPresentInTests() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/foreignAnnotations/tests"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
    }

    @TestMetadata("android.kt")
    public void testAndroid() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/foreignAnnotations/tests/android.kt");
        doTest(fileName);
    }

    @TestMetadata("aosp.kt")
    public void testAosp() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/foreignAnnotations/tests/aosp.kt");
        doTest(fileName);
    }

    @TestMetadata("eclipse.kt")
    public void testEclipse() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/foreignAnnotations/tests/eclipse.kt");
        doTest(fileName);
    }

    @TestMetadata("findBugsSimple.kt")
    public void testFindBugsSimple() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/foreignAnnotations/tests/findBugsSimple.kt");
        doTest(fileName);
    }

    @TestMetadata("jsr305Simple.kt")
    public void testJsr305Simple() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/foreignAnnotations/tests/jsr305Simple.kt");
        doTest(fileName);
    }

    @TestMetadata("jsr305Strange.kt")
    public void testJsr305Strange() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/foreignAnnotations/tests/jsr305Strange.kt");
        doTest(fileName);
    }

    @TestMetadata("lombokSimple.kt")
    public void testLombokSimple() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/foreignAnnotations/tests/lombokSimple.kt");
        doTest(fileName);
    }

    @TestMetadata("rxjava.kt")
    public void testRxjava() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/foreignAnnotations/tests/rxjava.kt");
        doTest(fileName);
    }
}
