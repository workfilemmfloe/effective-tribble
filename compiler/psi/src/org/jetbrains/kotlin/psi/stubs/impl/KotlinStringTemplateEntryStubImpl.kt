/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.jetbrains.kotlin.psi.stubs.KotlinStringTemplateEntryStub
import org.jetbrains.kotlin.psi.stubs.StringEntryKind
import org.jetbrains.kotlin.psi.stubs.elements.KtStringTemplateEntryElementType

class KotlinStringTemplateEntryStubImpl<T : KtStringTemplateEntry<T>>(
    parent: StubElement<out PsiElement>?,
    elementType: KtStringTemplateEntryElementType<T>,
    private val kind: StringEntryKind,
    private val value: StringRef?
) : KotlinStubBaseImpl<T>(parent, elementType), KotlinStringTemplateEntryStub<T> {
    override fun kind(): StringEntryKind = kind
    override fun value(): String? = StringRef.toString(value)
}