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

package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.addRemoveModifier.AddRemoveModifierPackage;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lexer.JetModifierKeywordToken;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collections;
import java.util.List;

public class JetModifierListOwnerStub<T extends StubElement> extends JetElementImplStub<T> implements JetModifierListOwner {
    public JetModifierListOwnerStub(ASTNode node) {
        super(node);
    }

    public JetModifierListOwnerStub(T stub, IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    @Nullable
    public JetModifierList getModifierList() {
        return getStubOrPsiChild(JetStubElementTypes.MODIFIER_LIST);
    }

    @Override
    public boolean hasModifier(@NotNull JetModifierKeywordToken modifier) {
        JetModifierList modifierList = getModifierList();
        return modifierList != null && modifierList.hasModifier(modifier);
    }

    @Override
    public void addModifier(@NotNull JetModifierKeywordToken modifier) {
        AddRemoveModifierPackage.addModifier(this, modifier, JetTokens.INTERNAL_KEYWORD);
    }

    @Override
    public void removeModifier(@NotNull JetModifierKeywordToken modifier) {
        AddRemoveModifierPackage.removeModifier(this, modifier);
    }

    @Override
    @NotNull
    public List<JetAnnotationEntry> getAnnotationEntries() {
        JetModifierList modifierList = getModifierList();
        if (modifierList == null) return Collections.emptyList();
        return modifierList.getAnnotationEntries();
    }

    @Override
    @NotNull
    public List<JetAnnotation> getAnnotations() {
        JetModifierList modifierList = getModifierList();
        if (modifierList == null) return Collections.emptyList();
        return modifierList.getAnnotations();
    }
}
