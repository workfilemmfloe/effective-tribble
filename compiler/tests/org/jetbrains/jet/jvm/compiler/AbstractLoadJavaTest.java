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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.junit.Assert;

import java.io.File;
import java.util.Collections;

import static org.jetbrains.jet.jvm.compiler.LoadDescriptorUtil.analyzeKotlinAndLoadTestNamespace;
import static org.jetbrains.jet.jvm.compiler.LoadDescriptorUtil.compileJavaAndLoadTestNamespaceFromBinary;
import static org.jetbrains.jet.test.util.NamespaceComparator.DONT_INCLUDE_METHODS_OF_OBJECT;
import static org.jetbrains.jet.test.util.NamespaceComparator.compareNamespaces;

/**
 * @author Stepan Koltsov
 */
/*
    The generated test compares namespace descriptors loaded from kotlin sources and read from compiled java.
*/
public abstract class AbstractLoadJavaTest extends TestCaseWithTmpdir {

    public void doTest(@NotNull String javaFileName) throws Exception {
        Assert.assertTrue("A java file expected: " + javaFileName, javaFileName.endsWith(".java"));
        File javaFile = new File(javaFileName);
        File ktFile = new File(javaFile.getPath().replaceFirst("\\.java$", ".kt"));
        File txtFile = new File(javaFile.getPath().replaceFirst("\\.java$", ".txt"));
        NamespaceDescriptor nsa = analyzeKotlinAndLoadTestNamespace(ktFile, myTestRootDisposable, ConfigurationKind.JDK_AND_ANNOTATIONS);
        NamespaceDescriptor nsb = compileJavaAndLoadTestNamespaceFromBinary(Collections.singletonList(javaFile),
                                                                            tmpdir, myTestRootDisposable, ConfigurationKind.JDK_AND_ANNOTATIONS);
        compareNamespaces(nsa, nsb, DONT_INCLUDE_METHODS_OF_OBJECT, txtFile);
    }
}
