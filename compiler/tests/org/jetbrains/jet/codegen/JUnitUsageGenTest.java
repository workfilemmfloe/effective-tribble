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

import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;

public class JUnitUsageGenTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        File junitJar = new File("libraries/lib/junit-4.9.jar");

        if (!junitJar.exists()) {
            throw new AssertionError("JUnit jar wasn't found");
        }

        myEnvironment = new JetCoreEnvironment(getTestRootDisposable(), JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, junitJar));
    }

    public void testKt2344() throws Exception {
        loadFile("junit/kt2344.kt");
        generateFunction().invoke(null);
    }

    public void testKt1592() throws Exception {
        loadFile("junit/kt1592.kt");
        Class<?> namespaceClass = generateNamespaceClass();
        Method method = namespaceClass.getMethod("foo", Method.class);
        method.setAccessible(true);
        Test annotation = method.getAnnotation(Test.class);
        assertEquals(annotation.timeout(), 0l);
        assertEquals(annotation.expected(), Test.None.class);
    }
}
