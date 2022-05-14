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

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetClass;
import org.jetbrains.kotlin.psi.JetEnumEntry;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.kotlin.psi.stubs.KotlinClassStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinClassStubImpl;
import org.jetbrains.kotlin.psi.stubs.impl.Utils;
import org.jetbrains.kotlin.resolve.lazy.ResolveSessionUtils;

import java.io.IOException;
import java.util.List;

public class JetClassElementType extends JetStubElementType<KotlinClassStub, JetClass> {
    public JetClassElementType(@NotNull @NonNls String debugName) {
        super(debugName, JetClass.class, KotlinClassStub.class);
    }

    @NotNull
    @Override
    public JetClass createPsi(@NotNull KotlinClassStub stub) {
        return !stub.isEnumEntry() ? new JetClass(stub) : new JetEnumEntry(stub);
    }

    @NotNull
    @Override
    public JetClass createPsiFromAst(@NotNull ASTNode node) {
        return node.getElementType() != JetStubElementTypes.ENUM_ENTRY ? new JetClass(node) : new JetEnumEntry(node);
    }

    @Override
    public KotlinClassStub createStub(@NotNull JetClass psi, StubElement parentStub) {
        FqName fqName = ResolveSessionUtils.safeFqNameForLazyResolve(psi);
        boolean isEnumEntry = psi instanceof JetEnumEntry;
        List<String> superNames = PsiUtilPackage.getSuperNames(psi);
        return new KotlinClassStubImpl(
                getStubType(isEnumEntry), parentStub, StringRef.fromString(fqName != null ? fqName.asString() : null),
                StringRef.fromString(psi.getName()), Utils.INSTANCE$.wrapStrings(superNames), psi.isInterface(), isEnumEntry,
                psi.isLocal(), psi.isTopLevel());
    }

    @Override
    public void serialize(@NotNull KotlinClassStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        FqName fqName = stub.getFqName();
        dataStream.writeName(fqName == null ? null : fqName.asString());
        dataStream.writeBoolean(stub.isInterface());
        dataStream.writeBoolean(stub.isEnumEntry());
        dataStream.writeBoolean(stub.isLocal());
        dataStream.writeBoolean(stub.isTopLevel());

        List<String> superNames = stub.getSuperNames();
        dataStream.writeVarInt(superNames.size());
        for (String name : superNames) {
            dataStream.writeName(name);
        }
    }

    @NotNull
    @Override
    public KotlinClassStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        StringRef qualifiedName = dataStream.readName();
        boolean isTrait = dataStream.readBoolean();
        boolean isEnumEntry = dataStream.readBoolean();
        boolean isLocal = dataStream.readBoolean();
        boolean isTopLevel = dataStream.readBoolean();

        int superCount = dataStream.readVarInt();
        StringRef[] superNames = StringRef.createArray(superCount);
        for (int i = 0; i < superCount; i++) {
            superNames[i] = dataStream.readName();
        }

        return new KotlinClassStubImpl(getStubType(isEnumEntry), parentStub, qualifiedName, name, superNames,
                                       isTrait, isEnumEntry, isLocal, isTopLevel);
    }

    @Override
    public void indexStub(@NotNull KotlinClassStub stub, @NotNull IndexSink sink) {
        StubIndexServiceFactory.getInstance().indexClass(stub, sink);
    }

    public static JetClassElementType getStubType(boolean isEnumEntry) {
        return isEnumEntry ? JetStubElementTypes.ENUM_ENTRY : JetStubElementTypes.CLASS;
    }
}
