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

package org.jetbrains.kotlin.idea.debugger.evaluate;

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
@RunWith(JUnit3RunnerWithInners.class)
public class SelectExpressionForDebuggerTestGenerated extends AbstractSelectExpressionForDebuggerTest {
    @TestMetadata("idea/testData/debugger/selectExpression")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class SelectExpression extends AbstractSelectExpressionForDebuggerTest {
        public void testAllFilesPresentInSelectExpression() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("idea/testData/debugger/selectExpression"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, false);
        }

        @TestMetadata("annotation.kt")
        public void testAnnotation() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/annotation.kt");
            doTest(fileName);
        }

        @TestMetadata("binaryExpression.kt")
        public void testBinaryExpression() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/binaryExpression.kt");
            doTest(fileName);
        }

        @TestMetadata("call.kt")
        public void testCall() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/call.kt");
            doTest(fileName);
        }

        @TestMetadata("companionObjectCall.kt")
        public void testCompanionObjectCall() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/companionObjectCall.kt");
            doTest(fileName);
        }

        @TestMetadata("companionObjectCall2.kt")
        public void testCompanionObjectCall2() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/companionObjectCall2.kt");
            doTest(fileName);
        }

        @TestMetadata("expressionInPropertyInitializer.kt")
        public void testExpressionInPropertyInitializer() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/expressionInPropertyInitializer.kt");
            doTest(fileName);
        }

        @TestMetadata("extensionFun.kt")
        public void testExtensionFun() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/extensionFun.kt");
            doTest(fileName);
        }

        @TestMetadata("firstCallInChain.kt")
        public void testFirstCallInChain() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/firstCallInChain.kt");
            doTest(fileName);
        }

        @TestMetadata("fullyQualified.kt")
        public void testFullyQualified() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/fullyQualified.kt");
            doTest(fileName);
        }

        @TestMetadata("funArgument.kt")
        public void testFunArgument() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/funArgument.kt");
            doTest(fileName);
        }

        @TestMetadata("functionLiteral.kt")
        public void testFunctionLiteral() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/functionLiteral.kt");
            doTest(fileName);
        }

        @TestMetadata("getConvention.kt")
        public void testGetConvention() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/getConvention.kt");
            doTest(fileName);
        }

        @TestMetadata("imports.kt")
        public void testImports() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/imports.kt");
            doTest(fileName);
        }

        @TestMetadata("infixCall.kt")
        public void testInfixCall() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/infixCall.kt");
            doTest(fileName);
        }

        @TestMetadata("infixCallArgument.kt")
        public void testInfixCallArgument() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/infixCallArgument.kt");
            doTest(fileName);
        }

        @TestMetadata("isExpression.kt")
        public void testIsExpression() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/isExpression.kt");
            doTest(fileName);
        }

        @TestMetadata("javaStaticMehtodCall.kt")
        public void testJavaStaticMehtodCall() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/javaStaticMehtodCall.kt");
            doTest(fileName);
        }

        @TestMetadata("keyword.kt")
        public void testKeyword() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/keyword.kt");
            doTest(fileName);
        }

        @TestMetadata("modifier.kt")
        public void testModifier() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/modifier.kt");
            doTest(fileName);
        }

        @TestMetadata("nameArgument.kt")
        public void testNameArgument() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/nameArgument.kt");
            doTest(fileName);
        }

        @TestMetadata("objectMethodCall.kt")
        public void testObjectMethodCall() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/objectMethodCall.kt");
            doTest(fileName);
        }

        @TestMetadata("package.kt")
        public void testPackage() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/package.kt");
            doTest(fileName);
        }

        @TestMetadata("param.kt")
        public void testParam() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/param.kt");
            doTest(fileName);
        }

        @TestMetadata("propertyCall.kt")
        public void testPropertyCall() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/propertyCall.kt");
            doTest(fileName);
        }

        @TestMetadata("propertyDeclaration.kt")
        public void testPropertyDeclaration() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/propertyDeclaration.kt");
            doTest(fileName);
        }

        @TestMetadata("qualifiedExpressionProperty.kt")
        public void testQualifiedExpressionProperty() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/qualifiedExpressionProperty.kt");
            doTest(fileName);
        }

        @TestMetadata("qualifiedExpressionReceiver.kt")
        public void testQualifiedExpressionReceiver() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/qualifiedExpressionReceiver.kt");
            doTest(fileName);
        }

        @TestMetadata("qualifiedExpressionSelector.kt")
        public void testQualifiedExpressionSelector() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/qualifiedExpressionSelector.kt");
            doTest(fileName);
        }

        @TestMetadata("super.kt")
        public void testSuper() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/super.kt");
            doTest(fileName);
        }

        @TestMetadata("superSelector.kt")
        public void testSuperSelector() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/superSelector.kt");
            doTest(fileName);
        }

        @TestMetadata("this.kt")
        public void testThis() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/this.kt");
            doTest(fileName);
        }

        @TestMetadata("thisSelector.kt")
        public void testThisSelector() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/thisSelector.kt");
            doTest(fileName);
        }

        @TestMetadata("thisWithLabel.kt")
        public void testThisWithLabel() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/thisWithLabel.kt");
            doTest(fileName);
        }

        @TestMetadata("unaryExpression.kt")
        public void testUnaryExpression() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/unaryExpression.kt");
            doTest(fileName);
        }

        @TestMetadata("userType.kt")
        public void testUserType() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/userType.kt");
            doTest(fileName);
        }

        @TestMetadata("userTypeGeneric.kt")
        public void testUserTypeGeneric() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/userTypeGeneric.kt");
            doTest(fileName);
        }

        @TestMetadata("userTypeQualified.kt")
        public void testUserTypeQualified() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/userTypeQualified.kt");
            doTest(fileName);
        }
    }

    @TestMetadata("idea/testData/debugger/selectExpression/disallowMethodCalls")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class DisallowMethodCalls extends AbstractSelectExpressionForDebuggerTest {
        public void testAllFilesPresentInDisallowMethodCalls() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("idea/testData/debugger/selectExpression/disallowMethodCalls"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
        }

        @TestMetadata("binaryExpression.kt")
        public void testBinaryExpression() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/disallowMethodCalls/binaryExpression.kt");
            doTestWoMethodCalls(fileName);
        }

        @TestMetadata("call.kt")
        public void testCall() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/disallowMethodCalls/call.kt");
            doTestWoMethodCalls(fileName);
        }

        @TestMetadata("expressionInPropertyInitializer.kt")
        public void testExpressionInPropertyInitializer() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/disallowMethodCalls/expressionInPropertyInitializer.kt");
            doTestWoMethodCalls(fileName);
        }

        @TestMetadata("extensionFun.kt")
        public void testExtensionFun() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/disallowMethodCalls/extensionFun.kt");
            doTestWoMethodCalls(fileName);
        }

        @TestMetadata("funArgument.kt")
        public void testFunArgument() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/disallowMethodCalls/funArgument.kt");
            doTestWoMethodCalls(fileName);
        }

        @TestMetadata("functionLiteral.kt")
        public void testFunctionLiteral() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/disallowMethodCalls/functionLiteral.kt");
            doTestWoMethodCalls(fileName);
        }

        @TestMetadata("getConvention.kt")
        public void testGetConvention() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/disallowMethodCalls/getConvention.kt");
            doTestWoMethodCalls(fileName);
        }

        @TestMetadata("infixCall.kt")
        public void testInfixCall() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/disallowMethodCalls/infixCall.kt");
            doTestWoMethodCalls(fileName);
        }

        @TestMetadata("infixCallArgument.kt")
        public void testInfixCallArgument() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/disallowMethodCalls/infixCallArgument.kt");
            doTestWoMethodCalls(fileName);
        }

        @TestMetadata("isExpression.kt")
        public void testIsExpression() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/disallowMethodCalls/isExpression.kt");
            doTestWoMethodCalls(fileName);
        }

        @TestMetadata("propertyCall.kt")
        public void testPropertyCall() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/disallowMethodCalls/propertyCall.kt");
            doTestWoMethodCalls(fileName);
        }

        @TestMetadata("qualifiedExpressionProperty.kt")
        public void testQualifiedExpressionProperty() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/disallowMethodCalls/qualifiedExpressionProperty.kt");
            doTestWoMethodCalls(fileName);
        }

        @TestMetadata("qualifiedExpressionReceiver.kt")
        public void testQualifiedExpressionReceiver() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/disallowMethodCalls/qualifiedExpressionReceiver.kt");
            doTestWoMethodCalls(fileName);
        }

        @TestMetadata("qualifiedExpressionSelector.kt")
        public void testQualifiedExpressionSelector() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/disallowMethodCalls/qualifiedExpressionSelector.kt");
            doTestWoMethodCalls(fileName);
        }

        @TestMetadata("super.kt")
        public void testSuper() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/disallowMethodCalls/super.kt");
            doTestWoMethodCalls(fileName);
        }

        @TestMetadata("superSelector.kt")
        public void testSuperSelector() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/disallowMethodCalls/superSelector.kt");
            doTestWoMethodCalls(fileName);
        }

        @TestMetadata("this.kt")
        public void testThis() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/disallowMethodCalls/this.kt");
            doTestWoMethodCalls(fileName);
        }

        @TestMetadata("thisSelector.kt")
        public void testThisSelector() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/disallowMethodCalls/thisSelector.kt");
            doTestWoMethodCalls(fileName);
        }

        @TestMetadata("thisWithLabel.kt")
        public void testThisWithLabel() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/disallowMethodCalls/thisWithLabel.kt");
            doTestWoMethodCalls(fileName);
        }

        @TestMetadata("unaryExpression.kt")
        public void testUnaryExpression() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/testData/debugger/selectExpression/disallowMethodCalls/unaryExpression.kt");
            doTestWoMethodCalls(fileName);
        }
    }
}
