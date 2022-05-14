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

package org.jetbrains.jet.jvm.compiler;

import com.google.common.collect.Iterables;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.MockLibraryUtil;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.common.messages.AnalyzerWithCompilerReport;
import org.jetbrains.jet.cli.common.messages.MessageCollectorPlainTextToStream;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.jetbrains.jet.test.util.DescriptorValidator;
import org.jetbrains.jet.test.util.RecursiveDescriptorComparator;
import org.jetbrains.jet.utils.UtilsPackage;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isObject;
import static org.jetbrains.jet.test.util.RecursiveDescriptorComparator.validateAndCompareDescriptorWithFile;

public class CompileKotlinAgainstCustomBinariesTest extends TestCaseWithTmpdir {
    public static final String TEST_DATA_PATH = "compiler/testData/compileKotlinAgainstCustomBinaries/";

    @NotNull
    private File getTestDataDirectory() {
        return new File(TEST_DATA_PATH, getTestName(true));
    }

    @NotNull
    private File getTestDataFileWithExtension(@NotNull String extension) {
        return new File(getTestDataDirectory(), getTestName(true) + "." + extension);
    }

    @NotNull
    private File compileLibrary(@NotNull String sourcePath) {
        return MockLibraryUtil.compileLibraryToJar(new File(getTestDataDirectory(), sourcePath).getPath(), false);
    }

    private void doTestWithTxt(@NotNull File... extraClassPath) throws Exception {
        PackageViewDescriptor packageView = analyzeFileToPackageView(extraClassPath);

        RecursiveDescriptorComparator.Configuration comparator =
                RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT.withValidationStrategy(
                        DescriptorValidator.ValidationVisitor.ALLOW_ERROR_TYPES);
        validateAndCompareDescriptorWithFile(packageView, comparator, getTestDataFileWithExtension("txt"));
    }

    @NotNull
    private PackageViewDescriptor analyzeFileToPackageView(@NotNull File... extraClassPath) throws IOException {
        Project project = createEnvironment(Arrays.asList(extraClassPath)).getProject();

        AnalyzeExhaust exhaust = AnalyzerFacadeForJVM.analyzeOneFileWithJavaIntegration(
                JetTestUtils.loadJetFile(project, getTestDataFileWithExtension("kt"))
        );

        PackageViewDescriptor packageView = exhaust.getModuleDescriptor().getPackage(LoadDescriptorUtil.TEST_PACKAGE_FQNAME);
        assertNotNull("Failed to find package: " + LoadDescriptorUtil.TEST_PACKAGE_FQNAME, packageView);
        return packageView;
    }

    @NotNull
    private JetCoreEnvironment createEnvironment(@NotNull List<File> extraClassPath) {
        List<File> extras = new ArrayList<File>();
        extras.addAll(extraClassPath);
        extras.add(JetTestUtils.getAnnotationsJar());

        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, extras.toArray(new File[extras.size()]));
        return JetCoreEnvironment.createForTests(getTestRootDisposable(), configuration);
    }

    @NotNull
    private Collection<DeclarationDescriptor> analyzeAndGetAllDescriptors(@NotNull File... extraClassPath) throws IOException {
        return analyzeFileToPackageView(extraClassPath).getMemberScope().getAllDescriptors();
    }

    @NotNull
    private static File copyJarFileWithoutEntry(@NotNull File jarPath, @NotNull String entryToDelete) {
        try {
            File outputFile = new File(jarPath.getParentFile(), FileUtil.getNameWithoutExtension(jarPath) + "-after.jar");

            @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
            JarFile jar = new JarFile(jarPath);
            ZipOutputStream output = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
            try {
                for (Enumeration<JarEntry> enumeration = jar.entries(); enumeration.hasMoreElements(); ) {
                    JarEntry jarEntry = enumeration.nextElement();
                    if (entryToDelete.equals(jarEntry.getName())) {
                        continue;
                    }
                    output.putNextEntry(jarEntry);
                    output.write(FileUtil.loadBytes(jar.getInputStream(jarEntry)));
                    output.closeEntry();
                }
            }
            finally {
                output.close();
                jar.close();
            }

            return outputFile;
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
    }


    public void testDuplicateObjectInBinaryAndSources() throws Exception {
        Collection<DeclarationDescriptor> allDescriptors = analyzeAndGetAllDescriptors(compileLibrary("library"));
        assertEquals(allDescriptors.toString(), 2, allDescriptors.size());
        for (DeclarationDescriptor descriptor : allDescriptors) {
            assertTrue("Wrong name: " + descriptor, descriptor.getName().asString().equals("Lol"));
            assertTrue("Should be an object: " + descriptor, isObject(descriptor));
            assertNotNull("Object should have a class object: " + descriptor, ((ClassDescriptor) descriptor).getClassObjectDescriptor());
        }
    }

    public void testBrokenJarWithNoClassForObject() throws Exception {
        File brokenJar = copyJarFileWithoutEntry(compileLibrary("library"), "test/Lol.class");
        Collection<DeclarationDescriptor> allDescriptors = analyzeAndGetAllDescriptors(brokenJar);
        assertEmpty("No descriptors should be found: " + allDescriptors, allDescriptors);
    }

    public void testSameLibraryTwiceInClasspath() throws Exception {
        doTestWithTxt(compileLibrary("library-1"), compileLibrary("library-2"));
    }

    public void testMissingEnumReferencedInAnnotationArgument() throws Exception {
        doTestWithTxt(copyJarFileWithoutEntry(compileLibrary("library"), "test/E.class"));
    }

    public void testNoWarningsOnJavaKotlinInheritance() throws Exception {
        // This test checks that there are no PARAMETER_NAME_CHANGED_ON_OVERRIDE or DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES
        // warnings when subclassing in Kotlin from Java binaries (in case when no parameter names are available for Java classes)

        JetTestUtils.compileJavaFiles(
                Collections.singletonList(getTestDataFileWithExtension("java")),
                Arrays.asList("-d", tmpdir.getPath())
        );

        Project project = createEnvironment(Collections.singletonList(tmpdir)).getProject();

        AnalyzeExhaust exhaust = AnalyzerFacadeForJVM.analyzeOneFileWithJavaIntegration(
                JetTestUtils.loadJetFile(project, getTestDataFileWithExtension("kt"))
        );
        exhaust.throwIfError();

        BindingContext bindingContext = exhaust.getBindingContext();
        AnalyzerWithCompilerReport.reportDiagnostics(bindingContext, MessageCollectorPlainTextToStream.PLAIN_TEXT_TO_SYSTEM_ERR);

        assertEquals("There should be no diagnostics", 0, Iterables.size(bindingContext.getDiagnostics()));
    }
}
