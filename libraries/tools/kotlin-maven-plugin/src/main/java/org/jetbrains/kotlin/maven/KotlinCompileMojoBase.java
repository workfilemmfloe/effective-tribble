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

package org.jetbrains.kotlin.maven;

import com.intellij.util.ArrayUtil;
import com.sampullara.cli.Args;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.CLICompiler;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.common.KotlinVersion;
import org.jetbrains.jet.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.config.Services;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public abstract class KotlinCompileMojoBase<A extends CommonCompilerArguments> extends AbstractMojo {
    // TODO it would be nice to avoid using 2 injected fields for sources
    // but I've not figured out how to have a defaulted parameter value
    // which is also customisable inside an <execution> in a maven pom.xml
    // so for now lets just use 2 fields

    /**
     * The default source directories containing the sources to be compiled.
     *
     * @parameter default-value="${project.compileSourceRoots}"
     * @required
     */
    private List<String> defaultSourceDirs;

    /**
     * The source directories containing the sources to be compiled.
     *
     * @parameter
     */
    private List<String> sourceDirs;

    public List<String> getSources() {
        if (sourceDirs != null && !sourceDirs.isEmpty()) return sourceDirs;
        return defaultSourceDirs;
    }

    // TODO not sure why this doesn't work :(
    // * @parameter default-value="$(project.basedir}/src/main/resources"

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    public MavenProject project;

    /**
     * The directory for compiled classes.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    public String output;

    /**
     * The directory for compiled tests classes.
     *
     * @parameter default-value="${project.build.testOutputDirectory}"
     * @required
     * @readonly
     */
    public String testOutput;

    /**
     * Kotlin compilation module, as alternative to source files or folders.
     *
     * @parameter
     */
    public String module;

    /**
     * Kotlin compilation module, as alternative to source files or folders (for tests).
     *
     * @parameter
     */
    public String testModule;

    /**
     * Additional command line arguments for Kotlin compiler.
     *
     * @parameter
     */
    public List<String> args;

    protected final Log LOG = getLog();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        LOG.info("Kotlin Compiler version " + KotlinVersion.VERSION);

        // Check sources
        List<String> sources = getSources();
        if (sources != null && sources.size() > 0) {
            boolean sourcesExists = false;

            for (String source : sources) {
                if (new File(source).exists()) {
                    sourcesExists = true;
                    break;
                }
            }

            if (!sourcesExists) {
                LOG.warn("No sources found skipping Kotlin compile");
                return;
            }
        }

        A arguments = createCompilerArguments();
        configureCompilerArguments(arguments);

        CLICompiler<A> compiler = createCompiler();
        printCompilerArgumentsIfDebugEnabled(arguments, compiler);

        MessageCollector messageCollector = new MessageCollector() {
            @Override
            public void report(@NotNull CompilerMessageSeverity severity, @NotNull String message, @NotNull CompilerMessageLocation location) {
                String path = location.getPath();
                String position = path == null ? "" : path + ": (" + (location.getLine() + ", " + location.getColumn()) + ") ";

                String text = position + message;

                if (CompilerMessageSeverity.VERBOSE.contains(severity)) {
                    LOG.debug(text);
                } else if (CompilerMessageSeverity.ERRORS.contains(severity)) {
                    LOG.error(text);
                } else if (severity == CompilerMessageSeverity.INFO) {
                    LOG.info(text);
                } else {
                    LOG.warn(text);
                }
            }
        };

        ExitCode exitCode = executeCompiler(compiler, arguments, messageCollector);

        switch (exitCode) {
            case COMPILATION_ERROR:
                throw new MojoExecutionException("Compilation error. See log for more details");
            case INTERNAL_ERROR:
                throw new MojoExecutionException("Internal compiler error. See log for more details");
            default:
        }
    }

    private void printCompilerArgumentsIfDebugEnabled(@NotNull A arguments, @NotNull CLICompiler<A> compiler) {
        if (getLog().isDebugEnabled()) {
            getLog().debug("Invoking compiler " + compiler + " with arguments:");
            try {
                Field[] fields = arguments.getClass().getFields();
                for (Field f : fields) {
                    Object value = f.get(arguments);

                    String valueString;
                    if (value instanceof Object[]) {
                        valueString = Arrays.deepToString((Object[]) value);
                    }
                    else if (value != null) {
                        valueString = String.valueOf(value);
                    }
                    else {
                        valueString = "(null)";
                    }

                    getLog().debug(f.getName() + "=" + valueString);
                }
                getLog().debug("End of arguments");
            }
            catch (Exception e) {
                getLog().warn("Failed to print compiler arguments: " + e, e);
            }
        }
    }

    @NotNull
    protected abstract CLICompiler<A> createCompiler();

    /**
     * Derived classes can create custom compiler argument implementations
     * such as for KDoc
     */
    @NotNull
    protected abstract A createCompilerArguments();

    @NotNull
    protected ExitCode executeCompiler(
            @NotNull CLICompiler<A> compiler,
            @NotNull A arguments,
            @NotNull MessageCollector messageCollector
    ) {
        return compiler.exec(messageCollector, Services.EMPTY, arguments);
    }

    /**
     * Derived classes can register custom plugins or configurations
     */
    protected abstract void configureSpecificCompilerArguments(@NotNull A arguments) throws MojoExecutionException;

    private void configureCompilerArguments(@NotNull A arguments) throws MojoExecutionException {
        if (LOG.isDebugEnabled()) {
            arguments.verbose = true;
        }

        List<String> sources = getSources();
        if (sources == null || sources.isEmpty()) {
            throw new MojoExecutionException("No source roots to compile");
        }

        arguments.freeArgs.addAll(sources);
        LOG.info("Compiling Kotlin sources from " + sources );

        configureSpecificCompilerArguments(arguments);

        try {
            Args.parse(arguments, ArrayUtil.toStringArray(args));
        }
        catch (IllegalArgumentException e) {
            throw new MojoExecutionException(e.getMessage());
        }

        if (arguments.noInline) {
            LOG.info("Method inlining is turned off");
        }
    }
}
