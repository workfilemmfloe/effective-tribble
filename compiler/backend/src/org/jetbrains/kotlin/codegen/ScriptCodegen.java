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

package org.jetbrains.kotlin.codegen;

import com.intellij.util.ArrayUtil;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.context.CodegenContext;
import org.jetbrains.kotlin.codegen.context.MethodContext;
import org.jetbrains.kotlin.codegen.context.ScriptContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.descriptors.ScriptDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.CLASS_FOR_SCRIPT;
import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.asmTypeForScriptDescriptor;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin.NO_ORIGIN;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

// SCRIPT: script code generator
public class ScriptCodegen extends MemberCodegen<KtScript> {

    public static ScriptCodegen createScriptCodegen(
            @NotNull KtScript declaration,
            @NotNull GenerationState state,
            @NotNull CodegenContext parentContext
    ) {
        BindingContext bindingContext = state.getBindingContext();
        ScriptDescriptor scriptDescriptor = bindingContext.get(BindingContext.SCRIPT, declaration);
        assert scriptDescriptor != null;

        ClassDescriptor classDescriptorForScript = bindingContext.get(CLASS_FOR_SCRIPT, scriptDescriptor);
        assert classDescriptorForScript != null;

        Type classType = asmTypeForScriptDescriptor(bindingContext, scriptDescriptor);

        ClassBuilder builder = state.getFactory().newVisitor(JvmDeclarationOriginKt.OtherOrigin(declaration, classDescriptorForScript),
                                                             classType, declaration.getContainingFile());
        List<ScriptDescriptor> earlierScripts = state.getEarlierScriptsForReplInterpreter();
        ScriptContext scriptContext = parentContext.intoScript(
                scriptDescriptor,
                earlierScripts == null ? Collections.<ScriptDescriptor>emptyList() : earlierScripts,
                classDescriptorForScript
        );
        return new ScriptCodegen(declaration, state, scriptContext, builder);
    }

    private final KtScript scriptDeclaration;
    private final ScriptContext context;
    private final ScriptDescriptor scriptDescriptor;

    private ScriptCodegen(
            @NotNull KtScript scriptDeclaration,
            @NotNull GenerationState state,
            @NotNull ScriptContext context,
            @NotNull ClassBuilder builder
    ) {
        super(state, null, context, scriptDeclaration, builder);
        this.scriptDeclaration = scriptDeclaration;
        this.context = context;
        this.scriptDescriptor = context.getScriptDescriptor();
    }

    @Override
    protected void generateDeclaration() {
        Type classType = typeMapper.mapClass(context.getContextDescriptor());

        v.defineClass(scriptDeclaration,
                      V1_6,
                      ACC_PUBLIC,
                      classType.getInternalName(),
                      null,
                      "java/lang/Object",
                      ArrayUtil.EMPTY_STRING_ARRAY);
    }

    @Override
    protected void generateBody() {
        genMembers();
        genFieldsForParameters(scriptDescriptor, v);
        genConstructor(scriptDescriptor, context.getContextDescriptor(), v,
                       context.intoFunction(scriptDescriptor.getScriptCodeDescriptor()));
    }

    @Override
    protected void generateKotlinAnnotation() {
        // TODO
    }

    private void genConstructor(
            @NotNull ScriptDescriptor scriptDescriptor,
            @NotNull ClassDescriptor classDescriptorForScript,
            @NotNull ClassBuilder classBuilder,
            @NotNull MethodContext methodContext
    ) {
        //noinspection ConstantConditions
        Type blockType = typeMapper.mapType(scriptDescriptor.getScriptCodeDescriptor().getReturnType());

        PropertyDescriptor scriptResultProperty = scriptDescriptor.getScriptResultProperty();
        classBuilder.newField(JvmDeclarationOriginKt.OtherOrigin(scriptResultProperty),
                              ACC_PUBLIC | ACC_FINAL, scriptResultProperty.getName().asString(),
                              blockType.getDescriptor(), null, null);

        JvmMethodSignature jvmSignature = typeMapper.mapScriptSignature(scriptDescriptor, context.getEarlierScripts());

        MethodVisitor mv = classBuilder.newMethod(
                JvmDeclarationOriginKt.OtherOrigin(scriptDeclaration, scriptDescriptor.getClassDescriptor().getUnsubstitutedPrimaryConstructor()),
                ACC_PUBLIC, jvmSignature.getAsmMethod().getName(), jvmSignature.getAsmMethod().getDescriptor(),
                null, null);

        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();

            InstructionAdapter iv = new InstructionAdapter(mv);

            Type classType = typeMapper.mapType(classDescriptorForScript);

            iv.load(0, classType);
            iv.invokespecial("java/lang/Object", "<init>", "()V", false);

            iv.load(0, classType);

            FrameMap frameMap = new FrameMap();
            frameMap.enterTemp(OBJECT_TYPE);

            for (ScriptDescriptor importedScript : context.getEarlierScripts()) {
                frameMap.enter(importedScript, OBJECT_TYPE);
            }

            Type[] argTypes = jvmSignature.getAsmMethod().getArgumentTypes();
            int add = 0;

            for (int i = 0; i < scriptDescriptor.getScriptCodeDescriptor().getValueParameters().size(); i++) {
                ValueParameterDescriptor parameter = scriptDescriptor.getScriptCodeDescriptor().getValueParameters().get(i);
                frameMap.enter(parameter, argTypes[i + add]);
            }

            int offset = 1;

            for (ScriptDescriptor earlierScript : context.getEarlierScripts()) {
                Type earlierClassType = asmTypeForScriptDescriptor(bindingContext, earlierScript);
                iv.load(0, classType);
                iv.load(offset, earlierClassType);
                offset += earlierClassType.getSize();
                iv.putfield(classType.getInternalName(), context.getScriptFieldName(earlierScript), earlierClassType.getDescriptor());
            }

            for (ValueParameterDescriptor parameter : scriptDescriptor.getScriptCodeDescriptor().getValueParameters()) {
                Type parameterType = typeMapper.mapType(parameter.getType());
                iv.load(0, classType);
                iv.load(offset, parameterType);
                offset += parameterType.getSize();
                iv.putfield(classType.getInternalName(), parameter.getName().getIdentifier(), parameterType.getDescriptor());
            }

            final ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, Type.VOID_TYPE, methodContext, state, this);

            generateInitializers(new Function0<ExpressionCodegen>() {
                @Override
                public ExpressionCodegen invoke() {
                    return codegen;
                }
            });

            StackValue stackValue = codegen.gen(scriptDeclaration.getBlockExpression());
            if (stackValue.type != Type.VOID_TYPE) {
                StackValue.Field resultValue = StackValue
                        .field(blockType, classType, ScriptDescriptor.LAST_EXPRESSION_VALUE_FIELD_NAME, false, StackValue.LOCAL_0);
                resultValue.store(stackValue, iv);
            }
            else {
                stackValue.put(blockType, iv);
            }

            iv.areturn(Type.VOID_TYPE);
        }

        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    private void genFieldsForParameters(@NotNull ScriptDescriptor script, @NotNull ClassBuilder classBuilder) {
        for (ScriptDescriptor earlierScript : context.getEarlierScripts()) {
            Type earlierClassName = asmTypeForScriptDescriptor(bindingContext, earlierScript);
            int access = ACC_PRIVATE | ACC_FINAL;
            classBuilder.newField(NO_ORIGIN, access, context.getScriptFieldName(earlierScript), earlierClassName.getDescriptor(), null, null);
        }

        for (ValueParameterDescriptor parameter : script.getScriptCodeDescriptor().getValueParameters()) {
            Type parameterType = typeMapper.mapType(parameter);
            int access = ACC_PUBLIC | ACC_FINAL;
            classBuilder.newField(JvmDeclarationOriginKt.OtherOrigin(parameter), access, parameter.getName().getIdentifier(), parameterType.getDescriptor(), null, null);
        }
    }

    private void genMembers() {
        for (KtDeclaration declaration : scriptDeclaration.getDeclarations()) {
            if (declaration instanceof KtProperty || declaration instanceof KtNamedFunction) {
                genFunctionOrProperty(declaration);
            }
            else if (declaration instanceof KtClassOrObject) {
                genClassOrObject((KtClassOrObject) declaration);
            }
        }
    }
}
