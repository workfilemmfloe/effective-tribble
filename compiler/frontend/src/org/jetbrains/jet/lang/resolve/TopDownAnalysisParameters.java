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

package org.jetbrains.jet.lang.resolve;

import com.google.common.base.Predicate;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.context.GlobalContext;
import org.jetbrains.jet.context.LazinessToken;
import org.jetbrains.jet.storage.ExceptionTracker;
import org.jetbrains.jet.storage.StorageManager;

/**
 * Various junk that cannot be placed into context (yet).
 */
public class TopDownAnalysisParameters extends LazinessToken implements GlobalContext {
    private static boolean LAZY;

    static {
        LAZY = "true".equals(System.getProperty("lazy.tda"));
    }

    @NotNull
    public static TopDownAnalysisParameters create(
            @NotNull StorageManager storageManager,
            @NotNull ExceptionTracker exceptionTracker,
            @NotNull Predicate<PsiFile> analyzeCompletely,
            boolean analyzingBootstrapLibrary,
            boolean declaredLocally
    ) {
        return new TopDownAnalysisParameters(storageManager, exceptionTracker, analyzeCompletely, analyzingBootstrapLibrary,
                                             declaredLocally, LAZY);
    }

    @NotNull
    public static TopDownAnalysisParameters createForLazy(
            @NotNull StorageManager storageManager,
            @NotNull ExceptionTracker exceptionTracker,
            @NotNull Predicate<PsiFile> analyzeCompletely,
            boolean analyzingBootstrapLibrary,
            boolean declaredLocally
    ) {
        return new TopDownAnalysisParameters(storageManager, exceptionTracker, analyzeCompletely, analyzingBootstrapLibrary,
                                             declaredLocally, true);
    }

    @NotNull
    public static TopDownAnalysisParameters createForLocalDeclarations(
            @NotNull StorageManager storageManager,
            @NotNull ExceptionTracker exceptionTracker,
            @NotNull Predicate<PsiFile> analyzeCompletely
    ) {
        return new TopDownAnalysisParameters(storageManager, exceptionTracker, analyzeCompletely, false, true, false);
    }

    @NotNull private final StorageManager storageManager;
    @NotNull private final ExceptionTracker exceptionTracker;
    @NotNull private final Predicate<PsiFile> analyzeCompletely;
    private final boolean analyzingBootstrapLibrary;
    private final boolean declaredLocally;
    private final boolean lazyTopDownAnalysis;

    private TopDownAnalysisParameters(
            @NotNull StorageManager storageManager,
            @NotNull ExceptionTracker exceptionTracker,
            @NotNull Predicate<PsiFile> analyzeCompletely,
            boolean analyzingBootstrapLibrary,
            boolean declaredLocally,
            boolean lazyTopDownAnalysis
    ) {
        this.storageManager = storageManager;
        this.exceptionTracker = exceptionTracker;
        this.analyzeCompletely = analyzeCompletely;
        this.analyzingBootstrapLibrary = analyzingBootstrapLibrary;
        this.declaredLocally = declaredLocally;
        this.lazyTopDownAnalysis = lazyTopDownAnalysis;
    }

    @Override
    @NotNull
    public StorageManager getStorageManager() {
        return storageManager;
    }

    @Override
    @NotNull
    public ExceptionTracker getExceptionTracker() {
        return exceptionTracker;
    }

    @NotNull
    public Predicate<PsiFile> getAnalyzeCompletely() {
        return analyzeCompletely;
    }

    public boolean isAnalyzingBootstrapLibrary() {
        return analyzingBootstrapLibrary;
    }

    public boolean isDeclaredLocally() {
        return declaredLocally;
    }

    // Used temporarily while we are transitioning from eager to lazy analysis of headers in the IDE
    @Override
    @Deprecated
    public boolean isLazy() {
        return lazyTopDownAnalysis;
    }
}
