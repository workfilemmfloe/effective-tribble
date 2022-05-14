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

package org.jetbrains.kotlin.idea.core.codeInsight;

import com.intellij.codeInsight.generation.ClassMemberWithElement;
import com.intellij.codeInsight.generation.MemberChooserObject;
import com.intellij.codeInsight.generation.MemberChooserObjectBase;
import com.intellij.codeInsight.generation.PsiElementMemberChooserObject;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.idea.JetDescriptorIconProvider;
import org.jetbrains.kotlin.psi.JetClass;
import org.jetbrains.kotlin.psi.JetDeclaration;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetNamedDeclaration;
import org.jetbrains.kotlin.renderer.*;
import org.jetbrains.kotlin.resolve.DescriptorUtils;

import javax.swing.*;
import java.util.Collections;

public class DescriptorClassMember extends MemberChooserObjectBase implements ClassMemberWithElement {

    public static final String NO_PARENT_FOR = "No parent for ";
    @NotNull
    private final DeclarationDescriptor myDescriptor;
    @NotNull
    private final PsiElement myPsiElement;

    private static final DescriptorRenderer MEMBER_RENDERER = DescriptorRenderer.Companion.withOptions(
            new Function1<DescriptorRendererOptions, Unit>() {
                @Override
                public Unit invoke(DescriptorRendererOptions options) {
                    options.setWithDefinedIn(false);
                    options.setModifiers(Collections.<DescriptorRendererModifier>emptySet());
                    options.setStartFromName(true);
                    options.setNameShortness(NameShortness.SHORT);
                    return Unit.INSTANCE$;
                }
            }
    );

    public DescriptorClassMember(@NotNull PsiElement element, @NotNull DeclarationDescriptor descriptor) {
        super(getText(descriptor), getIcon(element, descriptor));
        myPsiElement = element;
        myDescriptor = descriptor;
    }

    private static String getText(DeclarationDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor) {
            return RendererPackage.render(DescriptorUtils.getFqNameSafe(descriptor));
        }
        else {
            return MEMBER_RENDERER.render(descriptor);
        }
    }

    private static Icon getIcon(PsiElement element, DeclarationDescriptor declarationDescriptor) {
        if (element.isValid()) {
            boolean isClass = element instanceof PsiClass || element instanceof JetClass;
            int flags = isClass ? 0 : Iconable.ICON_FLAG_VISIBILITY;
            if (element instanceof JetDeclaration) {  // kotlin declaration
                // visibility and abstraction better detect by a descriptor
                return JetDescriptorIconProvider.getIcon(declarationDescriptor, element, flags);
            }
            else {
                // it is better to show java icons for java code
                return element.getIcon(flags);
            }
        }

        return JetDescriptorIconProvider.getIcon(declarationDescriptor, element, 0);
    }

    @Override
    public MemberChooserObject getParentNodeDelegate() {
        DeclarationDescriptor parent = myDescriptor.getContainingDeclaration();
        PsiElement declaration;
        if (myPsiElement instanceof JetDeclaration) {
            // kotlin
            declaration = PsiTreeUtil.getStubOrPsiParentOfType(myPsiElement, JetNamedDeclaration.class);
            if (declaration == null) {
                declaration = PsiTreeUtil.getStubOrPsiParentOfType(myPsiElement, JetFile.class);
            }
        }
        else {
            // java or bytecode
            declaration = ((PsiMember) myPsiElement).getContainingClass();
        }
        assert parent != null : NO_PARENT_FOR + myDescriptor;
        assert declaration != null : NO_PARENT_FOR + myPsiElement;

        if (declaration instanceof JetFile) {
            JetFile file = (JetFile) declaration;
            return new PsiElementMemberChooserObject(file, file.getName());
        }

        return new DescriptorClassMember(declaration, parent);
    }

    public DeclarationDescriptor getDescriptor() {
        return myDescriptor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DescriptorClassMember that = (DescriptorClassMember) o;

        if (!myDescriptor.equals(that.myDescriptor)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return myDescriptor.hashCode();
    }

    @Override
    public PsiElement getElement() {
        return myPsiElement;
    }
}
