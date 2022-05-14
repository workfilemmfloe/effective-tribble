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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.load.java.JvmAnnotationNames;
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils;
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.serialization.PackageData;
import org.jetbrains.kotlin.serialization.ProtoBuf;
import org.jetbrains.kotlin.serialization.deserialization.NameResolver;
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil;
import org.jetbrains.kotlin.test.ConfigurationKind;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KotlinPackageAnnotationTest extends CodegenTestCase {
    public static final FqName PACKAGE_NAME = new FqName("test");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    public void testOldPackageKotlinInfo() throws Exception {
        loadText("package " + PACKAGE_NAME + "\n" +
                 "\n" +
                 "fun foo() = 42\n" +
                 "val bar = 239\n" +
                 "\n" +
                 "class A\n" +
                 "class B\n" +
                 "object C\n");
        Class aClass = generateClass(PackageClassUtils.getPackageClassFqName(PACKAGE_NAME).asString());

        Class<? extends Annotation> annotationClass = loadAnnotationClassQuietly(JvmAnnotationNames.KOTLIN_PACKAGE.asString());
        assertTrue(aClass.isAnnotationPresent(annotationClass));

        Annotation kotlinPackage = aClass.getAnnotation(annotationClass);

        String[] data = (String[]) CodegenTestUtil.getAnnotationAttribute(kotlinPackage, "data");
        assertNotNull(data);
        PackageData packageData = JvmProtoBufUtil.readPackageDataFrom(data);

        Set<String> callableNames = collectCallableNames(packageData.getPackageProto().getMemberList(), packageData.getNameResolver());
        assertEmpty(callableNames);
    }

    public void testPackagePartKotlinInfo() throws Exception {
        loadText("package " + PACKAGE_NAME + "\n" +
                 "\n" +
                 "fun foo() = 42\n" +
                 "val bar = 239\n" +
                 "\n" +
                 "class A\n" +
                 "class B\n" +
                 "object C\n");
        Class aClass = generateClass(PackagePartClassUtils.getPackagePartFqName(PACKAGE_NAME, DEFAULT_TEST_FILE_NAME).asString());

        Class<? extends Annotation> annotationClass = loadAnnotationClassQuietly(JvmAnnotationNames.KOTLIN_FILE_FACADE.asString());
        assertTrue(aClass.isAnnotationPresent(annotationClass));

        Annotation kotlinPackage = aClass.getAnnotation(annotationClass);

        String[] data = (String[]) CodegenTestUtil.getAnnotationAttribute(kotlinPackage, "data");
        assertNotNull(data);
        PackageData packageData = JvmProtoBufUtil.readPackageDataFrom(data);

        Set<String> callableNames = collectCallableNames(packageData.getPackageProto().getMemberList(), packageData.getNameResolver());
        assertSameElements(callableNames, Arrays.asList("foo", "bar"));
    }

    @NotNull
    public static Set<String> collectCallableNames(@NotNull List<ProtoBuf.Callable> members, @NotNull NameResolver nameResolver) {
        Set<String> callableNames = new HashSet<String>();
        for (ProtoBuf.Callable callable : members) {
            callableNames.add(nameResolver.getName(callable.getName()).asString());
        }
        return callableNames;
    }
}
