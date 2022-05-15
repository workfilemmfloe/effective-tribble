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

package org.jetbrains.kotlin.js.test;

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
@TestMetadata("js/js.translator/testData/lineNumbers")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class JsLineNumberTestGenerated extends AbstractJsLineNumberTest {
    public void testAllFilesPresentInLineNumbers() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("js/js.translator/testData/lineNumbers"), Pattern.compile("^([^_](.+))\\.kt$"), TargetBackend.JS, true);
    }

    @TestMetadata("andAndWithSideEffect.kt")
    public void testAndAndWithSideEffect() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/andAndWithSideEffect.kt");
        doTest(fileName);
    }

    @TestMetadata("backingField.kt")
    public void testBackingField() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/backingField.kt");
        doTest(fileName);
    }

    @TestMetadata("catch.kt")
    public void testCatch() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/catch.kt");
        doTest(fileName);
    }

    @TestMetadata("chainedCall.kt")
    public void testChainedCall() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/chainedCall.kt");
        doTest(fileName);
    }

    @TestMetadata("classCapturingLocals.kt")
    public void testClassCapturingLocals() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/classCapturingLocals.kt");
        doTest(fileName);
    }

    @TestMetadata("closure.kt")
    public void testClosure() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/closure.kt");
        doTest(fileName);
    }

    @TestMetadata("complexExpressionAsDefaultArgument.kt")
    public void testComplexExpressionAsDefaultArgument() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/complexExpressionAsDefaultArgument.kt");
        doTest(fileName);
    }

    @TestMetadata("conditionalDecomposed.kt")
    public void testConditionalDecomposed() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/conditionalDecomposed.kt");
        doTest(fileName);
    }

    @TestMetadata("coroutine.kt")
    public void testCoroutine() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/coroutine.kt");
        doTest(fileName);
    }

    @TestMetadata("coroutineNullAssertion.kt")
    public void testCoroutineNullAssertion() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/coroutineNullAssertion.kt");
        doTest(fileName);
    }

    @TestMetadata("dataClass.kt")
    public void testDataClass() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/dataClass.kt");
        doTest(fileName);
    }

    @TestMetadata("delegateMemberVal.kt")
    public void testDelegateMemberVal() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/delegateMemberVal.kt");
        doTest(fileName);
    }

    @TestMetadata("delegatedProperty.kt")
    public void testDelegatedProperty() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/delegatedProperty.kt");
        doTest(fileName);
    }

    @TestMetadata("delegation.kt")
    public void testDelegation() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/delegation.kt");
        doTest(fileName);
    }

    @TestMetadata("destructuring.kt")
    public void testDestructuring() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/destructuring.kt");
        doTest(fileName);
    }

    @TestMetadata("destructuringInline.kt")
    public void testDestructuringInline() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/destructuringInline.kt");
        doTest(fileName);
    }

    @TestMetadata("doWhileWithComplexCondition.kt")
    public void testDoWhileWithComplexCondition() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/doWhileWithComplexCondition.kt");
        doTest(fileName);
    }

    @TestMetadata("elvis.kt")
    public void testElvis() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/elvis.kt");
        doTest(fileName);
    }

    @TestMetadata("enumCompanionObject.kt")
    public void testEnumCompanionObject() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/enumCompanionObject.kt");
        doTest(fileName);
    }

    @TestMetadata("enumObject.kt")
    public void testEnumObject() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/enumObject.kt");
        doTest(fileName);
    }

    @TestMetadata("expressionAsFunctionBody.kt")
    public void testExpressionAsFunctionBody() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/expressionAsFunctionBody.kt");
        doTest(fileName);
    }

    @TestMetadata("for.kt")
    public void testFor() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/for.kt");
        doTest(fileName);
    }

    @TestMetadata("increment.kt")
    public void testIncrement() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/increment.kt");
        doTest(fileName);
    }

    @TestMetadata("inlineArguments.kt")
    public void testInlineArguments() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/inlineArguments.kt");
        doTest(fileName);
    }

    @TestMetadata("inlineLocalVarsRef.kt")
    public void testInlineLocalVarsRef() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/inlineLocalVarsRef.kt");
        doTest(fileName);
    }

    @TestMetadata("inlineReturn.kt")
    public void testInlineReturn() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/inlineReturn.kt");
        doTest(fileName);
    }

    @TestMetadata("inlining.kt")
    public void testInlining() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/inlining.kt");
        doTest(fileName);
    }

    @TestMetadata("inliningWithLambda.kt")
    public void testInliningWithLambda() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/inliningWithLambda.kt");
        doTest(fileName);
    }

    @TestMetadata("innerClass.kt")
    public void testInnerClass() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/innerClass.kt");
        doTest(fileName);
    }

    @TestMetadata("isOperator.kt")
    public void testIsOperator() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/isOperator.kt");
        doTest(fileName);
    }

    @TestMetadata("jsCode.kt")
    public void testJsCode() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/jsCode.kt");
        doTest(fileName);
    }

    @TestMetadata("lambdaWithClosure.kt")
    public void testLambdaWithClosure() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/lambdaWithClosure.kt");
        doTest(fileName);
    }

    @TestMetadata("literals.kt")
    public void testLiterals() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/literals.kt");
        doTest(fileName);
    }

    @TestMetadata("longLiteral.kt")
    public void testLongLiteral() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/longLiteral.kt");
        doTest(fileName);
    }

    @TestMetadata("memberFunWithDefaultParam.kt")
    public void testMemberFunWithDefaultParam() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/memberFunWithDefaultParam.kt");
        doTest(fileName);
    }

    @TestMetadata("multipleReferences.kt")
    public void testMultipleReferences() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/multipleReferences.kt");
        doTest(fileName);
    }

    @TestMetadata("objectInstanceFunction.kt")
    public void testObjectInstanceFunction() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/objectInstanceFunction.kt");
        doTest(fileName);
    }

    @TestMetadata("optionalArgs.kt")
    public void testOptionalArgs() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/optionalArgs.kt");
        doTest(fileName);
    }

    @TestMetadata("propertyWithoutInitializer.kt")
    public void testPropertyWithoutInitializer() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/propertyWithoutInitializer.kt");
        doTest(fileName);
    }

    @TestMetadata("simple.kt")
    public void testSimple() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/simple.kt");
        doTest(fileName);
    }

    @TestMetadata("stringLiteral.kt")
    public void testStringLiteral() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/stringLiteral.kt");
        doTest(fileName);
    }

    @TestMetadata("syntheticCodeInConstructors.kt")
    public void testSyntheticCodeInConstructors() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/syntheticCodeInConstructors.kt");
        doTest(fileName);
    }

    @TestMetadata("syntheticCodeInEnums.kt")
    public void testSyntheticCodeInEnums() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/syntheticCodeInEnums.kt");
        doTest(fileName);
    }

    @TestMetadata("valParameter.kt")
    public void testValParameter() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/valParameter.kt");
        doTest(fileName);
    }

    @TestMetadata("whenEntryWithMultipleConditions.kt")
    public void testWhenEntryWithMultipleConditions() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/whenEntryWithMultipleConditions.kt");
        doTest(fileName);
    }

    @TestMetadata("whenIn.kt")
    public void testWhenIn() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/whenIn.kt");
        doTest(fileName);
    }

    @TestMetadata("whenIs.kt")
    public void testWhenIs() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/whenIs.kt");
        doTest(fileName);
    }

    @TestMetadata("whileWithComplexCondition.kt")
    public void testWhileWithComplexCondition() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/whileWithComplexCondition.kt");
        doTest(fileName);
    }

    @TestMetadata("js/js.translator/testData/lineNumbers/inlineMultiModule")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class InlineMultiModule extends AbstractJsLineNumberTest {
        public void testAllFilesPresentInInlineMultiModule() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("js/js.translator/testData/lineNumbers/inlineMultiModule"), Pattern.compile("^([^_](.+))\\.kt$"), TargetBackend.JS, true);
        }

        @TestMetadata("simple.kt")
        public void testSimple() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("js/js.translator/testData/lineNumbers/inlineMultiModule/simple.kt");
            doTest(fileName);
        }
    }
}
