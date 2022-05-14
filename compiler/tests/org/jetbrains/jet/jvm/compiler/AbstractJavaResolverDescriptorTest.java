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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.di.InjectorForJavaSemanticServices;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.test.TestCaseWithTmpdir;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractJavaResolverDescriptorTest extends TestCaseWithTmpdir {

    protected JavaDescriptorResolver javaDescriptorResolver;

    protected void compileJavaFile(@NotNull String fileRelativePath)
            throws IOException {
        compileJavaFile(fileRelativePath, null);
    }

    protected void compileJavaFile(@NotNull String fileRelativePath, @Nullable String classPath)
            throws IOException {
        File javaFile = new File(getPath() + fileRelativePath);
        assertNotNull(javaFile);
        List<String> options = new ArrayList<String>();
        options.add("-d");
        options.add(tmpdir.getPath());
        if (classPath != null) {
            options.add("-cp");
            options.add(classPath);
        }
        JetTestUtils.compileJavaFiles(Collections.singleton(javaFile), options);
    }

    @NotNull
    protected abstract String getPath();


    @Override
    public void setUp() throws Exception {
        super.setUp();
        setUpJavaDescriptorResolver();
    }

    @Override
    public void tearDown() throws Exception {
        javaDescriptorResolver = null;
        super.tearDown();
    }

    private void setUpJavaDescriptorResolver() {
        JetCoreEnvironment jetCoreEnvironment =
                new JetCoreEnvironment(myTestRootDisposable, JetTestUtils.compilerConfigurationForTests(
                        ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, JetTestUtils.getAnnotationsJar(), tmpdir));

        Project project = jetCoreEnvironment.getProject();
        InjectorForJavaSemanticServices injector = new InjectorForJavaSemanticServices(project);
        javaDescriptorResolver = injector.getJavaDescriptorResolver();
    }
}
