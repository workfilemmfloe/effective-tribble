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

import jet.KotlinClass;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.descriptors.serialization.ClassData;
import org.jetbrains.jet.descriptors.serialization.JavaProtoBufUtil;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Set;

import static org.jetbrains.jet.codegen.KotlinPackageAnnotationTest.collectCallableNames;

public class KotlinClassAnnotationTest extends CodegenTestCase {
    public static final FqName PACKAGE_NAME = new FqName("test");
    public static final FqNameUnsafe CLASS_NAME = new FqNameUnsafe("A");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testClassKotlinInfo() throws Exception {
        loadText("package " + PACKAGE_NAME + "\n" +
                 "\n" +
                 "class " + CLASS_NAME + " {\n" +
                 "    fun foo() {}\n" +
                 "    fun bar() = 42\n" +
                 "}\n");
        Class aClass = generateClass(PACKAGE_NAME + "." + CLASS_NAME);

        Class<? extends Annotation> annotationClass = getCorrespondingAnnotationClass(KotlinClass.class);
        assertTrue(aClass.isAnnotationPresent(annotationClass));
        Annotation kotlinClass = aClass.getAnnotation(annotationClass);

        ClassData data = JavaProtoBufUtil.readClassDataFrom((String[]) ClassLoaderIsolationUtil.getAnnotationAttribute(kotlinClass, "data"));

        Set<String> callableNames = collectCallableNames(data.getClassProto().getMemberList(), data.getNameResolver());
        assertSameElements(Arrays.asList("foo", "bar"), callableNames);
    }
}
