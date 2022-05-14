/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.highlighter;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.AsJavaPackage;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.Diagnostics;
import org.jetbrains.jet.plugin.util.ProjectRootsUtil;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.project.TargetPlatform;
import org.jetbrains.jet.plugin.project.TargetPlatformDetector;

import static org.jetbrains.jet.plugin.caches.resolve.ResolvePackage.getModuleInfo;

public class DuplicateJvmSignatureAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof JetFile) && !(element instanceof JetDeclaration)) return;
        if (!ProjectRootsUtil.isInProjectSource(element)) return;

        PsiFile file = element.getContainingFile();
        if (!(file instanceof JetFile) || TargetPlatformDetector.getPlatform((JetFile) file) != TargetPlatform.JVM) return;

        Diagnostics otherDiagnostics = ResolvePackage.getBindingContext((JetElement) element).getDiagnostics();
        GlobalSearchScope moduleScope = getModuleInfo(element).contentScope();
        Diagnostics diagnostics = AsJavaPackage.getJvmSignatureDiagnostics(element, otherDiagnostics, moduleScope);
        
        if (diagnostics == null) return;
        JetPsiChecker.annotateElement(element, holder, diagnostics);
    }
}
