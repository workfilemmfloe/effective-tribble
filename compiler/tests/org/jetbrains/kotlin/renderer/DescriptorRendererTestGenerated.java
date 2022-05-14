/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.renderer;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler/testData/renderer")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class DescriptorRendererTestGenerated extends AbstractDescriptorRendererTest {
    public void testAllFilesPresentInRenderer() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/renderer"), Pattern.compile("^(.+)\\.kt$"), true);
    }

    @TestMetadata("Classes.kt")
    public void testClasses() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/renderer/Classes.kt");
        doTest(fileName);
    }

    @TestMetadata("Enum.kt")
    public void testEnum() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/renderer/Enum.kt");
        doTest(fileName);
    }

    @TestMetadata("ErrorType.kt")
    public void testErrorType() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/renderer/ErrorType.kt");
        doTest(fileName);
    }

    @TestMetadata("FunctionTypes.kt")
    public void testFunctionTypes() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/renderer/FunctionTypes.kt");
        doTest(fileName);
    }

    @TestMetadata("FunctionTypesInSignature.kt")
    public void testFunctionTypesInSignature() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/renderer/FunctionTypesInSignature.kt");
        doTest(fileName);
    }

    @TestMetadata("GlobalFunctions.kt")
    public void testGlobalFunctions() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/renderer/GlobalFunctions.kt");
        doTest(fileName);
    }

    @TestMetadata("GlobalProperties.kt")
    public void testGlobalProperties() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/renderer/GlobalProperties.kt");
        doTest(fileName);
    }

    @TestMetadata("InheritedMembersVisibility.kt")
    public void testInheritedMembersVisibility() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/renderer/InheritedMembersVisibility.kt");
        doTest(fileName);
    }

    @TestMetadata("KeywordsInNames.kt")
    public void testKeywordsInNames() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/renderer/KeywordsInNames.kt");
        doTest(fileName);
    }

    @TestMetadata("ObjectWithConstructor.kt")
    public void testObjectWithConstructor() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/renderer/ObjectWithConstructor.kt");
        doTest(fileName);
    }

    @TestMetadata("StarProjection.kt")
    public void testStarProjection() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/renderer/StarProjection.kt");
        doTest(fileName);
    }

    @TestMetadata("TraitWithConstructor.kt")
    public void testTraitWithConstructor() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/renderer/TraitWithConstructor.kt");
        doTest(fileName);
    }

    @TestMetadata("UnitType.kt")
    public void testUnitType() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/renderer/UnitType.kt");
        doTest(fileName);
    }
}
