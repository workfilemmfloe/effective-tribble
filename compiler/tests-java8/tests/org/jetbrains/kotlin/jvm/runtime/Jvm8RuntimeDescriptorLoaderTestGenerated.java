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

package org.jetbrains.kotlin.jvm.runtime;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler/testData/loadJava8/compiledJava")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class Jvm8RuntimeDescriptorLoaderTestGenerated extends AbstractJvm8RuntimeDescriptorLoaderTest {
    public void testAllFilesPresentInCompiledJava() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/loadJava8/compiledJava"), Pattern.compile("^(.+)\\.java$"), true, "sam", "kotlinSignature/propagation");
    }

    @TestMetadata("MapRemove.java")
    public void testMapRemove() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/loadJava8/compiledJava/MapRemove.java");
        doTest(fileName);
    }

    @TestMetadata("TypeAnnotations.java")
    public void testTypeAnnotations() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/loadJava8/compiledJava/TypeAnnotations.java");
        doTest(fileName);
    }

    @TestMetadata("TypeParameterAnnotations.java")
    public void testTypeParameterAnnotations() throws Exception {
        String fileName = KotlinTestUtils.navigationMetadata("compiler/testData/loadJava8/compiledJava/TypeParameterAnnotations.java");
        doTest(fileName);
    }
}
