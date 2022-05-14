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

package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.stubs.PsiJetPlaceHolderStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collections;
import java.util.List;

public class JetNullableType extends JetElementImplStub<PsiJetPlaceHolderStub<JetNullableType>> implements JetTypeElement {
    public JetNullableType(@NotNull ASTNode node) {
        super(node);
    }

    public JetNullableType(@NotNull PsiJetPlaceHolderStub<JetNullableType> stub) {
        super(stub, JetStubElementTypes.NULLABLE_TYPE);
    }

    @NotNull
    public ASTNode getQuestionMarkNode() {
        return getNode().findChildByType(JetTokens.QUEST);
    }

    @NotNull
    @Override
    public List<JetTypeReference> getTypeArgumentsAsTypes() {
        JetTypeElement innerType = getInnerType();
        return innerType == null ? Collections.<JetTypeReference>emptyList() : innerType.getTypeArgumentsAsTypes();
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitNullableType(this, data);
    }

    @Nullable
    @IfNotParsed
    public JetTypeElement getInnerType() {
        return JetStubbedPsiUtil.getStubOrPsiChild(this, JetStubElementTypes.TYPE_ELEMENT_TYPES, JetTypeElement.ARRAY_FACTORY);
    }
}
