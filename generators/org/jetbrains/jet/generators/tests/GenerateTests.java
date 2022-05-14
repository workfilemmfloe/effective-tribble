/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.generators.tests;

import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.checkers.AbstractDiagnosticsTestWithEagerResolve;
import org.jetbrains.jet.checkers.AbstractJetPsiCheckerTest;
import org.jetbrains.jet.codegen.AbstractDataClassCodegenTest;
import org.jetbrains.jet.codegen.AbstractIntrinsicsTestCase;
import org.jetbrains.jet.codegen.AbstractMultiDeclTestCase;
import org.jetbrains.jet.codegen.extension.AbstractExtensionPropertiesTest;
import org.jetbrains.jet.codegen.flags.AbstractWriteFlagsTest;
import org.jetbrains.jet.codegen.labels.AbstractLabelGenTest;
import org.jetbrains.jet.jvm.compiler.AbstractLoadCompiledKotlinTest;
import org.jetbrains.jet.jvm.compiler.AbstractLoadJavaTest;
import org.jetbrains.jet.lang.resolve.lazy.AbstractLazyResolveDescriptorRendererTest;
import org.jetbrains.jet.lang.resolve.lazy.AbstractLazyResolveNamespaceComparingTest;
import org.jetbrains.jet.plugin.highlighter.AbstractDeprecatedHighlightingTest;
import org.jetbrains.jet.test.generator.SimpleTestClassModel;
import org.jetbrains.jet.test.generator.TestClassModel;
import org.jetbrains.jet.test.generator.TestGenerator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class GenerateTests {
    private static void generateTest(
            @NotNull String baseDir,
            @NotNull String suiteClass,
            @NotNull Class<? extends TestCase> baseTestClass,
            @NotNull TestClassModel... testClassModels
    ) throws IOException {
        new TestGenerator(
                baseDir,
                baseTestClass.getPackage().getName(),
                suiteClass,
                baseTestClass,
                Arrays.asList(testClassModels),
                GenerateTests.class
        ).generateAndSave();
    }

    public static void main(String[] args) throws IOException {
        generateTest(
                "compiler/tests/",
                "JetDiagnosticsTestGenerated",
                AbstractDiagnosticsTestWithEagerResolve.class,
                testModel("compiler/testData/diagnostics/tests"),
                testModel("compiler/testData/diagnostics/tests/script", true, "ktscript", "doTest")
        );

        generateTest(
                "compiler/tests/",
                "DataClassCodegenTestGenerated",
                AbstractDataClassCodegenTest.class,
                testModel("compiler/testData/codegen/dataClasses", "blackBoxFileByFullPath")
        );

        generateTest(
                "compiler/tests/",
                "IntrinsicsTestGenerated",
                AbstractIntrinsicsTestCase.class,
                testModel("compiler/testData/codegen/intrinsics", "blackBoxFileByFullPath")
        );

        generateTest(
                "compiler/tests/",
                "MultiDeclTestGenerated",
                AbstractMultiDeclTestCase.class,
                testModel("compiler/testData/codegen/multiDecl", "blackBoxFileByFullPath")
        );

        generateTest(
                "compiler/tests/",
                "WriteFlagsTestGenerated",
                AbstractWriteFlagsTest.class,
                testModel("compiler/testData/writeFlags")
        );

        generateTest(
                "compiler/tests/",
                "LabelGenTestGenerated",
                AbstractLabelGenTest.class,
                testModel("compiler/testData/codegen/label")
        );

        generateTest(
                "compiler/tests/",
                "ExtensionPropertiesTestGenerated",
                AbstractExtensionPropertiesTest.class,
                testModel("compiler/testData/codegen/extensionProperties")
        );

        generateTest(
                "compiler/tests/",
                "LoadCompiledKotlinTestGenerated",
                AbstractLoadCompiledKotlinTest.class,
                testModel("compiler/testData/loadKotlin")
        );


        generateTest(
                "compiler/tests/",
                "LoadJavaTestGenerated",
                AbstractLoadJavaTest.class,
                testModel("compiler/testData/loadJava", true, "java", "doTest")
        );

        generateTest(
                "compiler/tests/",
                "LazyResolveDescriptorRendererTestGenerated",
                AbstractLazyResolveDescriptorRendererTest.class,
                testModel("compiler/testData/renderer"),
                testModel("compiler/testData/lazyResolve/descriptorRenderer")
        );

        // TODO test is temporarily disabled
        //generateTest(
        //        "compiler/tests/",
        //        "org.jetbrains.jet.lang.resolve.lazy",
        //        "LazyResolveDiagnosticsTestGenerated",
        //        AbstractLazyResolveDiagnosticsTest.class,
        //        new SimpleTestClassModel(AbstractLazyResolveDiagnosticsTest.TEST_DATA_DIR, true, "kt", "doTest")
        //);

        generateTest(
                "compiler/tests/",
                "LazyResolveNamespaceComparingTestGenerated",
                AbstractLazyResolveNamespaceComparingTest.class,
                testModel("compiler/testData/loadKotlin", "doTestSinglePackage"),
                testModel("compiler/testData/loadJava", "doTestSinglePackage"),
                testModel("compiler/testData/lazyResolve/namespaceComparator", "doTestSinglePackage")
        );

        generateTest(
                "idea/tests/",
                "JetPsiCheckerTestGenerated",
                AbstractJetPsiCheckerTest.class,
                testModel("idea/testData/checker", false, "kt", "doTest"),
                testModel("idea/testData/checker/regression"),
                testModel("idea/testData/checker/rendering"),
                testModel("idea/testData/checker/infos")
        );

        generateTest(
                "idea/tests/",
                "DeprecatedHighlightingTestGenerated",
                AbstractDeprecatedHighlightingTest.class,
                testModel("idea/testData/highlighter/deprecated")
        );
    }
    
    private static SimpleTestClassModel testModel(@NotNull String rootPath) {
        return testModel(rootPath, true, "kt", "doTest");
    }

    private static SimpleTestClassModel testModel(@NotNull String rootPath, @NotNull String methodName) {
        return testModel(rootPath, true, "kt", methodName);
    }

    private static SimpleTestClassModel testModel(
            @NotNull String rootPath,
            boolean recursive,
            @NotNull String extension,
            @NotNull String doTestMethodName
    ) {
        return new SimpleTestClassModel(new File(rootPath), recursive, extension, doTestMethodName);
    }

    private GenerateTests() {
    }
}