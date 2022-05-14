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

package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;

import java.io.File;
import java.net.URLClassLoader;

import static org.jetbrains.jet.codegen.CodegenTestUtil.assertThrows;
import static org.jetbrains.jet.codegen.CodegenTestUtil.compileJava;

public class ClassPathInParentClassLoaderTest extends CodegenTestCase {
    @NotNull
    @Override
    protected GeneratedClassLoader createClassLoader(@NotNull ClassFileFactory factory) {
        ClassLoader parentClassLoader = new URLClassLoader(getClassPathURLs(), null);
        initializedClassLoader = new GeneratedClassLoader(factory, parentClassLoader);
        return initializedClassLoader;
    }

    public void testKt2781() throws Exception {
        File javaClassesTempDirectory = compileJava("classPathInParentClassLoader/kt2781.java");

        myEnvironment = JetCoreEnvironment.createForTests(getTestRootDisposable(), JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, JetTestUtils.getAnnotationsJar(), javaClassesTempDirectory));

        loadFile("classPathInParentClassLoader/kt2781.kt");
        assertThrows(generateFunction(), IllegalAccessError.class, null);
    }
}
