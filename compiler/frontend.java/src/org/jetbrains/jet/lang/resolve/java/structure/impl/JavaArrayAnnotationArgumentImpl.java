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

package org.jetbrains.jet.lang.resolve.java.structure.impl;

import com.intellij.psi.PsiArrayInitializerMemberValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotationArgument;
import org.jetbrains.jet.lang.resolve.java.structure.JavaArrayAnnotationArgument;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.List;

import static org.jetbrains.jet.lang.resolve.java.structure.impl.JavaElementCollectionFromPsiArrayUtil.namelessAnnotationArguments;

public class JavaArrayAnnotationArgumentImpl extends JavaAnnotationArgumentImpl<PsiArrayInitializerMemberValue>
        implements JavaArrayAnnotationArgument {
    protected JavaArrayAnnotationArgumentImpl(@NotNull PsiArrayInitializerMemberValue psiArrayInitializerMemberValue, @Nullable Name name) {
        super(psiArrayInitializerMemberValue, name);
    }

    @Override
    @NotNull
    public List<JavaAnnotationArgument> getElements() {
        return namelessAnnotationArguments(getPsi().getInitializers());
    }
}
