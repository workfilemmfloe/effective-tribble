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

package org.jetbrains.jet.jvm.compiler;

import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.junit.Assert;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.jvm.compiler.LoadDescriptorUtil.compileKotlinToDirAndGetAnalyzeExhaust;

/**
 * @author Stepan Koltsov
 *
 * @see WriteSignatureTest
 */
@SuppressWarnings({"JUnitTestCaseWithNonTrivialConstructors", "JUnitTestCaseWithNoTests"})
public class CompileJavaAgainstKotlinTest extends TestCaseWithTmpdir {

    @NotNull
    private final File ktFile;
    @NotNull
    private final File javaFile;

    public CompileJavaAgainstKotlinTest(@NotNull File ktFile) {
        this.ktFile = ktFile;
        Assert.assertTrue(ktFile.getName().endsWith(".kt"));
        this.javaFile = new File(ktFile.getPath().replaceFirst("\\.kt", ".java"));
    }

    @Override
    public String getName() {
        return ktFile.getName();
    }

    @Override
    protected void runTest() throws Throwable {
        compileKotlinToDirAndGetAnalyzeExhaust(ktFile, tmpdir, getTestRootDisposable(), ConfigurationKind.JDK_ONLY);

        List<String> options = Arrays.asList(
                "-classpath", tmpdir.getPath() + System.getProperty("path.separator") + "out/production/stdlib",
                "-d", tmpdir.getPath()
        );
        JetTestUtils.compileJavaFiles(Collections.singleton(javaFile), options);
    }

    public static Test suite() {
        return JetTestCaseBuilder.suiteForDirectory(JetTestCaseBuilder.getTestDataPathBase(), "/compileJavaAgainstKotlin", true, new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new CompileJavaAgainstKotlinTest(file);
            }
        });

    }

}
