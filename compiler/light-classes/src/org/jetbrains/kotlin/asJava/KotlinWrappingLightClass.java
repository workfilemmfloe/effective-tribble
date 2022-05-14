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

package org.jetbrains.kotlin.asJava;

import com.intellij.lang.Language;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.impl.light.AbstractLightClass;
import com.intellij.psi.impl.source.ClassInnerStuffCache;
import com.intellij.psi.impl.source.PsiExtensibleClass;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import kotlin.KotlinPackage;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.JetLanguage;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilPackage;

import java.util.List;

public abstract class KotlinWrappingLightClass extends AbstractLightClass implements KotlinLightClass, PsiExtensibleClass {
    private final ClassInnerStuffCache myInnersCache = new ClassInnerStuffCache(this);

    protected KotlinWrappingLightClass(PsiManager manager) {
        super(manager, JetLanguage.INSTANCE);
    }

    @Nullable
    @Override
    public abstract JetClassOrObject getOrigin();

    @NotNull
    @Override
    public abstract PsiClass getDelegate();

    @Override
    @NotNull
    public PsiField[] getFields() {
        return myInnersCache.getFields();
    }

    @Override
    @NotNull
    public PsiMethod[] getMethods() {
        return myInnersCache.getMethods();
    }

    @Override
    @NotNull
    public PsiMethod[] getConstructors() {
        return myInnersCache.getConstructors();
    }

    @Override
    @NotNull
    public PsiClass[] getInnerClasses() {
        return myInnersCache.getInnerClasses();
    }

    @Override
    @NotNull
    public PsiField[] getAllFields() {
        return PsiClassImplUtil.getAllFields(this);
    }

    @Override
    @NotNull
    public PsiMethod[] getAllMethods() {
        return PsiClassImplUtil.getAllMethods(this);
    }

    @Override
    @NotNull
    public PsiClass[] getAllInnerClasses() {
        return PsiClassImplUtil.getAllInnerClasses(this);
    }

    @Override
    public PsiField findFieldByName(String name, boolean checkBases) {
        return myInnersCache.findFieldByName(name, checkBases);
    }

    @Override
    @NotNull
    public PsiMethod[] findMethodsByName(String name, boolean checkBases) {
        return myInnersCache.findMethodsByName(name, checkBases);
    }

    @Override
    public PsiClass findInnerClassByName(String name, boolean checkBases) {
        return myInnersCache.findInnerClassByName(name, checkBases);
    }

    /**
     * @see org.jetbrains.kotlin.codegen.binding.CodegenBinding#ENUM_ENTRY_CLASS_NEED_SUBCLASS
      */
    @NotNull
    @Override
    public List<PsiField> getOwnFields() {
        return ContainerUtil.map(getDelegate().getFields(), new Function<PsiField, PsiField>() {
            @Override
            public PsiField fun(PsiField field) {
                JetDeclaration declaration = ClsWrapperStubPsiFactory.getOriginalDeclaration(field);
                if (declaration instanceof JetEnumEntry) {
                    assert field instanceof PsiEnumConstant : "Field delegate should be an enum constant (" + field.getName() + "):\n" +
                                                              PsiUtilPackage.getElementTextWithContext(declaration);
                    JetEnumEntry enumEntry = (JetEnumEntry) declaration;
                    PsiEnumConstant enumConstant = (PsiEnumConstant) field;
                    FqName enumConstantFqName = new FqName(getFqName().asString() + "." + enumEntry.getName());
                    KotlinLightClassForEnumEntry initializingClass =
                            enumEntry.getDeclarations().isEmpty()
                            ? null
                            : new KotlinLightClassForEnumEntry(myManager, enumConstantFqName, enumEntry, enumConstant);
                    return new KotlinLightEnumConstant(myManager, enumEntry, enumConstant, KotlinWrappingLightClass.this, initializingClass);
                }
                if (declaration != null) {
                    return new KotlinLightFieldForDeclaration(myManager, declaration, field, KotlinWrappingLightClass.this);
                }
                return new KotlinNoOriginLightField(myManager, field, KotlinWrappingLightClass.this);
            }
        });
    }

    @NotNull
    @Override
    public List<PsiMethod> getOwnMethods() {
        return KotlinPackage.map(getDelegate().getMethods(), new Function1<PsiMethod, PsiMethod>() {
            @Override
            public PsiMethod invoke(PsiMethod method) {
                JetDeclaration declaration = ClsWrapperStubPsiFactory.getOriginalDeclaration(method);
                if (declaration instanceof JetPropertyAccessor) {
                    declaration = PsiTreeUtil.getParentOfType(declaration, JetProperty.class);
                }

                if (declaration != null) {
                    return !isTraitFakeOverride(declaration) ?
                           new KotlinLightMethodForDeclaration(myManager, method, declaration, KotlinWrappingLightClass.this) :
                           new KotlinLightMethodForTraitFakeOverride(myManager, method, declaration, KotlinWrappingLightClass.this);
                }

                return new KotlinNoOriginLightMethod(myManager, method, KotlinWrappingLightClass.this);
            }
        });
    }

    @Override
    public boolean processDeclarations(
            @NotNull PsiScopeProcessor processor, @NotNull ResolveState state, PsiElement lastParent, @NotNull PsiElement place
    ) {
        if (isEnum()) {
            if (!PsiClassImplUtil.processDeclarationsInEnum(processor, state, myInnersCache)) return false;
        }

        return super.processDeclarations(processor, state, lastParent, place);
    }

    @Override
    public String getText() {
        JetClassOrObject origin = getOrigin();
        return origin == null ? null : origin.getText();
    }

    @NotNull
    @Override
    public Language getLanguage() {
        return JetLanguage.INSTANCE;
    }

    private boolean isTraitFakeOverride(@NotNull JetDeclaration originMethodDeclaration) {
        if (!(originMethodDeclaration instanceof JetNamedFunction ||
              originMethodDeclaration instanceof JetPropertyAccessor ||
              originMethodDeclaration instanceof JetProperty)) {
            return false;
        }

        JetClassOrObject parentOfMethodOrigin = PsiTreeUtil.getParentOfType(originMethodDeclaration, JetClassOrObject.class);
        JetClassOrObject thisClassDeclaration = getOrigin();

        // Method was generated from declaration in some other trait
        return (parentOfMethodOrigin != null && thisClassDeclaration != parentOfMethodOrigin && JetPsiUtil.isTrait(parentOfMethodOrigin));
    }

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();
}
