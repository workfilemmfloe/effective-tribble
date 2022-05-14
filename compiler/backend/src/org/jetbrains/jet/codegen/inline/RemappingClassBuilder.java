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

package org.jetbrains.jet.codegen.inline;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.AnnotationVisitor;
import org.jetbrains.asm4.ClassVisitor;
import org.jetbrains.asm4.FieldVisitor;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.commons.*;
import org.jetbrains.jet.codegen.ClassBuilder;
import org.jetbrains.jet.codegen.JvmSerializationBindings;

public class RemappingClassBuilder implements ClassBuilder {

    private final ClassBuilder builder;
    private final Remapper remapper;

    public RemappingClassBuilder(@NotNull ClassBuilder builder, @NotNull Remapper remapper) {
        this.builder = builder;
        this.remapper = remapper;
    }

    @Override
    @NotNull
    public FieldVisitor newField(
            @Nullable PsiElement origin,
            int access,
            @NotNull String name,
            @NotNull String desc,
            @Nullable String signature,
            @Nullable Object value
    ) {
        return new RemappingFieldAdapter(builder.newField(origin, access, name, remapper.mapDesc(desc), signature, value), remapper);
    }

    @Override
    @NotNull
    public MethodVisitor newMethod(
            @Nullable PsiElement origin,
            int access,
            @NotNull String name,
            @NotNull String desc,
            @Nullable String signature,
            @Nullable String[] exceptions
    ) {
        return new RemappingMethodAdapter(access, desc, builder.newMethod(origin, access, name, remapper.mapMethodDesc(desc), signature, exceptions), remapper);
    }

    @Override
    @NotNull
    public JvmSerializationBindings getSerializationBindings() {
        return builder.getSerializationBindings();
    }

    @Override
    @NotNull
    public AnnotationVisitor newAnnotation(@NotNull String desc, boolean visible) {
        return new RemappingAnnotationAdapter(builder.newAnnotation(remapper.mapDesc(desc), visible), remapper);
    }

    @Override
    public void done() {
        builder.done();
    }

    @Override
    @NotNull
    public ClassVisitor getVisitor() {
        return new RemappingClassAdapter(builder.getVisitor(), remapper);
    }

    @Override
    public void defineClass(
            @Nullable PsiElement origin,
            int version,
            int access,
            @NotNull String name,
            @Nullable String signature,
            @NotNull String superName,
            @NotNull String[] interfaces
    ) {
        builder.defineClass(origin, version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(@NotNull String name, @Nullable String debug) {
        builder.visitSource(name, debug);
    }

    @Override
    public void visitOuterClass(@NotNull String owner, @Nullable String name, @Nullable String desc) {
        builder.visitOuterClass(owner, name, desc);
    }

    @Override
    public void visitInnerClass(@NotNull String name, @Nullable String outerName, @Nullable String innerName, int access) {
        builder.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    @NotNull
    public String getThisName() {
        return builder.getThisName();
    }
}
