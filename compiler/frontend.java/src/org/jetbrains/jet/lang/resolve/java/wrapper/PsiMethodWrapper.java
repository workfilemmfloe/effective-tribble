/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.kt.JetConstructorAnnotation;
import org.jetbrains.jet.lang.resolve.java.kt.JetMethodAnnotation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class PsiMethodWrapper extends PsiMemberWrapper {

    public PsiMethodWrapper(@NotNull PsiMethod psiMethod) {
        super(psiMethod);
    }
    
    private List<PsiParameterWrapper> parameters;
    @NotNull
    public List<PsiParameterWrapper> getParameters() {
        if (parameters == null) {
            PsiParameter[] psiParameters = getPsiMethod().getParameterList().getParameters();
            parameters = new ArrayList<PsiParameterWrapper>(psiParameters.length);
            for (PsiParameter psiParameter : psiParameters) {
                parameters.add(new PsiParameterWrapper(psiParameter));
            }
        }
        return parameters;
    }

    @NotNull
    public PsiParameterWrapper getParameter(int i) {
        return getParameters().get(i);
    }

    private JetMethodAnnotation jetMethodAnnotation;
    @NotNull
    public JetMethodAnnotation getJetMethodAnnotation() {
        if (jetMethodAnnotation == null) {
            jetMethodAnnotation = JetMethodAnnotation.get(getPsiMethod());
        }
        return jetMethodAnnotation;
    }

    private JetConstructorAnnotation jetConstructorAnnotation;
    @NotNull
    public JetConstructorAnnotation getJetConstructorAnnotation() {
        if (jetConstructorAnnotation == null) {
            jetConstructorAnnotation = JetConstructorAnnotation.get(getPsiMethod());
        }
        return jetConstructorAnnotation;
    }

    @Override
    public boolean isAbstract() {
        return psiMember.hasModifierProperty(PsiModifier.ABSTRACT);
    }

    @NotNull
    public PsiMethod getPsiMethod() {
        return (PsiMethod) psiMember;
    }

    @Nullable
    public PsiType getReturnType() {
        return getPsiMethod().getReturnType();
    }
}
