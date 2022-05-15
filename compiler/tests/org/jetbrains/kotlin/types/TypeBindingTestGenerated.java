/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types;

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
@TestMetadata("compiler/testData/type/binding")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class TypeBindingTestGenerated extends AbstractTypeBindingTest {
    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doTest, TargetBackend.ANY, testDataFilePath);
    }

    public void testAllFilesPresentInBinding() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/type/binding"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
    }

    @TestMetadata("compiler/testData/type/binding/explicit")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Explicit extends AbstractTypeBindingTest {
        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, TargetBackend.ANY, testDataFilePath);
        }

        public void testAllFilesPresentInExplicit() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/type/binding/explicit"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
        }

        @TestMetadata("conflictingProjection.kt")
        public void testConflictingProjection() throws Exception {
            runTest("compiler/testData/type/binding/explicit/conflictingProjection.kt");
        }

        @TestMetadata("conflictingProjection2.kt")
        public void testConflictingProjection2() throws Exception {
            runTest("compiler/testData/type/binding/explicit/conflictingProjection2.kt");
        }

        @TestMetadata("errorPair.kt")
        public void testErrorPair() throws Exception {
            runTest("compiler/testData/type/binding/explicit/errorPair.kt");
        }

        @TestMetadata("errorWithProjection.kt")
        public void testErrorWithProjection() throws Exception {
            runTest("compiler/testData/type/binding/explicit/errorWithProjection.kt");
        }

        @TestMetadata("functionType.kt")
        public void testFunctionType() throws Exception {
            runTest("compiler/testData/type/binding/explicit/functionType.kt");
        }

        @TestMetadata("functionType2.kt")
        public void testFunctionType2() throws Exception {
            runTest("compiler/testData/type/binding/explicit/functionType2.kt");
        }

        @TestMetadata("functionType3.kt")
        public void testFunctionType3() throws Exception {
            runTest("compiler/testData/type/binding/explicit/functionType3.kt");
        }

        @TestMetadata("inProjection.kt")
        public void testInProjection() throws Exception {
            runTest("compiler/testData/type/binding/explicit/inProjection.kt");
        }

        @TestMetadata("int.kt")
        public void testInt() throws Exception {
            runTest("compiler/testData/type/binding/explicit/int.kt");
        }

        @TestMetadata("list0.kt")
        public void testList0() throws Exception {
            runTest("compiler/testData/type/binding/explicit/list0.kt");
        }

        @TestMetadata("list2.kt")
        public void testList2() throws Exception {
            runTest("compiler/testData/type/binding/explicit/list2.kt");
        }

        @TestMetadata("nullableType.kt")
        public void testNullableType() throws Exception {
            runTest("compiler/testData/type/binding/explicit/nullableType.kt");
        }

        @TestMetadata("outProjection.kt")
        public void testOutProjection() throws Exception {
            runTest("compiler/testData/type/binding/explicit/outProjection.kt");
        }

        @TestMetadata("pair.kt")
        public void testPair() throws Exception {
            runTest("compiler/testData/type/binding/explicit/pair.kt");
        }

        @TestMetadata("simple.kt")
        public void testSimple() throws Exception {
            runTest("compiler/testData/type/binding/explicit/simple.kt");
        }

        @TestMetadata("star.kt")
        public void testStar() throws Exception {
            runTest("compiler/testData/type/binding/explicit/star.kt");
        }

        @TestMetadata("typeWithBracket.kt")
        public void testTypeWithBracket() throws Exception {
            runTest("compiler/testData/type/binding/explicit/typeWithBracket.kt");
        }

        @TestMetadata("unresolvedType.kt")
        public void testUnresolvedType() throws Exception {
            runTest("compiler/testData/type/binding/explicit/unresolvedType.kt");
        }
    }

    @TestMetadata("compiler/testData/type/binding/implicit")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Implicit extends AbstractTypeBindingTest {
        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, TargetBackend.ANY, testDataFilePath);
        }

        public void testAllFilesPresentInImplicit() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/type/binding/implicit"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
        }

        @TestMetadata("conflictingProjection.kt")
        public void testConflictingProjection() throws Exception {
            runTest("compiler/testData/type/binding/implicit/conflictingProjection.kt");
        }

        @TestMetadata("conflictingProjection2.kt")
        public void testConflictingProjection2() throws Exception {
            runTest("compiler/testData/type/binding/implicit/conflictingProjection2.kt");
        }

        @TestMetadata("errorPair.kt")
        public void testErrorPair() throws Exception {
            runTest("compiler/testData/type/binding/implicit/errorPair.kt");
        }

        @TestMetadata("errorWithProjection.kt")
        public void testErrorWithProjection() throws Exception {
            runTest("compiler/testData/type/binding/implicit/errorWithProjection.kt");
        }

        @TestMetadata("functionType.kt")
        public void testFunctionType() throws Exception {
            runTest("compiler/testData/type/binding/implicit/functionType.kt");
        }

        @TestMetadata("functionType2.kt")
        public void testFunctionType2() throws Exception {
            runTest("compiler/testData/type/binding/implicit/functionType2.kt");
        }

        @TestMetadata("functionType3.kt")
        public void testFunctionType3() throws Exception {
            runTest("compiler/testData/type/binding/implicit/functionType3.kt");
        }

        @TestMetadata("inProjection.kt")
        public void testInProjection() throws Exception {
            runTest("compiler/testData/type/binding/implicit/inProjection.kt");
        }

        @TestMetadata("int.kt")
        public void testInt() throws Exception {
            runTest("compiler/testData/type/binding/implicit/int.kt");
        }

        @TestMetadata("list0.kt")
        public void testList0() throws Exception {
            runTest("compiler/testData/type/binding/implicit/list0.kt");
        }

        @TestMetadata("list2.kt")
        public void testList2() throws Exception {
            runTest("compiler/testData/type/binding/implicit/list2.kt");
        }

        @TestMetadata("nullableType.kt")
        public void testNullableType() throws Exception {
            runTest("compiler/testData/type/binding/implicit/nullableType.kt");
        }

        @TestMetadata("outProjection.kt")
        public void testOutProjection() throws Exception {
            runTest("compiler/testData/type/binding/implicit/outProjection.kt");
        }

        @TestMetadata("pair.kt")
        public void testPair() throws Exception {
            runTest("compiler/testData/type/binding/implicit/pair.kt");
        }

        @TestMetadata("simple.kt")
        public void testSimple() throws Exception {
            runTest("compiler/testData/type/binding/implicit/simple.kt");
        }

        @TestMetadata("star.kt")
        public void testStar() throws Exception {
            runTest("compiler/testData/type/binding/implicit/star.kt");
        }

        @TestMetadata("typeWithBracket.kt")
        public void testTypeWithBracket() throws Exception {
            runTest("compiler/testData/type/binding/implicit/typeWithBracket.kt");
        }

        @TestMetadata("unresolvedType.kt")
        public void testUnresolvedType() throws Exception {
            runTest("compiler/testData/type/binding/implicit/unresolvedType.kt");
        }
    }
}
