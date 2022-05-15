/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test;

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
@TestMetadata("idea/idea-completion/testData/keywords")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class KeywordCompletionTestGenerated extends AbstractKeywordCompletionTest {
    @TestMetadata("AfterClassProperty.kt")
    public void testAfterClassProperty() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/AfterClassProperty.kt");
        doTest(fileName);
    }

    @TestMetadata("AfterClasses.kt")
    public void testAfterClasses() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/AfterClasses.kt");
        doTest(fileName);
    }

    @TestMetadata("AfterClasses_LangLevel10.kt")
    public void testAfterClasses_LangLevel10() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/AfterClasses_LangLevel10.kt");
        doTest(fileName);
    }

    @TestMetadata("AfterClasses_LangLevel11.kt")
    public void testAfterClasses_LangLevel11() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/AfterClasses_LangLevel11.kt");
        doTest(fileName);
    }

    @TestMetadata("AfterDot.kt")
    public void testAfterDot() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/AfterDot.kt");
        doTest(fileName);
    }

    @TestMetadata("AfterFuns.kt")
    public void testAfterFuns() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/AfterFuns.kt");
        doTest(fileName);
    }

    @TestMetadata("AfterIf.kt")
    public void testAfterIf() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/AfterIf.kt");
        doTest(fileName);
    }

    @TestMetadata("AfterSafeDot.kt")
    public void testAfterSafeDot() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/AfterSafeDot.kt");
        doTest(fileName);
    }

    @TestMetadata("AfterSpaceAndDot.kt")
    public void testAfterSpaceAndDot() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/AfterSpaceAndDot.kt");
        doTest(fileName);
    }

    @TestMetadata("AfterTry.kt")
    public void testAfterTry() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/AfterTry.kt");
        doTest(fileName);
    }

    @TestMetadata("AfterTryCatch.kt")
    public void testAfterTryCatch() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/AfterTryCatch.kt");
        doTest(fileName);
    }

    @TestMetadata("AfterTryFinally.kt")
    public void testAfterTryFinally() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/AfterTryFinally.kt");
        doTest(fileName);
    }

    @TestMetadata("AfterTryInAssignment.kt")
    public void testAfterTryInAssignment() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/AfterTryInAssignment.kt");
        doTest(fileName);
    }

    public void testAllFilesPresentInKeywords() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("idea/idea-completion/testData/keywords"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, false);
    }

    @TestMetadata("BeforeClass.kt")
    public void testBeforeClass() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/BeforeClass.kt");
        doTest(fileName);
    }

    @TestMetadata("BeforeDelegationCall.kt")
    public void testBeforeDelegationCall() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/BeforeDelegationCall.kt");
        doTest(fileName);
    }

    @TestMetadata("BreakContinue.kt")
    public void testBreakContinue() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/BreakContinue.kt");
        doTest(fileName);
    }

    @TestMetadata("BreakWithLabel.kt")
    public void testBreakWithLabel() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/BreakWithLabel.kt");
        doTest(fileName);
    }

    @TestMetadata("CommaExpected.kt")
    public void testCommaExpected() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/CommaExpected.kt");
        doTest(fileName);
    }

    @TestMetadata("CompanionObjectBeforeObject.kt")
    public void testCompanionObjectBeforeObject() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/CompanionObjectBeforeObject.kt");
        doTest(fileName);
    }

    @TestMetadata("ContinueWithLabel.kt")
    public void testContinueWithLabel() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/ContinueWithLabel.kt");
        doTest(fileName);
    }

    @TestMetadata("Else1.kt")
    public void testElse1() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/Else1.kt");
        doTest(fileName);
    }

    @TestMetadata("Else2.kt")
    public void testElse2() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/Else2.kt");
        doTest(fileName);
    }

    @TestMetadata("FileKeyword.kt")
    public void testFileKeyword() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/FileKeyword.kt");
        doTest(fileName);
    }

    @TestMetadata("GlobalPropertyAccessors.kt")
    public void testGlobalPropertyAccessors() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/GlobalPropertyAccessors.kt");
        doTest(fileName);
    }

    @TestMetadata("IfTry.kt")
    public void testIfTry() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/IfTry.kt");
        doTest(fileName);
    }

    @TestMetadata("IfTryCatch.kt")
    public void testIfTryCatch() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/IfTryCatch.kt");
        doTest(fileName);
    }

    @TestMetadata("InAnnotationClassScope.kt")
    public void testInAnnotationClassScope() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InAnnotationClassScope.kt");
        doTest(fileName);
    }

    @TestMetadata("InArgumentList.kt")
    public void testInArgumentList() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InArgumentList.kt");
        doTest(fileName);
    }

    @TestMetadata("InBlockComment.kt")
    public void testInBlockComment() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InBlockComment.kt");
        doTest(fileName);
    }

    @TestMetadata("InChar.kt")
    public void testInChar() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InChar.kt");
        doTest(fileName);
    }

    @TestMetadata("InClassBeforeFun.kt")
    public void testInClassBeforeFun() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InClassBeforeFun.kt");
        doTest(fileName);
    }

    @TestMetadata("InClassNoCompletionInValName.kt")
    public void testInClassNoCompletionInValName() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InClassNoCompletionInValName.kt");
        doTest(fileName);
    }

    @TestMetadata("InClassProperty.kt")
    public void testInClassProperty() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InClassProperty.kt");
        doTest(fileName);
    }

    @TestMetadata("InClassScope.kt")
    public void testInClassScope() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InClassScope.kt");
        doTest(fileName);
    }

    @TestMetadata("InClassTypeParameters.kt")
    public void testInClassTypeParameters() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InClassTypeParameters.kt");
        doTest(fileName);
    }

    @TestMetadata("InCodeBlock.kt")
    public void testInCodeBlock() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InCodeBlock.kt");
        doTest(fileName);
    }

    @TestMetadata("InElse.kt")
    public void testInElse() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InElse.kt");
        doTest(fileName);
    }

    @TestMetadata("InEnumScope1.kt")
    public void testInEnumScope1() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InEnumScope1.kt");
        doTest(fileName);
    }

    @TestMetadata("InEnumScope2.kt")
    public void testInEnumScope2() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InEnumScope2.kt");
        doTest(fileName);
    }

    @TestMetadata("InFunctionExpressionBody.kt")
    public void testInFunctionExpressionBody() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InFunctionExpressionBody.kt");
        doTest(fileName);
    }

    @TestMetadata("InFunctionName.kt")
    public void testInFunctionName() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InFunctionName.kt");
        doTest(fileName);
    }

    @TestMetadata("InFunctionRecieverType.kt")
    public void testInFunctionRecieverType() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InFunctionRecieverType.kt");
        doTest(fileName);
    }

    @TestMetadata("InFunctionTypePosition.kt")
    public void testInFunctionTypePosition() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InFunctionTypePosition.kt");
        doTest(fileName);
    }

    @TestMetadata("InGetterExpressionBody.kt")
    public void testInGetterExpressionBody() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InGetterExpressionBody.kt");
        doTest(fileName);
    }

    @TestMetadata("InIf.kt")
    public void testInIf() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InIf.kt");
        doTest(fileName);
    }

    @TestMetadata("InInterfaceScope.kt")
    public void testInInterfaceScope() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InInterfaceScope.kt");
        doTest(fileName);
    }

    @TestMetadata("InMemberFunParametersList.kt")
    public void testInMemberFunParametersList() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InMemberFunParametersList.kt");
        doTest(fileName);
    }

    @TestMetadata("InModifierListInsideClass.kt")
    public void testInModifierListInsideClass() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InModifierListInsideClass.kt");
        doTest(fileName);
    }

    @TestMetadata("InNotFinishedGenericWithFunAfter.kt")
    public void testInNotFinishedGenericWithFunAfter() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InNotFinishedGenericWithFunAfter.kt");
        doTest(fileName);
    }

    @TestMetadata("InObjectScope.kt")
    public void testInObjectScope() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InObjectScope.kt");
        doTest(fileName);
    }

    @TestMetadata("InParameterDefaultValue.kt")
    public void testInParameterDefaultValue() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InParameterDefaultValue.kt");
        doTest(fileName);
    }

    @TestMetadata("InPrimaryConstructorParametersList.kt")
    public void testInPrimaryConstructorParametersList() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InPrimaryConstructorParametersList.kt");
        doTest(fileName);
    }

    @TestMetadata("InPropertyInitializer.kt")
    public void testInPropertyInitializer() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InPropertyInitializer.kt");
        doTest(fileName);
    }

    @TestMetadata("InPropertyTypeReference.kt")
    public void testInPropertyTypeReference() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InPropertyTypeReference.kt");
        doTest(fileName);
    }

    @TestMetadata("InString.kt")
    public void testInString() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InString.kt");
        doTest(fileName);
    }

    @TestMetadata("InTopFunParametersList.kt")
    public void testInTopFunParametersList() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InTopFunParametersList.kt");
        doTest(fileName);
    }

    @TestMetadata("InTopScopeAfterPackage.kt")
    public void testInTopScopeAfterPackage() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InTopScopeAfterPackage.kt");
        doTest(fileName);
    }

    @TestMetadata("InTypePosition.kt")
    public void testInTypePosition() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/InTypePosition.kt");
        doTest(fileName);
    }

    @TestMetadata("LabeledLambdaThis.kt")
    public void testLabeledLambdaThis() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/LabeledLambdaThis.kt");
        doTest(fileName);
    }

    @TestMetadata("LineComment.kt")
    public void testLineComment() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/LineComment.kt");
        doTest(fileName);
    }

    @TestMetadata("NoBreak1.kt")
    public void testNoBreak1() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/NoBreak1.kt");
        doTest(fileName);
    }

    @TestMetadata("NoBreak2.kt")
    public void testNoBreak2() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/NoBreak2.kt");
        doTest(fileName);
    }

    @TestMetadata("NoCompanionThis.kt")
    public void testNoCompanionThis() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/NoCompanionThis.kt");
        doTest(fileName);
    }

    @TestMetadata("NoCompletionForCapitalPrefix.kt")
    public void testNoCompletionForCapitalPrefix() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/NoCompletionForCapitalPrefix.kt");
        doTest(fileName);
    }

    @TestMetadata("NoContinue.kt")
    public void testNoContinue() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/NoContinue.kt");
        doTest(fileName);
    }

    @TestMetadata("NoFinalInParameterList.kt")
    public void testNoFinalInParameterList() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/NoFinalInParameterList.kt");
        doTest(fileName);
    }

    @TestMetadata("NotInNotIs.kt")
    public void testNotInNotIs() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/NotInNotIs.kt");
        doTest(fileName);
    }

    @TestMetadata("NotInNotIs2.kt")
    public void testNotInNotIs2() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/NotInNotIs2.kt");
        doTest(fileName);
    }

    @TestMetadata("PrefixMatcher.kt")
    public void testPrefixMatcher() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/PrefixMatcher.kt");
        doTest(fileName);
    }

    @TestMetadata("PropertyAccessors.kt")
    public void testPropertyAccessors() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/PropertyAccessors.kt");
        doTest(fileName);
    }

    @TestMetadata("PropertyAccessors2.kt")
    public void testPropertyAccessors2() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/PropertyAccessors2.kt");
        doTest(fileName);
    }

    @TestMetadata("PropertySetter.kt")
    public void testPropertySetter() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/PropertySetter.kt");
        doTest(fileName);
    }

    @TestMetadata("QualifiedThis.kt")
    public void testQualifiedThis() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/QualifiedThis.kt");
        doTest(fileName);
    }

    @TestMetadata("QualifiedThisInAccessor.kt")
    public void testQualifiedThisInAccessor() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/QualifiedThisInAccessor.kt");
        doTest(fileName);
    }

    @TestMetadata("Receiver.kt")
    public void testReceiver() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/Receiver.kt");
        doTest(fileName);
    }

    @TestMetadata("Return1.kt")
    public void testReturn1() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/Return1.kt");
        doTest(fileName);
    }

    @TestMetadata("Return2.kt")
    public void testReturn2() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/Return2.kt");
        doTest(fileName);
    }

    @TestMetadata("Return3.kt")
    public void testReturn3() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/Return3.kt");
        doTest(fileName);
    }

    @TestMetadata("Return4.kt")
    public void testReturn4() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/Return4.kt");
        doTest(fileName);
    }

    @TestMetadata("Return5.kt")
    public void testReturn5() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/Return5.kt");
        doTest(fileName);
    }

    @TestMetadata("Return6.kt")
    public void testReturn6() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/Return6.kt");
        doTest(fileName);
    }

    @TestMetadata("Return7.kt")
    public void testReturn7() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/Return7.kt");
        doTest(fileName);
    }

    @TestMetadata("Return8.kt")
    public void testReturn8() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/Return8.kt");
        doTest(fileName);
    }

    @TestMetadata("Return9.kt")
    public void testReturn9() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/Return9.kt");
        doTest(fileName);
    }

    @TestMetadata("ReturnBoolean.kt")
    public void testReturnBoolean() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/ReturnBoolean.kt");
        doTest(fileName);
    }

    @TestMetadata("ReturnCollection.kt")
    public void testReturnCollection() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/ReturnCollection.kt");
        doTest(fileName);
    }

    @TestMetadata("ReturnIterable.kt")
    public void testReturnIterable() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/ReturnIterable.kt");
        doTest(fileName);
    }

    @TestMetadata("ReturnKeywordName.kt")
    public void testReturnKeywordName() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/ReturnKeywordName.kt");
        doTest(fileName);
    }

    @TestMetadata("ReturnList.kt")
    public void testReturnList() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/ReturnList.kt");
        doTest(fileName);
    }

    @TestMetadata("ReturnNotNull.kt")
    public void testReturnNotNull() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/ReturnNotNull.kt");
        doTest(fileName);
    }

    @TestMetadata("ReturnNull.kt")
    public void testReturnNull() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/ReturnNull.kt");
        doTest(fileName);
    }

    @TestMetadata("ReturnNullableBoolean.kt")
    public void testReturnNullableBoolean() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/ReturnNullableBoolean.kt");
        doTest(fileName);
    }

    @TestMetadata("ReturnSet.kt")
    public void testReturnSet() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/ReturnSet.kt");
        doTest(fileName);
    }

    @TestMetadata("SuspendInsideTypeArguments.kt")
    public void testSuspendInsideTypeArguments() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/SuspendInsideTypeArguments.kt");
        doTest(fileName);
    }

    @TestMetadata("SuspendInsideTypeArguments1.kt")
    public void testSuspendInsideTypeArguments1() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/SuspendInsideTypeArguments1.kt");
        doTest(fileName);
    }

    @TestMetadata("This.kt")
    public void testThis() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/This.kt");
        doTest(fileName);
    }

    @TestMetadata("ThisPrefixMatching.kt")
    public void testThisPrefixMatching() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/ThisPrefixMatching.kt");
        doTest(fileName);
    }

    @TestMetadata("TopScope.kt")
    public void testTopScope() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/TopScope.kt");
        doTest(fileName);
    }

    @TestMetadata("UseSiteTargetForPrimaryConstructorParameter.kt")
    public void testUseSiteTargetForPrimaryConstructorParameter() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/idea-completion/testData/keywords/UseSiteTargetForPrimaryConstructorParameter.kt");
        doTest(fileName);
    }
}
