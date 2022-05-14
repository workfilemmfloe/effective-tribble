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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentUtil;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.di.InjectorForJavaSemanticServices;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.jetbrains.jet.JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations;
import static org.jetbrains.jet.codegen.GenerationUtils.compileFilesGetGenerationState;
import static org.jetbrains.jet.lang.psi.JetPsiFactory.createFile;

public final class LoadDescriptorUtil {

    @NotNull
    public static final FqName TEST_PACKAGE_FQNAME = FqName.topLevel(Name.identifier("test"));

    private LoadDescriptorUtil() {
    }

    @NotNull
    public static NamespaceDescriptor compileKotlinAndLoadTestNamespaceDescriptorFromBinary(
            @NotNull File kotlinFile,
            @NotNull File outDir,
            @NotNull Disposable disposable,
            @NotNull ConfigurationKind configurationKind
    )
            throws IOException {
        compileKotlinToDirAndGetAnalyzeExhaust(kotlinFile, outDir, disposable, configurationKind);
        return loadTestNamespaceAndBindingContextFromBinaries(outDir, disposable, ConfigurationKind.JDK_ONLY).first;
    }

    @NotNull
    public static AnalyzeExhaust compileKotlinToDirAndGetAnalyzeExhaust(
            @NotNull File kotlinFile,
            @NotNull File outDir,
            @NotNull Disposable disposable,
            @NotNull ConfigurationKind configurationKind
    ) throws IOException {
        return compileKotlinToDirAndGetAnalyzeExhaust(kotlinFile, outDir, createEnvironmentWithMockJdkAndIdeaAnnotations(disposable,
                                                                                                                         configurationKind));
    }

    @NotNull
    public static AnalyzeExhaust compileKotlinToDirAndGetAnalyzeExhaust(
            @NotNull File kotlinFile,
            @NotNull File outDir,
            @NotNull JetCoreEnvironment environment
    ) throws IOException {
        JetFileAndExhaust fileAndExhaust = JetFileAndExhaust.createJetFileAndAnalyze(kotlinFile, environment);
        GenerationState state = compileFilesGetGenerationState(fileAndExhaust.getJetFile().getProject(), fileAndExhaust.getExhaust(),
                                                               Collections.singletonList(fileAndExhaust.getJetFile()));
        ClassFileFactory classFileFactory = state.getFactory();
        CompileEnvironmentUtil.writeToOutputDirectory(classFileFactory, outDir);
        return fileAndExhaust.getExhaust();
    }

    @NotNull
    public static Pair<NamespaceDescriptor, BindingContext> loadTestNamespaceAndBindingContextFromBinaries(
            @NotNull File outDir,
            @NotNull Disposable disposable,
            @NotNull ConfigurationKind configurationKind
    ) {
        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(
                configurationKind, TestJdkKind.MOCK_JDK, JetTestUtils.getAnnotationsJar(), outDir,
                ForTestCompileRuntime.runtimeJarForTests());
        JetCoreEnvironment jetCoreEnvironment = new JetCoreEnvironment(disposable, configuration);
        Project project = jetCoreEnvironment.getProject();
        InjectorForJavaSemanticServices injector = new InjectorForJavaSemanticServices(project);
        JavaDescriptorResolver javaDescriptorResolver = injector.getJavaDescriptorResolver();
        NamespaceDescriptor namespaceDescriptor = javaDescriptorResolver.resolveNamespace(TEST_PACKAGE_FQNAME);
        assert namespaceDescriptor != null;
        return Pair.create(namespaceDescriptor, injector.getBindingTrace().getBindingContext());
    }

    @NotNull
    public static Pair<NamespaceDescriptor, BindingContext> compileJavaAndLoadTestNamespaceAndBindingContextFromBinary(
            @NotNull Collection<File> javaFiles,
            @NotNull File outDir,
            @NotNull Disposable disposable,
            @NotNull ConfigurationKind configurationKind
    )
            throws IOException {
        compileJavaWithAnnotationsJar(javaFiles, outDir);
        return loadTestNamespaceAndBindingContextFromBinaries(outDir, disposable, configurationKind);
    }

    private static void compileJavaWithAnnotationsJar(@NotNull Collection<File> javaFiles, @NotNull File outDir) throws IOException {
        String classPath = "out/production/runtime" + File.pathSeparator + JetTestUtils.getAnnotationsJar().getPath();
        JetTestUtils.compileJavaFiles(javaFiles, Arrays.asList(
                "-classpath", classPath,
                "-d", outDir.getPath()
        ));
    }

    @NotNull
    public static NamespaceDescriptor analyzeKotlinAndLoadTestNamespace(
            @NotNull File ktFile,
            @NotNull Disposable disposable,
            @NotNull ConfigurationKind configurationKind
    ) throws Exception {
        JetFileAndExhaust fileAndExhaust = JetFileAndExhaust.createJetFileAndAnalyze(ktFile, createEnvironmentWithMockJdkAndIdeaAnnotations(
                disposable, configurationKind));
        //noinspection ConstantConditions
        return fileAndExhaust.getExhaust().getBindingContext().get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, TEST_PACKAGE_FQNAME);
    }

    private static class JetFileAndExhaust {

        @NotNull
        public static JetFileAndExhaust createJetFileAndAnalyze(
                @NotNull File kotlinFile, @NotNull JetCoreEnvironment environment
        )
                throws IOException {
            JetFile jetFile = createFile(environment.getProject(), FileUtil.loadFile(kotlinFile, true));
            AnalyzeExhaust exhaust = AnalyzerFacadeForJVM.analyzeOneFileWithJavaIntegrationAndCheckForErrors(
                    jetFile, Collections.<AnalyzerScriptParameter>emptyList());
            return new JetFileAndExhaust(jetFile, exhaust);
        }

        @NotNull
        private final JetFile jetFile;
        @NotNull
        private final AnalyzeExhaust exhaust;

        private JetFileAndExhaust(@NotNull JetFile file, @NotNull AnalyzeExhaust exhaust) {
            this.jetFile = file;
            this.exhaust = exhaust;
        }

        @NotNull
        public JetFile getJetFile() {
            return jetFile;
        }

        @NotNull
        public AnalyzeExhaust getExhaust() {
            return exhaust;
        }
    }
}
