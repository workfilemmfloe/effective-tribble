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

package org.jetbrains.kotlin.android.tests;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection;
import org.jetbrains.kotlin.cli.common.output.outputUtils.OutputUtilsKt;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.codegen.CodegenTestFiles;
import org.jetbrains.kotlin.codegen.GenerationUtils;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.JVMConfigurationKeys;
import org.jetbrains.kotlin.idea.KotlinFileType;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.*;
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase;
import org.jetbrains.kotlin.utils.Printer;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CodegenTestsOnAndroidGenerator extends KtUsefulTestCase {

    private final PathManager pathManager;
    private static final String testClassPackage = "org.jetbrains.kotlin.android.tests";
    private static final String testClassName = "CodegenTestCaseOnAndroid";
    private static final String baseTestClassPackage = "org.jetbrains.kotlin.android.tests";
    private static final String baseTestClassName = "AbstractCodegenTestCaseOnAndroid";
    private static final String generatorName = "CodegenTestsOnAndroidGenerator";

    private static int MODULE_INDEX = 1;

    private final List<String> generatedTestNames = Lists.newArrayList();

    public static void generate(PathManager pathManager) throws Throwable {
        new CodegenTestsOnAndroidGenerator(pathManager).generateOutputFiles();
    }

    private CodegenTestsOnAndroidGenerator(PathManager pathManager) {
        this.pathManager = pathManager;
    }

    private void generateOutputFiles() throws Throwable {
        prepareAndroidModule();
        generateAndSave();
    }

    private void prepareAndroidModule() throws IOException {
        System.out.println("Copying kotlin-runtime.jar and kotlin-reflect.jar in android module...");
        copyKotlinRuntimeJars();

        System.out.println("Check \"libs\" folder in tested android module...");
        File libsFolderInTestedModule = new File(pathManager.getLibsFolderInAndroidTestedModuleTmpFolder());
        if (!libsFolderInTestedModule.exists()) {
            libsFolderInTestedModule.mkdirs();
        }
    }

    private void copyKotlinRuntimeJars() throws IOException {
        FileUtil.copy(
                ForTestCompileRuntime.runtimeJarForTests(),
                new File(pathManager.getLibsFolderInAndroidTmpFolder() + "/kotlin-runtime.jar")
        );
        FileUtil.copy(
                ForTestCompileRuntime.reflectJarForTests(),
                new File(pathManager.getLibsFolderInAndroidTmpFolder() + "/kotlin-reflect.jar")
        );

        FileUtil.copy(
                ForTestCompileRuntime.kotlinTestJarForTests(),
                new File(pathManager.getLibsFolderInAndroidTmpFolder() + "/kotlin-test.jar")
        );
    }

    private void generateAndSave() throws Throwable {
        System.out.println("Generating test files...");
        StringBuilder out = new StringBuilder();
        Printer p = new Printer(out);

        p.print(FileUtil.loadFile(new File("license/LICENSE.txt")));
        p.println("package " + testClassPackage + ";");
        p.println();
        p.println("import ", baseTestClassPackage, ".", baseTestClassName, ";");
        p.println();
        p.println("/* This class is generated by " + generatorName + ". DO NOT MODIFY MANUALLY */");
        p.println("public class ", testClassName, " extends ", baseTestClassName, " {");
        p.pushIndent();

        generateTestMethodsForDirectories(p, new File("compiler/testData/codegen/box"), new File("compiler/testData/codegen/boxInline"));

        p.popIndent();
        p.println("}");

        String testSourceFilePath =
                pathManager.getSrcFolderInAndroidTmpFolder() + "/" + testClassPackage.replace(".", "/") + "/" + testClassName + ".java";
        FileUtil.writeToFile(new File(testSourceFilePath), out.toString());
    }

    private void generateTestMethodsForDirectories(Printer p, File... dirs) throws IOException {
        FilesWriter holderMock = new FilesWriter(false, false);
        FilesWriter holderFull = new FilesWriter(true, false);
        FilesWriter holderInheritMFP = new FilesWriter(true, true);

        for (File dir : dirs) {
            File[] files = dir.listFiles();
            Assert.assertNotNull("Folder with testData is empty: " + dir.getAbsolutePath(), files);
            processFiles(p, files, holderFull, holderMock, holderInheritMFP);
        }

        holderFull.writeFilesOnDisk();
        holderMock.writeFilesOnDisk();
        holderInheritMFP.writeFilesOnDisk();
    }

    class FilesWriter {
        private final boolean isFullJdkAndRuntime;
        private final boolean inheritMultifileParts;

        public List<KtFile> files = new ArrayList<KtFile>();
        private KotlinCoreEnvironment environment;

        private FilesWriter(boolean isFullJdkAndRuntime, boolean inheritMultifileParts) {
            this.isFullJdkAndRuntime = isFullJdkAndRuntime;
            this.inheritMultifileParts = inheritMultifileParts;
            this.environment = createEnvironment(isFullJdkAndRuntime);
        }

        private KotlinCoreEnvironment createEnvironment(boolean isFullJdkAndRuntime) {
            ConfigurationKind configurationKind = isFullJdkAndRuntime ? ConfigurationKind.ALL : ConfigurationKind.NO_KOTLIN_REFLECT;
            TestJdkKind testJdkKind = isFullJdkAndRuntime ? TestJdkKind.FULL_JDK : TestJdkKind.MOCK_JDK;
            CompilerConfiguration configuration =
                    KotlinTestUtils.newConfiguration(configurationKind, testJdkKind, KotlinTestUtils.getAnnotationsJar());
            configuration.put(CommonConfigurationKeys.MODULE_NAME, "android-module-" + MODULE_INDEX++);
            if (inheritMultifileParts) {
                configuration.put(JVMConfigurationKeys.INHERIT_MULTIFILE_PARTS, true);
            }
            return KotlinCoreEnvironment.createForTests(myTestRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
        }

        public boolean shouldWriteFilesOnDisk() {
            return files.size() > 300;
        }

        public void writeFilesOnDiskIfNeeded() {
            if (shouldWriteFilesOnDisk()) {
                writeFilesOnDisk();
            }
        }

        public void writeFilesOnDisk() {
            writeFiles(files);
            files = new ArrayList<KtFile>();
            environment = createEnvironment(isFullJdkAndRuntime);
        }

        public void addFile(String name, String content) {
            try {
                files.add(CodegenTestFiles.create(name, content, environment.getProject()).getPsiFile());
            }
            catch (Throwable e) {
                throw new RuntimeException("Problem during creating file " + name + ": \n" + content, e);
            }
        }

        private void writeFiles(List<KtFile> filesToCompile) {
            if (filesToCompile.isEmpty()) return;

            System.out.println("Generating " + filesToCompile.size() + " files" +
                               (inheritMultifileParts
                                ? " (JVM.INHERIT_MULTIFILE_PARTS)"
                                : isFullJdkAndRuntime ? " (full jdk and runtime)" : "") + "...");
            OutputFileCollection outputFiles;
            try {
                outputFiles = GenerationUtils.compileFiles(filesToCompile, environment).getFactory();
            }
            catch (Throwable e) {
                throw new RuntimeException(e);
            }

            File outputDir = new File(pathManager.getOutputForCompiledFiles());
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            Assert.assertTrue("Cannot create directory for compiled files", outputDir.exists());

            OutputUtilsKt.writeAllTo(outputFiles, outputDir);
        }
    }

    private void processFiles(
            @NotNull Printer printer,
            @NotNull File[] files,
            @NotNull FilesWriter holderFull,
            @NotNull FilesWriter holderMock,
            @NotNull FilesWriter holderInheritMFP
    ) throws IOException {
        holderFull.writeFilesOnDiskIfNeeded();
        holderMock.writeFilesOnDiskIfNeeded();
        holderInheritMFP.writeFilesOnDiskIfNeeded();

        for (File file : files) {
            if (SpecialFiles.getExcludedFiles().contains(file.getName())) {
                continue;
            }
            if (file.isDirectory()) {
                File[] listFiles = file.listFiles();
                if (listFiles != null) {
                    processFiles(printer, listFiles, holderFull, holderMock, holderInheritMFP);
                }
            }
            else if (!FileUtilRt.getExtension(file.getName()).equals(KotlinFileType.INSTANCE.getDefaultExtension())) {
                // skip non kotlin files
            }
            else {
                String fullFileText = FileUtil.loadFile(file, true);

                if (!InTextDirectivesUtils.isPassingTarget(TargetBackend.JVM, file)) {
                    continue;
                }

                //TODO: support multifile facades
                //TODO: support multifile facades hierarchies
                if (hasBoxMethod(fullFileText)) {
                    FilesWriter filesHolder = InTextDirectivesUtils.isDirectiveDefined(fullFileText, "FULL_JDK") ||
                                              InTextDirectivesUtils.isDirectiveDefined(fullFileText, "WITH_RUNTIME") ||
                                              InTextDirectivesUtils.isDirectiveDefined(fullFileText, "WITH_REFLECT") ? holderFull : holderMock;
                    filesHolder = fullFileText.contains("+JVM.INHERIT_MULTIFILE_PARTS") ? holderInheritMFP : filesHolder;

                    FqName classWithBoxMethod = AndroidTestGeneratorKt.genFiles(file, fullFileText, filesHolder);
                    if (classWithBoxMethod == null)
                        continue;

                    String generatedTestName = generateTestName(file.getName());
                    generateTestMethod(printer, generatedTestName, classWithBoxMethod.asString(), StringUtil.escapeStringCharacters(file.getPath()));
                }
            }
        }
    }



    private static boolean hasBoxMethod(String text) {
        return text.contains("fun box()");
    }

    private static void generateTestMethod(Printer p, String testName, String className, String filePath) {
        p.println("public void test" + testName + "() throws Exception {");
        p.pushIndent();
        p.println("invokeBoxMethod(" + className + ".class, \"" + filePath + "\", \"OK\");");
        p.popIndent();
        p.println("}");
        p.println();
    }

    private String generateTestName(String fileName) {
        String result = JvmAbi.sanitizeAsJavaIdentifier(FileUtil.getNameWithoutExtension(StringUtil.capitalize(fileName)));

        int i = 0;
        while (generatedTestNames.contains(result)) {
            result += "_" + i++;
        }
        generatedTestNames.add(result);
        return result;
    }
}
