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

package org.jetbrains.kotlin.load.java.structure.impl;

import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation;
import org.jetbrains.kotlin.load.java.structure.JavaType;
import org.jetbrains.kotlin.load.java.structure.JavaValueParameter;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;

import java.util.Collection;

public class JavaValueParameterImpl extends JavaElementImpl<PsiParameter> implements JavaValueParameter, JavaAnnotationOwnerImpl {
    public JavaValueParameterImpl(@NotNull PsiParameter psiParameter) {
        super(psiParameter);
    }

    @NotNull
    @Override
    public Collection<JavaAnnotation> getAnnotations() {
        return JavaElementUtil.getAnnotations(this);
    }

    @Nullable
    @Override
    public JavaAnnotation findAnnotation(@NotNull FqName fqName) {
        return JavaElementUtil.findAnnotation(this, fqName);
    }

    @Override
    @Nullable
    public Name getName() {
        String name = getPsi().getName();
        return name == null ? null : Name.identifier(name);
    }

    @Override
    @NotNull
    public JavaType getType() {
        return JavaTypeImpl.create(getPsi().getType());
    }

    @Override
    public boolean isVararg() {
        return getPsi().isVarArgs();
    }
}
