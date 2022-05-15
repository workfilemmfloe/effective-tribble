/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter.dsl

import com.intellij.ide.highlighter.custom.CustomHighlighterColors.*
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.highlighter.HighlighterExtension
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import kotlin.math.absoluteValue

class DslHighlighterExtension : HighlighterExtension() {
    override fun highlightDeclaration(elementToHighlight: PsiElement, descriptor: DeclarationDescriptor): TextAttributesKey? {
        return null
    }

    override fun highlightCall(elementToHighlight: PsiElement, resolvedCall: ResolvedCall<*>): TextAttributesKey? {
        val markerAnnotation = resolvedCall.resultingDescriptor.annotations.find { annotation ->
            annotation.annotationClass?.isDslHighlightingMarker() ?: false
        }?.annotationClass ?: return null

        val styleId = styleIdByMarkerAnnotation(markerAnnotation) ?: return null
        return styles[styleId - 1]
    }

    companion object {
        private const val numStyles = 4

        private val defaultKeys = listOf(
            CUSTOM_KEYWORD1_ATTRIBUTES,
            CUSTOM_KEYWORD2_ATTRIBUTES,
            CUSTOM_KEYWORD3_ATTRIBUTES,
            CUSTOM_KEYWORD4_ATTRIBUTES
        )

        private val styles = (1..numStyles).map { index ->
            TextAttributesKey.createTextAttributesKey("KOTLIN_DSL_STYLE$index", defaultKeys[index - 1])
        }

        val descriptionsToStyles = (1..numStyles).associate { index ->
            "Dsl//${styleOptionDisplayName(index)}" to styles[index - 1]
        }

        fun styleOptionDisplayName(index: Int) = "Style$index"

        fun styleIdByMarkerAnnotation(markerAnnotation: ClassDescriptor): Int? {
            val markerAnnotationFqName = markerAnnotation.fqNameSafe
            return (markerAnnotationFqName.asString().hashCode() % numStyles).absoluteValue + 1
        }
    }
}

internal fun ClassDescriptor.isDslHighlightingMarker(): Boolean {
    return annotations.any {
        it.annotationClass?.fqNameSafe?.asString() == "kotlin.DslMarker"
    }
}
