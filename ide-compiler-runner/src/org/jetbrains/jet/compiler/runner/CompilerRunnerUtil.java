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

package org.jetbrains.jet.compiler.runner;

import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.messages.*;
import org.jetbrains.jet.utils.KotlinPaths;

import java.io.*;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static org.jetbrains.jet.cli.common.messages.CompilerMessageLocation.NO_LOCATION;
import static org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity.*;

public class CompilerRunnerUtil {

    private static SoftReference<URLClassLoader> ourClassLoaderRef = new SoftReference<URLClassLoader>(null);

    public static List<File> kompilerClasspath(KotlinPaths paths, MessageCollector messageCollector) {
        File libs = paths.getLibPath();

        if (!libs.exists() || libs.isFile()) {
            messageCollector.report(ERROR, "Broken compiler at '" + libs.getAbsolutePath() + "'. Make sure plugin is properly installed", NO_LOCATION);
            return Collections.emptyList();
        }

        ArrayList<File> answer = new ArrayList<File>();
        answer.add(new File(libs, "kotlin-compiler.jar"));
        return answer;
    }

    public static URLClassLoader getOrCreateClassLoader(KotlinPaths paths, MessageCollector messageCollector) {
        URLClassLoader answer = ourClassLoaderRef.get();
        if (answer == null) {
            answer = createClassloader(paths, messageCollector);
            ourClassLoaderRef = new SoftReference<URLClassLoader>(answer);
        }
        return answer;
    }

    private static URLClassLoader createClassloader(KotlinPaths paths, MessageCollector messageCollector) {
        List<File> jars = kompilerClasspath(paths, messageCollector);
        URL[] urls = new URL[jars.size()];
        for (int i = 0; i < urls.length; i++) {
            try {
                urls[i] = jars.get(i).toURI().toURL();
            }
            catch (MalformedURLException e) {
                throw new RuntimeException(e); // Checked exceptions are great! I love them, and I love brilliant library designers too!
            }
        }
        return new URLClassLoader(urls, null);
    }

    static void handleProcessTermination(int exitCode, MessageCollector messageCollector) {
        if (exitCode != 0 && exitCode != 1) {
            messageCollector.report(ERROR, "Compiler terminated with exit code: " + exitCode, NO_LOCATION);
        }
    }

    public static int getReturnCodeFromObject(Object rc) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if ("org.jetbrains.jet.cli.common.ExitCode".equals(rc.getClass().getCanonicalName())) {
            return (Integer)rc.getClass().getMethod("getCode").invoke(rc);
        }
        else {
            throw new IllegalStateException("Unexpected return: " + rc);
        }
    }

    public static Object invokeExecMethod(CompilerEnvironment environment,
            PrintStream out,
            MessageCollector messageCollector, String[] arguments, String name) throws Exception {
        URLClassLoader loader = getOrCreateClassLoader(environment.getKotlinPaths(), messageCollector);
        Class<?> kompiler = Class.forName(name, true, loader);
        Method exec = kompiler.getMethod("exec", PrintStream.class, String[].class);
        return exec.invoke(kompiler.newInstance(), out, arguments);
    }

    public static void outputCompilerMessagesAndHandleExitCode(@NotNull MessageCollector messageCollector,
            @NotNull OutputItemsCollector outputItemsCollector,
            @NotNull Function<PrintStream, Integer> compilerRun) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outputStream);

        int exitCode = compilerRun.fun(out);

        BufferedReader reader = new BufferedReader(new StringReader(outputStream.toString()));
        CompilerOutputParser.parseCompilerMessagesFromReader(messageCollector, reader, outputItemsCollector);
        handleProcessTermination(exitCode, messageCollector);
    }
}
