/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.jetbrains.kotlin.psi.stubs.KotlinStringTemplateEntryStub
import org.jetbrains.kotlin.psi.stubs.StringEntryKind
import org.jetbrains.kotlin.psi.stubs.impl.KotlinStringTemplateEntryStubImpl

class KtStringTemplateEntryElementType<PsiT : KtStringTemplateEntry<PsiT>>(
    @NonNls debugName: String,
    @NotNull psiClass: Class<PsiT>
) :
    KtStubElementType<KotlinStringTemplateEntryStub<PsiT>, PsiT>(
        debugName,
        psiClass,
        KotlinStringTemplateEntryStub::class.java
    ) {

    override fun createStub(psi: PsiT, parentStub: StubElement<*>?): KotlinStringTemplateEntryStub<PsiT> {
        @Suppress("UNCHECKED_CAST")
        val elementType = psi.node.elementType as? KtStringTemplateEntryElementType<PsiT>
            ?: throw IllegalStateException("Stub element type is expected for string entry")

        val value = psi.text

        return KotlinStringTemplateEntryStubImpl(
            parentStub,
            elementType,
            stringEntryElementTypeToKind(elementType),
            StringRef.fromString(value)
        )
    }

    override fun serialize(stub: KotlinStringTemplateEntryStub<PsiT>, dataStream: StubOutputStream) {
        dataStream.writeInt(stub.kind().ordinal)
        dataStream.writeName(stub.value())
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinStringTemplateEntryStub<PsiT> {
        val kindOrdinal = dataStream.readInt()
        val value = dataStream.readName()

        val valueKind = StringEntryKind.values()[kindOrdinal]

        return KotlinStringTemplateEntryStubImpl(
            parentStub,
            kindToStringEntryElementType(valueKind),
            valueKind,
            value
        )
    }

    companion object {
        fun kindToStringEntryElementType(kind: StringEntryKind): KtStringTemplateEntryElementType<*> {
            return when (kind) {
                StringEntryKind.LONG_STRING_TEMPLATE_ENTRY -> KtStubElementTypes.LONG_STRING_TEMPLATE_ENTRY
                StringEntryKind.SHORT_STRING_TEMPLATE_ENTRY -> KtStubElementTypes.SHORT_STRING_TEMPLATE_ENTRY
                StringEntryKind.LITERAL_STRING_TEMPLATE_ENTRY -> KtStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY
                StringEntryKind.ESCAPE_STRING_TEMPLATE_ENTRY -> KtStubElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY
            }
        }

        private fun stringEntryElementTypeToKind(elementType: KtStringTemplateEntryElementType<*>): StringEntryKind {
            return when (elementType) {
                KtStubElementTypes.LONG_STRING_TEMPLATE_ENTRY -> StringEntryKind.LONG_STRING_TEMPLATE_ENTRY
                KtStubElementTypes.SHORT_STRING_TEMPLATE_ENTRY -> StringEntryKind.SHORT_STRING_TEMPLATE_ENTRY
                KtStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY -> StringEntryKind.LITERAL_STRING_TEMPLATE_ENTRY
                KtStubElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY -> StringEntryKind.ESCAPE_STRING_TEMPLATE_ENTRY
                else -> throw IllegalStateException("Unknown string element node type: $elementType")
            }
        }
    }

}