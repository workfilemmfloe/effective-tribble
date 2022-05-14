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
import org.jetbrains.jet.OutputFile;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;

public class SourceInfoGenTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testSingleFilePackage() {
        String producer = "sourceInfo/foo1.kt";
        loadFiles(producer);
        assertEquals(producer, getProducerInfo("foo/FooPackage.class"));
    }

    public void testMultiFilePackage() {
        loadFiles("sourceInfo/foo1.kt", "sourceInfo/foo2.kt");
        assertEquals(null, getProducerInfo("foo/FooPackage.class"));
    }

    public void testSingleClass() {
        String producer = "sourceInfo/singleClass.kt";
        loadFiles(producer);
        assertEquals(producer, getProducerInfo("SingleClass.class"));
    }

    private String getProducerInfo(String name) {
        OutputFile file = generateClassesInFile().get(name);
        assertNotNull(file);

        ClassReader classReader = new ClassReader(file.asByteArray());

        final String [] producer = new String[1];
        classReader.accept(new ClassVisitor(Opcodes.ASM5) {

            @Override
            public void visitSource(String source, String debug) {
                producer[0] = source;
            }

        }, 0);
        return producer[0];
    }
}
