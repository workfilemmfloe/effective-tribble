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

package org.jetbrains.kotlin.resolve;

import com.intellij.psi.PsiElement;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider;
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetScript;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer;
import org.jetbrains.kotlin.resolve.lazy.LazyFileScope;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class LazyTopDownAnalyzerForTopLevel {

    @NotNull private final KotlinCodeAnalyzer codeAnalyzer;
    @NotNull private final LazyTopDownAnalyzer lazyTopDownAnalyzer;

    public LazyTopDownAnalyzerForTopLevel(
            @NotNull LazyTopDownAnalyzer lazyTopDownAnalyzer,
            @NotNull KotlinCodeAnalyzer codeAnalyzer
    ) {
        this.lazyTopDownAnalyzer = lazyTopDownAnalyzer;
        this.codeAnalyzer = codeAnalyzer;
    }

    @NotNull
    public TopDownAnalysisContext analyzeFiles(
            @NotNull TopDownAnalysisMode topDownAnalysisMode,
            @NotNull Collection<JetFile> files,
            @NotNull List<? extends PackageFragmentProvider> additionalProviders
    ) {
        PackageFragmentProvider provider;
        if (additionalProviders.isEmpty()) {
            provider = codeAnalyzer.getPackageFragmentProvider();
        }
        else {
            provider = new CompositePackageFragmentProvider(KotlinPackage.plus(
                    Arrays.asList(codeAnalyzer.getPackageFragmentProvider()),
                    additionalProviders));
        }

        ((ModuleDescriptorImpl) codeAnalyzer.getModuleDescriptor()).initialize(provider);

        return analyzeDeclarations(topDownAnalysisMode, files);
    }

    @NotNull
    public TopDownAnalysisContext analyzeDeclarations(
            @NotNull TopDownAnalysisMode topDownAnalysisMode,
            @NotNull Collection<? extends PsiElement> elements
    ) {
        TopDownAnalysisContext c = lazyTopDownAnalyzer.analyzeDeclarations(topDownAnalysisMode, elements, DataFlowInfo.EMPTY);

        resolveImportsInAllFiles(c, codeAnalyzer);

        return c;
    }

    private static void resolveImportsInAllFiles(TopDownAnalysisContext c, KotlinCodeAnalyzer resolveSession) {
        for (JetFile file : c.getFiles()) {
            resolveAndCheckImports(file, resolveSession);
        }

        for (JetScript script : c.getScripts().keySet()) {
            resolveAndCheckImports(script.getContainingJetFile(), resolveSession);
        }
    }

    private static void resolveAndCheckImports(@NotNull JetFile file, @NotNull KotlinCodeAnalyzer resolveSession) {
        LazyFileScope fileScope = resolveSession.getFileScopeProvider().getFileScope(file);
        fileScope.forceResolveAllImports();
    }
}


