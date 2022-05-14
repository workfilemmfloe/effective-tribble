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

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.Visibilities;
import org.jetbrains.jet.lang.descriptors.Visibility;
import org.jetbrains.jet.lang.resolve.java.JavaVisibilities;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotation;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;
import java.util.Collections;

import static org.jetbrains.jet.lang.resolve.java.structure.impl.JavaElementCollectionFromPsiArrayUtil.annotations;

/* package */ class JavaElementUtil {
    private JavaElementUtil() {
    }

    public static boolean isAbstract(@NotNull JavaModifierListOwnerImpl owner) {
        return owner.getPsi().hasModifierProperty(PsiModifier.ABSTRACT);
    }

    public static boolean isStatic(@NotNull JavaModifierListOwnerImpl owner) {
        return owner.getPsi().hasModifierProperty(PsiModifier.STATIC);
    }

    public static boolean isFinal(@NotNull JavaModifierListOwnerImpl owner) {
        return owner.getPsi().hasModifierProperty(PsiModifier.FINAL);
    }

    @NotNull
    public static Visibility getVisibility(@NotNull JavaModifierListOwnerImpl owner) {
        PsiModifierListOwner psiOwner = owner.getPsi();
        if (psiOwner.hasModifierProperty(PsiModifier.PUBLIC)) {
            return Visibilities.PUBLIC;
        }
        if (psiOwner.hasModifierProperty(PsiModifier.PRIVATE)) {
            return Visibilities.PRIVATE;
        }
        if (psiOwner.hasModifierProperty(PsiModifier.PROTECTED)) {
            return owner.isStatic() ? JavaVisibilities.PROTECTED_STATIC_VISIBILITY : JavaVisibilities.PROTECTED_AND_PACKAGE;
        }
        return JavaVisibilities.PACKAGE_VISIBILITY;
    }

    @NotNull
    public static Collection<JavaAnnotation> getAnnotations(@NotNull JavaAnnotationOwnerImpl owner) {
        PsiModifierList modifierList = owner.getPsi().getModifierList();
        if (modifierList != null) {
            return annotations(modifierList.getAnnotations());
        }
        return Collections.emptyList();
    }

    @Nullable
    public static JavaAnnotation findAnnotation(@NotNull JavaAnnotationOwnerImpl owner, @NotNull FqName fqName) {
        PsiModifierList modifierList = owner.getPsi().getModifierList();
        if (modifierList != null) {
            PsiAnnotation psiAnnotation = modifierList.findAnnotation(fqName.asString());
            return psiAnnotation == null ? null : new JavaAnnotationImpl(psiAnnotation);
        }
        return null;
    }
}
