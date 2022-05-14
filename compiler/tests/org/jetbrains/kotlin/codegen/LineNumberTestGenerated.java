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

package org.jetbrains.kotlin.codegen;

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
@InnerTestClasses({
        LineNumberTestGenerated.LineNumber.class,
        LineNumberTestGenerated.Custom.class,
})
@RunWith(JUnit3RunnerWithInners.class)
public class LineNumberTestGenerated extends AbstractLineNumberTest {
    @TestMetadata("compiler/testData/lineNumber")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class LineNumber extends AbstractLineNumberTest {
        public void testAllFilesPresentInLineNumber() throws Exception {
            JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/lineNumber"), Pattern.compile("^(.+)\\.kt$"), false);
        }

        @TestMetadata("anonymousFunction.kt")
        public void testAnonymousFunction() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/anonymousFunction.kt");
            doTest(fileName);
        }

        @TestMetadata("class.kt")
        public void testClass() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/class.kt");
            doTest(fileName);
        }

        @TestMetadata("classObject.kt")
        public void testClassObject() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/classObject.kt");
            doTest(fileName);
        }

        @TestMetadata("defaultParameter.kt")
        public void testDefaultParameter() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/defaultParameter.kt");
            doTest(fileName);
        }

        @TestMetadata("enum.kt")
        public void testEnum() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/enum.kt");
            doTest(fileName);
        }

        @TestMetadata("for.kt")
        public void testFor() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/for.kt");
            doTest(fileName);
        }

        @TestMetadata("if.kt")
        public void testIf() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/if.kt");
            doTest(fileName);
        }

        @TestMetadata("inlineSimpleCall.kt")
        public void testInlineSimpleCall() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/inlineSimpleCall.kt");
            doTest(fileName);
        }

        @TestMetadata("localFunction.kt")
        public void testLocalFunction() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/localFunction.kt");
            doTest(fileName);
        }

        @TestMetadata("object.kt")
        public void testObject() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/object.kt");
            doTest(fileName);
        }

        @TestMetadata("propertyAccessor.kt")
        public void testPropertyAccessor() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/propertyAccessor.kt");
            doTest(fileName);
        }

        @TestMetadata("psvm.kt")
        public void testPsvm() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/psvm.kt");
            doTest(fileName);
        }

        @TestMetadata("simpleSmap.kt")
        public void testSimpleSmap() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/simpleSmap.kt");
            doTest(fileName);
        }

        @TestMetadata("topLevel.kt")
        public void testTopLevel() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/topLevel.kt");
            doTest(fileName);
        }

        @TestMetadata("trait.kt")
        public void testTrait() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/trait.kt");
            doTest(fileName);
        }

        @TestMetadata("tryCatch.kt")
        public void testTryCatch() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/tryCatch.kt");
            doTest(fileName);
        }

        @TestMetadata("while.kt")
        public void testWhile() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/while.kt");
            doTest(fileName);
        }
    }

    @TestMetadata("compiler/testData/lineNumber/custom")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Custom extends AbstractLineNumberTest {
        public void testAllFilesPresentInCustom() throws Exception {
            JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/lineNumber/custom"), Pattern.compile("^(.+)\\.kt$"), true);
        }

        @TestMetadata("callWithCallInArguments.kt")
        public void testCallWithCallInArguments() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/custom/callWithCallInArguments.kt");
            doTestCustom(fileName);
        }

        @TestMetadata("callWithReceiver.kt")
        public void testCallWithReceiver() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/custom/callWithReceiver.kt");
            doTestCustom(fileName);
        }

        @TestMetadata("chainCall.kt")
        public void testChainCall() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/custom/chainCall.kt");
            doTestCustom(fileName);
        }

        @TestMetadata("compileTimeConstant.kt")
        public void testCompileTimeConstant() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/custom/compileTimeConstant.kt");
            doTestCustom(fileName);
        }

        @TestMetadata("functionCallWithDefault.kt")
        public void testFunctionCallWithDefault() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/custom/functionCallWithDefault.kt");
            doTestCustom(fileName);
        }

        @TestMetadata("functionCallWithInlinedLambdaParam.kt")
        public void testFunctionCallWithInlinedLambdaParam() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/custom/functionCallWithInlinedLambdaParam.kt");
            doTestCustom(fileName);
        }

        @TestMetadata("functionCallWithLambdaParam.kt")
        public void testFunctionCallWithLambdaParam() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/custom/functionCallWithLambdaParam.kt");
            doTestCustom(fileName);
        }

        @TestMetadata("ifThen.kt")
        public void testIfThen() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/custom/ifThen.kt");
            doTestCustom(fileName);
        }

        @TestMetadata("ifThenElse.kt")
        public void testIfThenElse() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/custom/ifThenElse.kt");
            doTestCustom(fileName);
        }

        @TestMetadata("multilineFunctionCall.kt")
        public void testMultilineFunctionCall() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/custom/multilineFunctionCall.kt");
            doTestCustom(fileName);
        }

        @TestMetadata("multilineInfixCall.kt")
        public void testMultilineInfixCall() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/custom/multilineInfixCall.kt");
            doTestCustom(fileName);
        }

        @TestMetadata("tryCatchExpression.kt")
        public void testTryCatchExpression() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/custom/tryCatchExpression.kt");
            doTestCustom(fileName);
        }

        @TestMetadata("tryCatchFinally.kt")
        public void testTryCatchFinally() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/custom/tryCatchFinally.kt");
            doTestCustom(fileName);
        }

        @TestMetadata("tryFinally.kt")
        public void testTryFinally() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/custom/tryFinally.kt");
            doTestCustom(fileName);
        }

        @TestMetadata("when.kt")
        public void testWhen() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/custom/when.kt");
            doTestCustom(fileName);
        }

        @TestMetadata("whenSubject.kt")
        public void testWhenSubject() throws Exception {
            String fileName = JetTestUtils.navigationMetadata("compiler/testData/lineNumber/custom/whenSubject.kt");
            doTestCustom(fileName);
        }
    }
}