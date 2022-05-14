/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.uast

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.uast.kinds.KotlinUastVisibilities
import org.jetbrains.uast.*

private val JVM_STATIC_FQNAME = "kotlin.jvm.JvmStatic"
private val JVM_FIELD_FQNAME = "kotlin.jvm.JvmField"

private val MODIFIER_MAP = mapOf(
        UastModifier.ABSTRACT to KtTokens.ABSTRACT_KEYWORD,
        UastModifier.OVERRIDE to KtTokens.OVERRIDE_KEYWORD
)

internal fun KtDeclaration.getVisibility() = when (visibilityModifierType()) {
    KtTokens.PRIVATE_KEYWORD -> UastVisibility.PRIVATE
    KtTokens.PROTECTED_KEYWORD -> UastVisibility.PROTECTED
    KtTokens.INTERNAL_KEYWORD -> KotlinUastVisibilities.INTERNAL
    else -> UastVisibility.PUBLIC
}

internal fun KtModifierListOwner.hasModifier(modifier: UastModifier): Boolean {
    if (modifier == UastModifier.STATIC) {
        // Object literals can't be static
        if (this is KtObjectDeclaration && this.isObjectLiteral()) {
            return false
        }
        if (this is KtClassOrObject && !hasModifier(KtTokens.INNER_KEYWORD)) {
            return true
        }
        if (this is KtDeclaration && (parent is KtObjectDeclaration ||
                                      parent is KtClassBody && parent?.parent is KtObjectDeclaration)) {
            return hasAnyAnnotation(JVM_STATIC_FQNAME, JVM_FIELD_FQNAME)
        }
        return false
    }

    if (modifier == UastModifier.JVM_FIELD) {
        var bindingContext: BindingContext? = null
        fun getOrCreateBindingContext(): BindingContext {
            if (bindingContext == null) {
                bindingContext = analyze(BodyResolveMode.PARTIAL)
            }
            return bindingContext!!
        }

        if (this is KtProperty) {
            val context = getOrCreateBindingContext()
            val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? PropertyDescriptor ?: return false
            return context[BindingContext.BACKING_FIELD_REQUIRED, descriptor] ?: false
        }
    }
    if (modifier == UastModifier.FINAL) return hasModifier(KtTokens.FINAL_KEYWORD)
    if (modifier == UastModifier.IMMUTABLE && this is KtVariableDeclaration && !this.isVar) return true

    val javaModifier = MODIFIER_MAP[modifier] ?: return false
    return hasModifier(javaModifier)
}

private fun KtElement.hasAnyAnnotation(vararg annotationFqNames: String): Boolean {
    if (this !is KtAnnotated) return false

    val bindingContext = analyze(BodyResolveMode.PARTIAL)
    for (annotationEntry in annotationEntries) {
        val annotationDescriptor = bindingContext[BindingContext.ANNOTATION, annotationEntry] ?: continue
        val classifierDescriptor = annotationDescriptor.type.constructor.declarationDescriptor ?: continue
        val fqName = DescriptorUtils.getFqName(classifierDescriptor).asString()

        for (annotationFqName in annotationFqNames) {
            if (fqName == annotationFqName) return true
        }
    }

    return false
}


internal fun KtElement?.resolveCallToUDeclaration(context: UastContext): UDeclaration? {
    if (this == null) return null
    val resolvedCall = this.getResolvedCall(analyze(BodyResolveMode.PARTIAL)) ?: return null
    val source = (resolvedCall.resultingDescriptor).toSource() ?: return null
    return context.convert(source) as? UDeclaration
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun String?.orAnonymous(kind: String = ""): String {
    return this ?: "<anonymous" + (if (kind.isNotBlank()) " $kind" else "") + ">"
}

internal fun KtAnnotated.getUastAnnotations(parent: UElement) = annotationEntries.map { KotlinUAnnotation(it, parent) }

internal fun DeclarationDescriptor.toSource() = try {
    DescriptorToSourceUtils.descriptorToDeclaration(this)
} catch (e: Exception) {
    null
}

internal fun <T> lz(initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)