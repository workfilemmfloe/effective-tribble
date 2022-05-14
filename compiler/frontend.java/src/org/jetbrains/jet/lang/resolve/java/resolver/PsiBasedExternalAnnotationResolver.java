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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiModifierListOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotation;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotationOwner;
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaAnnotationImpl;
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaAnnotationOwnerImpl;
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaElementCollectionFromPsiArrayUtil;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;
import java.util.Collections;

public class PsiBasedExternalAnnotationResolver implements ExternalAnnotationResolver {
    @Nullable
    @Override
    public JavaAnnotation findExternalAnnotation(@NotNull JavaAnnotationOwner owner, @NotNull FqName fqName) {
        PsiAnnotation psiAnnotation = findExternalAnnotation(((JavaAnnotationOwnerImpl) owner).getPsi(), fqName);
        return psiAnnotation == null ? null : new JavaAnnotationImpl(psiAnnotation);
    }

    @NotNull
    @Override
    public Collection<JavaAnnotation> findExternalAnnotations(@NotNull JavaAnnotationOwner owner) {
        PsiModifierListOwner psiOwner = ((JavaAnnotationOwnerImpl) owner).getPsi();
        PsiAnnotation[] annotations = ExternalAnnotationsManager.getInstance(psiOwner.getProject()).findExternalAnnotations(psiOwner);
        return annotations == null
               ? Collections.<JavaAnnotation>emptyList()
               : JavaElementCollectionFromPsiArrayUtil.annotations(annotations);
    }

    @Nullable
    public static PsiAnnotation findExternalAnnotation(@NotNull PsiModifierListOwner owner, @NotNull FqName fqName) {
        return ExternalAnnotationsManager.getInstance(owner.getProject()).findExternalAnnotation(owner, fqName.asString());
    }
}
