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

package org.jetbrains.kotlin.cli.jvm.compiler;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import kotlin.Function1;
import kotlin.Unit;
import kotlin.io.IoPackage;
import kotlin.modules.AllModules;
import kotlin.modules.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil;
import org.jetbrains.kotlin.cli.common.modules.ModuleScriptData;
import org.jetbrains.kotlin.cli.common.modules.ModuleXmlParser;
import org.jetbrains.kotlin.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.kotlin.codegen.ClassFileFactory;
import org.jetbrains.kotlin.codegen.GeneratedClassLoader;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.idea.JetFileType;
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.utils.KotlinPaths;
import org.jetbrains.kotlin.utils.PathUtil;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.io.*;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.jar.*;

import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation.NO_LOCATION;
import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR;

public class CompileEnvironmentUtil {

    @NotNull
    public static ModuleScriptData loadModuleDescriptions(KotlinPaths paths, String moduleDefinitionFile, MessageCollector messageCollector) {
        File file = new File(moduleDefinitionFile);
        if (!file.exists()) {
            messageCollector.report(ERROR, "Module definition file does not exist: " + moduleDefinitionFile, NO_LOCATION);
            return ModuleScriptData.EMPTY;
        }
        String extension = FileUtilRt.getExtension(moduleDefinitionFile);
        if ("ktm".equalsIgnoreCase(extension)) {
            return loadModuleScript(paths, moduleDefinitionFile, messageCollector);
        }
        if ("xml".equalsIgnoreCase(extension)) {
            return ModuleXmlParser.parseModuleScript(moduleDefinitionFile, messageCollector);
        }
        messageCollector.report(ERROR, "Unknown module definition type: " + moduleDefinitionFile, NO_LOCATION);
        return ModuleScriptData.EMPTY;
    }

    @NotNull
    private static ModuleScriptData loadModuleScript(KotlinPaths paths, String moduleScriptFile, MessageCollector messageCollector) {
        CompilerConfiguration configuration = new CompilerConfiguration();
        File runtimePath = paths.getRuntimePath();
        if (runtimePath.exists()) {
            configuration.add(JVMConfigurationKeys.CLASSPATH_KEY, runtimePath);
        }
        configuration.addAll(JVMConfigurationKeys.CLASSPATH_KEY, PathUtil.getJdkClassesRoots());
        File jdkAnnotationsPath = paths.getJdkAnnotationsPath();
        if (jdkAnnotationsPath.exists()) {
            configuration.add(JVMConfigurationKeys.ANNOTATIONS_PATH_KEY, jdkAnnotationsPath);
        }
        configuration.add(CommonConfigurationKeys.SOURCE_ROOTS_KEY, moduleScriptFile);
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector);

        List<Module> modules;

        Disposable disposable = Disposer.newDisposable();
        try {
            KotlinCoreEnvironment scriptEnvironment =
                    KotlinCoreEnvironment.createForProduction(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
            GenerationState generationState = KotlinToJVMBytecodeCompiler.analyzeAndGenerate(scriptEnvironment);
            if (generationState == null) {
                throw new CompileEnvironmentException("Module script " + moduleScriptFile + " analyze failed:\n" +
                                                      loadModuleScriptText(moduleScriptFile));
            }

            modules = runDefineModules(paths, generationState.getFactory());
        }
        finally {
            Disposer.dispose(disposable);
        }

        if (modules == null) {
            throw new CompileEnvironmentException("Module script " + moduleScriptFile + " compilation failed");
        }

        if (modules.isEmpty()) {
            throw new CompileEnvironmentException("No modules where defined by " + moduleScriptFile);
        }
        return new ModuleScriptData(modules);
    }

    private static List<Module> runDefineModules(KotlinPaths paths, ClassFileFactory factory) {
        File stdlibJar = paths.getRuntimePath();
        GeneratedClassLoader loader;
        if (stdlibJar.exists()) {
            try {
                loader = new GeneratedClassLoader(factory, new URLClassLoader(new URL[]{stdlibJar.toURI().toURL()},
                                                                              AllModules.class.getClassLoader()));
            }
            catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            loader = new GeneratedClassLoader(factory, KotlinToJVMBytecodeCompiler.class.getClassLoader());
        }
        try {
            Class<?> packageClass = loader.loadClass(PackageClassUtils.getPackageClassName(FqName.ROOT));
            Method method = packageClass.getDeclaredMethod("project");

            method.setAccessible(true);
            method.invoke(null);

            List<Module> answer = new ArrayList<Module>(AllModules.INSTANCE$.get());
            AllModules.INSTANCE$.get().clear();
            return answer;
        }
        catch (Exception e) {
            throw new ModuleExecutionException(e);
        }
        finally {
            loader.dispose();
        }
    }

    // TODO: includeRuntime should be not a flag but a path to runtime
    private static void doWriteToJar(ClassFileFactory outputFiles, OutputStream fos, @Nullable FqName mainClass, boolean includeRuntime) {
        try {
            Manifest manifest = new Manifest();
            Attributes mainAttributes = manifest.getMainAttributes();
            mainAttributes.putValue("Manifest-Version", "1.0");
            mainAttributes.putValue("Created-By", "JetBrains Kotlin");
            if (mainClass != null) {
                mainAttributes.putValue("Main-Class", mainClass.asString());
            }
            JarOutputStream stream = new JarOutputStream(fos, manifest);
            for (OutputFile outputFile : outputFiles.asList()) {
                stream.putNextEntry(new JarEntry(outputFile.getRelativePath()));
                stream.write(outputFile.asByteArray());
            }
            if (includeRuntime) {
                writeRuntimeToJar(stream);
            }
            stream.finish();
        }
        catch (IOException e) {
            throw new CompileEnvironmentException("Failed to generate jar file", e);
        }
    }

    public static void writeToJar(File jarPath, boolean jarRuntime, FqName mainClass, ClassFileFactory outputFiles) {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(jarPath);
            doWriteToJar(outputFiles, outputStream, mainClass, jarRuntime);
            outputStream.close();
        }
        catch (FileNotFoundException e) {
            throw new CompileEnvironmentException("Invalid jar path " + jarPath, e);
        }
        catch (IOException e) {
            throw UtilsPackage.rethrow(e);
        }
        finally {
            UtilsPackage.closeQuietly(outputStream);
        }
    }

    private static void writeRuntimeToJar(JarOutputStream stream) throws IOException {
        File runtimePath = PathUtil.getKotlinPathsForCompiler().getRuntimePath();
        if (!runtimePath.exists()) {
            throw new CompileEnvironmentException("Couldn't find runtime library");
        }

        JarInputStream jis = new JarInputStream(new FileInputStream(runtimePath));
        try {
            while (true) {
                JarEntry e = jis.getNextJarEntry();
                if (e == null) {
                    break;
                }
                if (FileUtilRt.extensionEquals(e.getName(), "class")) {
                    stream.putNextEntry(e);
                    FileUtil.copy(jis, stream);
                }
            }
        }
        finally {
            jis.close();
        }
    }

    // Used for debug output only
    private static String loadModuleScriptText(String moduleScriptFile) {
        try {
            return FileUtil.loadFile(new File(moduleScriptFile));
        }
        catch (IOException e) {
            return "Can't load module script text:\n" + OutputMessageUtil.renderException(e);
        }
    }

    @NotNull
    public static List<JetFile> getJetFiles(
            @NotNull final Project project,
            @NotNull Collection<String> sourceRoots,
            @NotNull Function1<String, Unit> reportError
    ) {
        final VirtualFileSystem localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL);

        final Set<VirtualFile> processedFiles = Sets.newHashSet();
        final List<JetFile> result = Lists.newArrayList();

        for (String sourceRootPath : sourceRoots) {
            if (sourceRootPath == null) {
                continue;
            }

            VirtualFile vFile = localFileSystem.findFileByPath(sourceRootPath);
            if (vFile == null) {
                reportError.invoke("Source file or directory not found: " + sourceRootPath);
                continue;
            }
            if (!vFile.isDirectory() && vFile.getFileType() != JetFileType.INSTANCE) {
                reportError.invoke("Source entry is not a Kotlin file: " + sourceRootPath);
                continue;
            }

            IoPackage.recurse(new File(sourceRootPath), new Function1<File, Unit>() {
                @Override
                public Unit invoke(File file) {
                    if (file.isFile()) {
                        VirtualFile virtualFile = localFileSystem.findFileByPath(file.getAbsolutePath());
                        if (virtualFile != null && !processedFiles.contains(virtualFile)) {
                            processedFiles.add(virtualFile);
                            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                            if (psiFile instanceof JetFile) {
                                result.add((JetFile) psiFile);
                            }
                        }
                    }
                    return Unit.INSTANCE$;
                }
            });
        }

        return result;
    }
}
