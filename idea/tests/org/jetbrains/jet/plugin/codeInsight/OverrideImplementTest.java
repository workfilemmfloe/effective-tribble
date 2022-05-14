/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.codeInsight;

import org.jetbrains.jet.plugin.PluginTestCaseBase;

public final class OverrideImplementTest extends AbstractOverrideImplementTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.setTestDataPath(PluginTestCaseBase.getTestDataPathBase() + "/codeInsight/overrideImplement");
    }

    public void testEmptyClassBodyFunctionMethod() {
        doImplementFileTest();
    }

    public void testFunctionMethod() {
        doImplementFileTest();
    }

    public void testFunctionProperty() {
        doImplementFileTest();
    }

    public void testFunctionWithTypeParameters() {
        doImplementFileTest();
    }

    public void testGenericTypesSeveralMethods() {
        doImplementFileTest();
    }

    public void testJavaInterfaceMethod() {
        doImplementDirectoryTest();
    }

    public void testJavaParameters() {
        doImplementDirectoryTest();
    }

    public void testFunctionFromTraitInJava() {
        doImplementJavaDirectoryTest("foo.KotlinTrait", "bar");
    }

    public void testGenericMethod() {
        doImplementFileTest();
    }

    public void testProperty() {
        doImplementFileTest();
    }

    public void testTraitGenericImplement() {
        doImplementFileTest();
    }

    public void testRespectCaretPosition() {
        doMultiImplementFileTest();
    }

    public void testGenerateMulti() {
        doMultiImplementFileTest();
    }

    public void testTraitNullableFunction() {
        doImplementFileTest();
    }

    public void testOverrideUnitFunction() {
        doOverrideFileTest();
    }

    public void testOverrideNonUnitFunction() {
        doOverrideFileTest();
    }

    public void testOverrideFunctionProperty() {
        doOverrideFileTest();
    }

    public void testOverridePrimitiveProperty() {
        doMultiImplementFileTest();
    }

    public void testOverrideGenericFunction() {
        doOverrideFileTest();
    }

    public void testMultiOverride() {
        doMultiOverrideFileTest();
    }

    public void testOverrideExplicitFunction() {
        doOverrideFileTest();
    }

    public void testOverrideExplicitProperty() {
        doOverrideFileTest();
    }

    public void testComplexMultiOverride() {
        doMultiOverrideFileTest();
    }

    public void testOverrideRespectCaretPosition() {
        doMultiOverrideFileTest();
    }

    public void testOverrideJavaMethod() {
        doOverrideDirectoryTest("getAnswer");
    }

    public void testJavaMethodWithPackageVisibility() {
        doOverrideDirectoryTest("getFooBar");
    }

    public void testJavaMethodWithPackageProtectedVisibility() {
        doOverrideDirectoryTest("getFooBar");
    }

    public void testInheritVisibilities() {
        doMultiOverrideFileTest();
    }

    public void testImplementSamAdapters() {
        doImplementDirectoryTest();
    }

    public void testOverrideSamAdapters() {
        doOverrideDirectoryTest("foo");
    }

    public void testSameTypeName() {
        doDirectoryTest(new OverrideMethodsHandler());
    }
}
