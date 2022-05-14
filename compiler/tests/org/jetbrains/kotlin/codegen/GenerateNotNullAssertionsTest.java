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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection;
import org.jetbrains.kotlin.cli.common.output.outputUtils.OutputUtilsPackage;
import org.jetbrains.kotlin.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import static org.jetbrains.kotlin.codegen.CodegenTestUtil.compileJava;

public class GenerateNotNullAssertionsTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    private void setUpEnvironment(boolean disableCallAssertions, boolean disableParamAssertions, File... extraClassPath) {
        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, extraClassPath);

        configuration.put(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS, disableCallAssertions);
        configuration.put(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS, disableParamAssertions);

        myEnvironment = JetCoreEnvironment.createForTests(getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
    }

    private void doTestCallAssertions(boolean disableCallAssertions) throws Exception {
        File javaClassesTempDirectory = compileJava("notNullAssertions/A.java");

        setUpEnvironment(disableCallAssertions, true, javaClassesTempDirectory);

        loadFile("notNullAssertions/AssertionChecker.kt");
        generateFunction("checkAssertions").invoke(null, !disableCallAssertions);
    }

    public void testGenerateAssertions() throws Exception {
        doTestCallAssertions(false);
    }

    public void testDoNotGenerateAssertions() throws Exception {
        doTestCallAssertions(true);
    }

    public void testNoAssertionsForKotlinFromSource() throws Exception {
        setUpEnvironment(false, true);

        loadFiles("notNullAssertions/noAssertionsForKotlin.kt", "notNullAssertions/noAssertionsForKotlinMain.kt");

        assertNoIntrinsicsMethodIsCalled(PackageClassUtils.getPackageClassName(FqName.ROOT));
    }

    public void testNoAssertionsForKotlinFromBinary() throws Exception {
        setUpEnvironment(false, true);
        loadFile("notNullAssertions/noAssertionsForKotlin.kt");
        OutputFileCollection outputFiles = generateClassesInFile();
        File compiledDirectory = new File(FileUtil.getTempDirectory(), "kotlin-classes");
        OutputUtilsPackage.writeAllTo(outputFiles, compiledDirectory);

        setUpEnvironment(false, true, compiledDirectory);
        loadFile("notNullAssertions/noAssertionsForKotlinMain.kt");

        assertNoIntrinsicsMethodIsCalled(PackageClassUtils.getPackageClassName(FqName.ROOT));
    }

    public void testGenerateParamAssertions() throws Exception {
        File javaClassesTempDirectory = compileJava("notNullAssertions/doGenerateParamAssertions.java");

        setUpEnvironment(true, false, javaClassesTempDirectory);

        loadFile("notNullAssertions/doGenerateParamAssertions.kt");
        generateFunction().invoke(null);
    }

    public void testDoNotGenerateParamAssertions() throws Exception {
        setUpEnvironment(true, true);

        loadFile("notNullAssertions/doNotGenerateParamAssertions.kt");

        assertNoIntrinsicsMethodIsCalled("A");
    }

    public void testNoParamAssertionForPrivateMethod() throws Exception {
        setUpEnvironment(true, false);

        loadFile("notNullAssertions/noAssertionForPrivateMethod.kt");

        assertNoIntrinsicsMethodIsCalled("A");
    }

    public void testArrayListGet() {
        setUpEnvironment(false, false);

        loadFile("notNullAssertions/arrayListGet.kt");
        String text = generateToText();

        assertTrue(text.contains("checkExpressionValueIsNotNull"));
        assertTrue(text.contains("checkParameterIsNotNull"));
    }

    public void testJavaMultipleSubstitutions() {
        File javaClassesTempDirectory = compileJava("notNullAssertions/javaMultipleSubstitutions.java");
        setUpEnvironment(false, false, javaClassesTempDirectory);

        loadFile("notNullAssertions/javaMultipleSubstitutions.kt");
        String text = generateToText();

        assertEquals(3, StringUtil.getOccurrenceCount(text, "checkExpressionValueIsNotNull"));
        assertEquals(3, StringUtil.getOccurrenceCount(text, "checkParameterIsNotNull"));
    }

    public void testAssertionForNotNullTypeParam() {
        setUpEnvironment(false, false);

        loadFile("notNullAssertions/assertionForNotNullTypeParam.kt");

        assertTrue(generateToText().contains("checkParameterIsNotNull"));
    }

    public void testNoAssertionForNullableGenericMethod() {
        setUpEnvironment(false, true);

        loadFile("notNullAssertions/noAssertionForNullableGenericMethod.kt");

        assertNoIntrinsicsMethodIsCalled(PackageClassUtils.getPackageClassName(FqName.ROOT));
    }

    public void testNoAssertionForNullableGenericMethodCall() {
        setUpEnvironment(false, true);

        loadFile("notNullAssertions/noAssertionForNullableGenericMethodCall.kt");

        assertNoIntrinsicsMethodIsCalled("A");
    }

    public void testParamAssertionMessage() throws Exception {
        setUpEnvironment(false, false);

        loadText("class A { fun foo(s: String) {} }");
        Class<?> a = generateClass("A");
        try {
            a.getDeclaredMethod("foo", String.class).invoke(a.newInstance(), new Object[] {null});
        }
        catch (InvocationTargetException ite) {
            Throwable e = ite.getTargetException();
            //noinspection ThrowableResultOfMethodCallIgnored
            assertInstanceOf(e, IllegalArgumentException.class);
            assertEquals("Parameter specified as non-null is null: method A.foo, parameter s", e.getMessage());
            return;
        }

        fail("Assertion should have been fired");
    }

    private void assertNoIntrinsicsMethodIsCalled(String className) {
        OutputFileCollection classes = generateClassesInFile();
        OutputFile file = classes.get(className + ".class");
        assertNotNull(file);
        ClassReader reader = new ClassReader(file.asByteArray());

        reader.accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public MethodVisitor visitMethod(
                    int access, @NotNull final String callerName, @NotNull final String callerDesc, String signature, String[] exceptions
            ) {
                return new MethodVisitor(Opcodes.ASM5) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        assertFalse(
                                "Intrinsics method is called: " + name + desc + "  Caller: " + callerName + callerDesc,
                                "kotlin/jvm/internal/Intrinsics".equals(owner)
                        );
                    }
                };
            }
        }, 0);
    }
}
