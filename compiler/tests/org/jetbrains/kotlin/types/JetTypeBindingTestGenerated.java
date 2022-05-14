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

package org.jetbrains.kotlin.types;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.InnerTestClasses;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler/testData/type/binding")
@TestDataPath("$PROJECT_ROOT")
@InnerTestClasses({
        JetTypeBindingTestGenerated.Explicit.class,
        JetTypeBindingTestGenerated.Implicit.class,
})
@RunWith(JUnit3RunnerWithInners.class)
public class JetTypeBindingTestGenerated extends AbstractJetTypeBindingTest {
    public void testAllFilesPresentInBinding() throws Exception {
        JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/type/binding"), Pattern.compile("^(.+)\\.kt$"), true);
    }

    @TestMetadata("compiler/testData/type/binding/explicit")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Explicit extends AbstractJetTypeBindingTest {
        public void testAllFilesPresentInExplicit() throws Exception {
            JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/type/binding/explicit"), Pattern.compile("^(.+)\\.kt$"), true);
        }

        @TestMetadata("conflictingProjection.kt")
        public void testConflictingProjection() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/explicit/conflictingProjection.kt");
            doTest(fileName);
        }

        @TestMetadata("conflictingProjection2.kt")
        public void testConflictingProjection2() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/explicit/conflictingProjection2.kt");
            doTest(fileName);
        }

        @TestMetadata("errorPair.kt")
        public void testErrorPair() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/explicit/errorPair.kt");
            doTest(fileName);
        }

        @TestMetadata("errorWithProjection.kt")
        public void testErrorWithProjection() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/explicit/errorWithProjection.kt");
            doTest(fileName);
        }

        @TestMetadata("functionType.kt")
        public void testFunctionType() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/explicit/functionType.kt");
            doTest(fileName);
        }

        @TestMetadata("functionType2.kt")
        public void testFunctionType2() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/explicit/functionType2.kt");
            doTest(fileName);
        }

        @TestMetadata("functionType3.kt")
        public void testFunctionType3() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/explicit/functionType3.kt");
            doTest(fileName);
        }

        @TestMetadata("inProjection.kt")
        public void testInProjection() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/explicit/inProjection.kt");
            doTest(fileName);
        }

        @TestMetadata("int.kt")
        public void testInt() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/explicit/int.kt");
            doTest(fileName);
        }

        @TestMetadata("list0.kt")
        public void testList0() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/explicit/list0.kt");
            doTest(fileName);
        }

        @TestMetadata("list2.kt")
        public void testList2() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/explicit/list2.kt");
            doTest(fileName);
        }

        @TestMetadata("nullableType.kt")
        public void testNullableType() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/explicit/nullableType.kt");
            doTest(fileName);
        }

        @TestMetadata("outProjection.kt")
        public void testOutProjection() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/explicit/outProjection.kt");
            doTest(fileName);
        }

        @TestMetadata("pair.kt")
        public void testPair() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/explicit/pair.kt");
            doTest(fileName);
        }

        @TestMetadata("simple.kt")
        public void testSimple() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/explicit/simple.kt");
            doTest(fileName);
        }

        @TestMetadata("star.kt")
        public void testStar() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/explicit/star.kt");
            doTest(fileName);
        }

        @TestMetadata("typeWithBracket.kt")
        public void testTypeWithBracket() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/explicit/typeWithBracket.kt");
            doTest(fileName);
        }

        @TestMetadata("unresolvedType.kt")
        public void testUnresolvedType() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/explicit/unresolvedType.kt");
            doTest(fileName);
        }
    }

    @TestMetadata("compiler/testData/type/binding/implicit")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Implicit extends AbstractJetTypeBindingTest {
        public void testAllFilesPresentInImplicit() throws Exception {
            JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/type/binding/implicit"), Pattern.compile("^(.+)\\.kt$"), true);
        }

        @TestMetadata("conflictingProjection.kt")
        public void testConflictingProjection() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/implicit/conflictingProjection.kt");
            doTest(fileName);
        }

        @TestMetadata("conflictingProjection2.kt")
        public void testConflictingProjection2() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/implicit/conflictingProjection2.kt");
            doTest(fileName);
        }

        @TestMetadata("errorPair.kt")
        public void testErrorPair() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/implicit/errorPair.kt");
            doTest(fileName);
        }

        @TestMetadata("errorWithProjection.kt")
        public void testErrorWithProjection() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/implicit/errorWithProjection.kt");
            doTest(fileName);
        }

        @TestMetadata("functionType.kt")
        public void testFunctionType() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/implicit/functionType.kt");
            doTest(fileName);
        }

        @TestMetadata("functionType2.kt")
        public void testFunctionType2() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/implicit/functionType2.kt");
            doTest(fileName);
        }

        @TestMetadata("functionType3.kt")
        public void testFunctionType3() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/implicit/functionType3.kt");
            doTest(fileName);
        }

        @TestMetadata("inProjection.kt")
        public void testInProjection() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/implicit/inProjection.kt");
            doTest(fileName);
        }

        @TestMetadata("int.kt")
        public void testInt() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/implicit/int.kt");
            doTest(fileName);
        }

        @TestMetadata("list0.kt")
        public void testList0() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/implicit/list0.kt");
            doTest(fileName);
        }

        @TestMetadata("list2.kt")
        public void testList2() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/implicit/list2.kt");
            doTest(fileName);
        }

        @TestMetadata("nullableType.kt")
        public void testNullableType() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/implicit/nullableType.kt");
            doTest(fileName);
        }

        @TestMetadata("outProjection.kt")
        public void testOutProjection() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/implicit/outProjection.kt");
            doTest(fileName);
        }

        @TestMetadata("pair.kt")
        public void testPair() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/implicit/pair.kt");
            doTest(fileName);
        }

        @TestMetadata("simple.kt")
        public void testSimple() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/implicit/simple.kt");
            doTest(fileName);
        }

        @TestMetadata("star.kt")
        public void testStar() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/implicit/star.kt");
            doTest(fileName);
        }

        @TestMetadata("typeWithBracket.kt")
        public void testTypeWithBracket() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/implicit/typeWithBracket.kt");
            doTest(fileName);
        }

        @TestMetadata("unresolvedType.kt")
        public void testUnresolvedType() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/type/binding/implicit/unresolvedType.kt");
            doTest(fileName);
        }
    }
}