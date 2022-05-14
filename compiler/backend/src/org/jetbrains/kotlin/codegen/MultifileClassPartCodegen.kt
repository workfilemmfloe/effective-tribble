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

package org.jetbrains.kotlin.codegen

import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext
import org.jetbrains.kotlin.codegen.serialization.JvmSerializerExtension
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import java.util.*

public class MultifileClassPartCodegen(
        v: ClassBuilder,
        file: KtFile,
        private val filePartType: Type,
        private val multifileClassType: Type,
        partContext: FieldOwnerContext<*>,
        state: GenerationState
) : MemberCodegen<KtFile>(state, null, partContext, file, v) {
    override fun generate() {
        if (state.classBuilderMode == ClassBuilderMode.LIGHT_CLASSES) return
        super.generate()
    }

    override fun generateDeclaration() {
        v.defineClass(element, Opcodes.V1_6,
                      Opcodes.ACC_FINAL or Opcodes.ACC_SYNTHETIC or Opcodes.ACC_SUPER,
                      filePartType.internalName,
                      null,
                      "java/lang/Object",
                      ArrayUtil.EMPTY_STRING_ARRAY)
        v.visitSource(element.name, null)

        generatePropertyMetadataArrayFieldIfNeeded(filePartType)
    }

    override fun generateBody() {
        for (declaration in element.declarations) {
            if (declaration is KtNamedFunction || declaration is KtProperty) {
                genFunctionOrProperty(declaration)
            }
        }

        if (state.classBuilderMode == ClassBuilderMode.FULL) {
            generateInitializers { createOrGetClInitCodegen() }
        }
    }

    override fun generateKotlinAnnotation() {
        val members = ArrayList<DeclarationDescriptor>()
        for (declaration in element.declarations) {
            when (declaration) {
                is KtNamedFunction -> {
                    val functionDescriptor = bindingContext.get(BindingContext.FUNCTION, declaration)
                    members.add(functionDescriptor ?: throw AssertionError("Function ${declaration.name} is not bound in ${element.name}"))
                }
                is KtProperty -> {
                    val property = bindingContext.get(BindingContext.VARIABLE, declaration)
                    members.add(property ?: throw AssertionError("Property ${declaration.name} is not bound in ${element.name}"))
                }
            }
        }

        val bindings = v.serializationBindings

        val serializer = DescriptorSerializer.createTopLevel(
                JvmSerializerExtension(bindings, state.typeMapper, state.useTypeTableInSerializer)
        )
        val packageProto = serializer.packagePartProto(members).build()

        val av = v.newAnnotation(AsmUtil.asmDescByFqNameWithoutInnerClasses(JvmAnnotationNames.KOTLIN_MULTIFILE_CLASS_PART), true)
        AsmUtil.writeAnnotationData(av, serializer, packageProto)
        av.visit(JvmAnnotationNames.MULTIFILE_CLASS_NAME_FIELD_NAME, multifileClassType.internalName)
        av.visitEnd()
    }

    override fun generateSyntheticParts() {
        generateSyntheticAccessors()
    }
}
