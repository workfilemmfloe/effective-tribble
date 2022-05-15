/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.navigation;

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
@TestMetadata("idea/testData/navigation/gotoSuper")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class GotoSuperTestGenerated extends AbstractGotoSuperTest {
    public void testAllFilesPresentInGotoSuper() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("idea/testData/navigation/gotoSuper"), Pattern.compile("^(.+)\\.test$"), TargetBackend.ANY, true);
    }

    @TestMetadata("BadPositionLambdaParameter.test")
    public void testBadPositionLambdaParameter() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/navigation/gotoSuper/BadPositionLambdaParameter.test");
        doTest(fileName);
    }

    @TestMetadata("ClassSimple.test")
    public void testClassSimple() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/navigation/gotoSuper/ClassSimple.test");
        doTest(fileName);
    }

    @TestMetadata("DelegatedFun.test")
    public void testDelegatedFun() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/navigation/gotoSuper/DelegatedFun.test");
        doTest(fileName);
    }

    @TestMetadata("DelegatedProperty.test")
    public void testDelegatedProperty() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/navigation/gotoSuper/DelegatedProperty.test");
        doTest(fileName);
    }

    @TestMetadata("FakeOverrideFun.test")
    public void testFakeOverrideFun() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/navigation/gotoSuper/FakeOverrideFun.test");
        doTest(fileName);
    }

    @TestMetadata("FakeOverrideFunWithMostRelevantImplementation.test")
    public void testFakeOverrideFunWithMostRelevantImplementation() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/navigation/gotoSuper/FakeOverrideFunWithMostRelevantImplementation.test");
        doTest(fileName);
    }

    @TestMetadata("FakeOverrideProperty.test")
    public void testFakeOverrideProperty() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/navigation/gotoSuper/FakeOverrideProperty.test");
        doTest(fileName);
    }

    @TestMetadata("FunctionSimple.test")
    public void testFunctionSimple() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/navigation/gotoSuper/FunctionSimple.test");
        doTest(fileName);
    }

    @TestMetadata("ObjectSimple.test")
    public void testObjectSimple() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/navigation/gotoSuper/ObjectSimple.test");
        doTest(fileName);
    }

    @TestMetadata("PropertySimple.test")
    public void testPropertySimple() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/navigation/gotoSuper/PropertySimple.test");
        doTest(fileName);
    }

    @TestMetadata("SuperWithNativeToGenericMapping.test")
    public void testSuperWithNativeToGenericMapping() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/navigation/gotoSuper/SuperWithNativeToGenericMapping.test");
        doTest(fileName);
    }

    @TestMetadata("TraitSimple.test")
    public void testTraitSimple() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/navigation/gotoSuper/TraitSimple.test");
        doTest(fileName);
    }

    @TestMetadata("TypeAliasInSuperType.test")
    public void testTypeAliasInSuperType() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("idea/testData/navigation/gotoSuper/TypeAliasInSuperType.test");
        doTest(fileName);
    }
}
