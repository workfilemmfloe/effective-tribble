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

import com.google.common.base.Predicates;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.common.output.outputUtils.OutputUtilsPackage;
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.GenerationUtils;
import org.jetbrains.jet.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolver;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolverUtil;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.lazy.JvmResolveUtil;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations;

public final class LoadDescriptorUtil {

    @NotNull
    public static final FqName TEST_PACKAGE_FQNAME = FqName.topLevel(Name.identifier("test"));

    private LoadDescriptorUtil() {
    }

    @NotNull
    public static AnalyzeExhaust compileKotlinToDirAndGetAnalyzeExhaust(
            @NotNull List<File> kotlinFiles,
            @NotNull File outDir,
            @NotNull Disposable disposable,
            @NotNull ConfigurationKind configurationKind
    ) {
        JetFilesAndExhaust fileAndExhaust = JetFilesAndExhaust.createJetFilesAndAnalyze(kotlinFiles, disposable, configurationKind);
        AnalyzeExhaust exhaust = fileAndExhaust.getExhaust();
        List<JetFile> files = fileAndExhaust.getJetFiles();
        GenerationState state = GenerationUtils.compileFilesGetGenerationState(files.get(0).getProject(), exhaust, files);
        OutputUtilsPackage.writeAllTo(state.getFactory(), outDir);
        return exhaust;
    }

    @NotNull
    public static Pair<PackageViewDescriptor, BindingContext> loadTestPackageAndBindingContextFromJavaRoot(
            @NotNull File javaRoot,
            @NotNull Disposable disposable,
            @NotNull ConfigurationKind configurationKind
    ) {
        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(
                configurationKind, TestJdkKind.MOCK_JDK,
                JetTestUtils.getAnnotationsJar(),
                javaRoot,
                new File("compiler/tests") // for @ExpectLoadError annotation
        );
        JetCoreEnvironment jetCoreEnvironment = JetCoreEnvironment.createForTests(disposable, configuration);
        BindingTraceContext trace = new BindingTraceContext();
        InjectorForJavaDescriptorResolver injector =
                InjectorForJavaDescriptorResolverUtil.create(jetCoreEnvironment.getProject(), trace, true);
        ModuleDescriptorImpl module = injector.getModule();

        CliLightClassGenerationSupport.getInstanceForCli(jetCoreEnvironment.getProject()).setModule(module);
        PackageViewDescriptor packageView = module.getPackage(TEST_PACKAGE_FQNAME);
        assert packageView != null;

        return Pair.create(packageView, trace.getBindingContext());
    }

    @NotNull
    public static Pair<PackageViewDescriptor, BindingContext> compileJavaAndLoadTestPackageAndBindingContextFromBinary(
            @NotNull Collection<File> javaFiles,
            @NotNull File outDir,
            @NotNull Disposable disposable,
            @NotNull ConfigurationKind configurationKind
    )
            throws IOException {
        compileJavaWithAnnotationsJar(javaFiles, outDir);
        return loadTestPackageAndBindingContextFromJavaRoot(outDir, disposable, configurationKind);
    }

    private static void compileJavaWithAnnotationsJar(@NotNull Collection<File> javaFiles, @NotNull File outDir) throws IOException {
        String classPath = ForTestCompileRuntime.runtimeJarForTests() + File.pathSeparator +
                           JetTestUtils.getAnnotationsJar().getPath();
        JetTestUtils.compileJavaFiles(javaFiles, Arrays.asList(
                "-classpath", classPath,
                "-sourcepath", "compiler/tests", // for @ExpectLoadError annotation
                "-d", outDir.getPath()
        ));
    }

    private static class JetFilesAndExhaust {
        @NotNull
        public static JetFilesAndExhaust createJetFilesAndAnalyze(
                @NotNull List<File> kotlinFiles,
                @NotNull Disposable disposable,
                @NotNull ConfigurationKind configurationKind
        ) {
            final JetCoreEnvironment jetCoreEnvironment = createEnvironmentWithMockJdkAndIdeaAnnotations(disposable, configurationKind);
            List<JetFile> jetFiles = ContainerUtil.map(kotlinFiles, new Function<File, JetFile>() {
                @Override
                public JetFile fun(File kotlinFile) {
                    try {
                        return JetTestUtils.createFile(
                                kotlinFile.getName(), FileUtil.loadFile(kotlinFile, true), jetCoreEnvironment.getProject());
                    }
                    catch (IOException e) {
                        throw new AssertionError(e);
                    }
                }
            });
            AnalyzeExhaust exhaust = JvmResolveUtil.analyzeFilesWithJavaIntegrationAndCheckForErrors(
                    jetCoreEnvironment.getProject(), jetFiles, Predicates.<PsiFile>alwaysTrue());
            return new JetFilesAndExhaust(jetFiles, exhaust);
        }

        private final List<JetFile> jetFiles;
        private final AnalyzeExhaust exhaust;

        private JetFilesAndExhaust(@NotNull List<JetFile> jetFiles, @NotNull AnalyzeExhaust exhaust) {
            this.jetFiles = jetFiles;
            this.exhaust = exhaust;
        }

        @NotNull
        public List<JetFile> getJetFiles() {
            return jetFiles;
        }

        @NotNull
        public AnalyzeExhaust getExhaust() {
            return exhaust;
        }
    }
}
