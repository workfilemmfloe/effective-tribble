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

package org.jetbrains.jet.lang.resolve.java.wrapper;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.kt.JetClassAnnotation;
import org.jetbrains.jet.lang.resolve.java.kt.JetPackageClassAnnotation;

public class PsiClassWrapper {

    @NotNull
    private final PsiClass psiClass;

    public PsiClassWrapper(@NotNull PsiClass psiClass) {
        this.psiClass = psiClass;
    }

    public String getQualifiedName() {
        return psiClass.getQualifiedName();
    }

    @NotNull
    public PsiClass getPsiClass() {
        return psiClass;
    }
    
    private JetClassAnnotation jetClass;
    private JetPackageClassAnnotation jetPackageClass;

    @NotNull
    public JetClassAnnotation getJetClass() {
        if (jetClass == null) {
            jetClass = JetClassAnnotation.get(psiClass);
        }
        return jetClass;
    }

    @NotNull
    public JetPackageClassAnnotation getJetPackageClass() {
        if (jetPackageClass == null) {
            jetPackageClass = JetPackageClassAnnotation.get(psiClass);
        }
        return jetPackageClass;
    }
}
