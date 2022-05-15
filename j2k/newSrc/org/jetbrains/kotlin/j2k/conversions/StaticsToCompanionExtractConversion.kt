/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*

class StaticsToCompanionExtractConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKClass) return recurse(element)
        if (element.classKind == JKClass.ClassKind.COMPANION || element.classKind == JKClass.ClassKind.OBJECT) return element
        val statics = element.declarationList.filter { declaration ->
            declaration is JKModifierListOwner &&
                    declaration.modifierList.modifiers.any { it is JKJavaModifier && it.type == JKJavaModifier.JavaModifierType.STATIC }
        }

        if (statics.isEmpty()) return recurse(element)
        val companion = findOrCreateCompanion(element)


        element.declarationList -= statics
        companion.declarationList += statics.onEach { declaration ->
            (declaration as JKModifierListOwner)
            declaration.modifierList.modifiers =
                    declaration.modifierList.modifiers.filterNot { it is JKJavaModifier && it.type == JKJavaModifier.JavaModifierType.STATIC }
        }

        return recurse(element)
    }

    fun findOrCreateCompanion(element: JKClass): JKClass {
        val companion = element.declarationList
            .asSequence()
            .filterIsInstance<JKClass>()
            .firstOrNull { it.classKind == JKClass.ClassKind.COMPANION }

        if (companion != null) return companion

        return JKClassImpl(
            JKModifierListImpl(
                listOf(
                    JKAccessModifierImpl(JKAccessModifier.Visibility.PUBLIC),
                    JKModalityModifierImpl(JKModalityModifier.Modality.FINAL)
                )
            ),
            JKNameIdentifierImpl(""),
            JKInheritanceInfoImpl(emptyList()),
            JKClass.ClassKind.COMPANION
        ).also { element.declarationList += it }
    }
}
