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

package org.jetbrains.jet.asJava;

import com.intellij.psi.PsiAnnotationOwner;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.jet.plugin.JetLanguage;

import java.util.List;

public class KotlinLightParameter extends LightParameter implements KotlinLightElement<JetParameter, PsiParameter> {
    private static String getName(PsiParameter delegate, int index) {
        String name = delegate.getName();
        return name != null ? name : "p" + index;
    }

    private final PsiModifierList modifierList;
    private final PsiParameter delegate;
    private final int index;
    private final KotlinLightMethod method;

    public KotlinLightParameter(final PsiParameter delegate, int index, KotlinLightMethod method) {
        super(getName(delegate, index), delegate.getType(), method, JetLanguage.INSTANCE);

        this.delegate = delegate;
        this.index = index;
        this.method = method;

        this.modifierList = new KotlinLightModifierList(method.getManager(), ArrayUtil.EMPTY_STRING_ARRAY) {
            @Override
            public PsiAnnotationOwner getDelegate() {
                return delegate.getModifierList();
            }
        };
    }

    @NotNull
    @Override
    public PsiModifierList getModifierList() {
        return modifierList;
    }

    @NotNull
    @Override
    public PsiParameter getDelegate() {
        return delegate;
    }

    @Nullable
    @Override
    public JetParameter getOrigin() {
        JetDeclaration declaration = method.getOrigin();
        if (declaration == null) return null;

        int jetIndex = PsiUtilPackage.isExtensionDeclaration(declaration) ? index - 1 : index;
        if (jetIndex < 0) return null;

        if (declaration instanceof JetNamedFunction) {
            List<JetParameter> paramList = ((JetNamedFunction) declaration).getValueParameters();
            return jetIndex < paramList.size() ? paramList.get(jetIndex) : null;
        }

        if (declaration instanceof JetClass) {
            List<JetParameter> paramList = ((JetClass) declaration).getPrimaryConstructorParameters();
            return jetIndex < paramList.size() ? paramList.get(jetIndex) : null;
        }

        if (jetIndex != 0) return null;

        JetPropertyAccessor setter = null;
        if (declaration instanceof JetPropertyAccessor) {
            JetPropertyAccessor accessor = (JetPropertyAccessor) declaration;
            setter = accessor.isSetter() ? accessor : null;
        }
        else if (declaration instanceof JetProperty) {
            setter = ((JetProperty) declaration).getSetter();
        }

        return setter != null ? setter.getParameter() : null;
    }
}
