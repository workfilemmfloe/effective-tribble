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

package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.asm4.ClassReader;
import org.jetbrains.asm4.ClassVisitor;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.jet.CompileCompilerDependenciesTest;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentUtil;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.config.CompilerConfiguration;

import java.io.File;

/**
 * @author udalov
 */
public class GenerateNotNullAssertionsTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    private void setUpEnvironment(boolean generateAssertions, boolean generateParamAssertions, File... extraClassPath) {
        CompilerConfiguration configuration = CompileCompilerDependenciesTest.compilerConfigurationForTests(
                ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, extraClassPath);

        configuration.put(JVMConfigurationKeys.GENERATE_NOT_NULL_ASSERTIONS, generateAssertions);
        configuration.put(JVMConfigurationKeys.GENERATE_NOT_NULL_PARAMETER_ASSERTIONS, generateParamAssertions);

        myEnvironment = new JetCoreEnvironment(getTestRootDisposable(), configuration);
    }

    private void doTestGenerateAssertions(boolean generateAssertions, String ktFile) throws Exception {
        File javaClassesTempDirectory = compileJava("notNullAssertions/A.java");

        setUpEnvironment(generateAssertions, false, javaClassesTempDirectory);

        blackBoxMultiFile("OK", false, "notNullAssertions/AssertionChecker.kt", ktFile);
    }

    public void testGenerateAssertions() throws Exception {
        doTestGenerateAssertions(true, "notNullAssertions/doGenerateAssertions.kt");
    }

    public void testDoNotGenerateAssertions() throws Exception {
        doTestGenerateAssertions(false, "notNullAssertions/doNotGenerateAssertions.kt");
    }

    public void testNoAssertionsForKotlinFromSource() throws Exception {
        setUpEnvironment(true, false);

        loadFiles("notNullAssertions/noAssertionsForKotlin.kt", "notNullAssertions/noAssertionsForKotlinMain.kt");

        assertNoIntrinsicsMethodIsCalled("namespace");
    }

    public void testNoAssertionsForKotlinFromBinary() throws Exception {
        CompilerConfiguration configuration = CompileCompilerDependenciesTest.compilerConfigurationForTests(
                ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK);
        JetCoreEnvironment tmpEnvironment = new JetCoreEnvironment(getTestRootDisposable(), configuration);
        GenerationState state = generateCommon(ClassBuilderFactories.TEST, tmpEnvironment,
                CodegenTestFiles.create(tmpEnvironment.getProject(), new String[] {"notNullAssertions/noAssertionsForKotlin.kt"}));
        File compiledDirectory = new File(FileUtil.getTempDirectory(), "kotlin-classes");
        CompileEnvironmentUtil.writeToOutputDirectory(state.getFactory(), compiledDirectory);

        setUpEnvironment(true, false, compiledDirectory);

        loadFile("notNullAssertions/noAssertionsForKotlinMain.kt");

        assertNoIntrinsicsMethodIsCalled("namespace");
    }

    public void testGenerateParamAssertions() throws Exception {
        File javaClassesTempDirectory = compileJava("notNullAssertions/doGenerateParamAssertions.java");

        setUpEnvironment(false, true, javaClassesTempDirectory);

        blackBoxFile("notNullAssertions/doGenerateParamAssertions.kt");
    }

    public void testDoNotGenerateParamAssertions() throws Exception {
        setUpEnvironment(false, false);

        loadFile("notNullAssertions/doNotGenerateParamAssertions.kt");

        assertNoIntrinsicsMethodIsCalled("A");
    }

    public void testNoParamAssertionForPrivateMethod() throws Exception {
        setUpEnvironment(false, true);

        loadFile("notNullAssertions/noAssertionForPrivateMethod.kt");

        assertNoIntrinsicsMethodIsCalled("A");
    }

    private void assertNoIntrinsicsMethodIsCalled(String className) {
        ClassFileFactory classes = generateClassesInFile();
        ClassReader reader = new ClassReader(classes.asBytes(className + ".class"));

        reader.accept(new ClassVisitor(Opcodes.ASM4) {
            @Override
            public MethodVisitor visitMethod(
                    int access, final String callerName, final String callerDesc, String signature, String[] exceptions
            ) {
                return new MethodVisitor(Opcodes.ASM4) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                        assertFalse(
                                "jet/Intrinsics method is called: " + name + desc + "  Caller: " + callerName + callerDesc,
                                "jet/Intrinsics".equals(owner)
                        );
                    }
                };
            }
        }, 0);
    }
}
