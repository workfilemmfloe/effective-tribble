/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.maven.configuration;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@RunWith(JUnit3RunnerWithInners.class)
public class MavenConfigureProjectByChangingFileTestGenerated extends AbstractMavenConfigureProjectByChangingFileTest {
    @TestMetadata("idea/idea-maven/testData/configurator/jvm")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Jvm extends AbstractMavenConfigureProjectByChangingFileTest {
        public void testAllFilesPresentInJvm() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("idea/idea-maven/testData/configurator/jvm"), Pattern.compile("^([^\\.]+)$"), TargetBackend.ANY, false);
        }

        @TestMetadata("fixExisting")
        public void testFixExisting() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/idea-maven/testData/configurator/jvm/fixExisting/");
            doTestWithMaven(fileName);
        }

        @TestMetadata("libraryMissed")
        public void testLibraryMissed() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/idea-maven/testData/configurator/jvm/libraryMissed/");
            doTestWithMaven(fileName);
        }

        @TestMetadata("pluginMissed")
        public void testPluginMissed() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/idea-maven/testData/configurator/jvm/pluginMissed/");
            doTestWithMaven(fileName);
        }

        @TestMetadata("simpleProject")
        public void testSimpleProject() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/idea-maven/testData/configurator/jvm/simpleProject/");
            doTestWithMaven(fileName);
        }

        @TestMetadata("simpleProjectEAP")
        public void testSimpleProjectEAP() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/idea-maven/testData/configurator/jvm/simpleProjectEAP/");
            doTestWithMaven(fileName);
        }

        @TestMetadata("simpleProjectRc")
        public void testSimpleProjectRc() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/idea-maven/testData/configurator/jvm/simpleProjectRc/");
            doTestWithMaven(fileName);
        }

        @TestMetadata("simpleProjectSnapshot")
        public void testSimpleProjectSnapshot() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/idea-maven/testData/configurator/jvm/simpleProjectSnapshot/");
            doTestWithMaven(fileName);
        }
    }

    @TestMetadata("idea/idea-maven/testData/configurator/js")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Js extends AbstractMavenConfigureProjectByChangingFileTest {
        public void testAllFilesPresentInJs() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("idea/idea-maven/testData/configurator/js"), Pattern.compile("^([^\\.]+)$"), TargetBackend.ANY, false);
        }

        @TestMetadata("libraryMissed")
        public void testLibraryMissed() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/idea-maven/testData/configurator/js/libraryMissed/");
            doTestWithJSMaven(fileName);
        }

        @TestMetadata("pluginMissed")
        public void testPluginMissed() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/idea-maven/testData/configurator/js/pluginMissed/");
            doTestWithJSMaven(fileName);
        }

        @TestMetadata("simpleProject")
        public void testSimpleProject() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/idea-maven/testData/configurator/js/simpleProject/");
            doTestWithJSMaven(fileName);
        }

        @TestMetadata("simpleProjectSnapshot")
        public void testSimpleProjectSnapshot() throws Exception {
            String fileName = KotlinTestUtils.navigationMetadata("idea/idea-maven/testData/configurator/js/simpleProjectSnapshot/");
            doTestWithJSMaven(fileName);
        }
    }
}
