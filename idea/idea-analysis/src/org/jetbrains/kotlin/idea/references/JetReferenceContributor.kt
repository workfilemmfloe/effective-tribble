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

package org.jetbrains.kotlin.idea.references

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.kdoc.KDocReference
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*

public class JetReferenceContributor() : PsiReferenceContributor() {
    public override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        with(registrar) {
            registerProvider(javaClass<JetSimpleNameExpression>()) {
                JetSimpleNameReference(it)
            }

            registerMultiProvider(javaClass<JetNameReferenceExpression>()) {
                if (it.getReferencedNameElementType() != JetTokens.IDENTIFIER) return@registerMultiProvider emptyArray()

               ^when (it.readWriteAccess(useResolveForReadWrite = false)) {
                    ReferenceAccess.READ -> arrayOf(SyntheticPropertyAccessorReference.Getter(it))
                    ReferenceAccess.WRITE -> arrayOf(SyntheticPropertyAccessorReference.Setter(it))
                    ReferenceAccess.READ_WRITE -> arrayOf(SyntheticPropertyAccessorReference.Getter(it), SyntheticPropertyAccessorReference.Setter(it))
                }
            }

            registerProvider(javaClass<JetConstructorDelegationReferenceExpression>()) {
                JetConstructorDelegationReference(it)
            }

            registerProvider(javaClass<JetCallExpression>()) {
                JetInvokeFunctionReference(it)
            }

            registerProvider(javaClass<JetArrayAccessExpression>()) {
                JetArrayAccessReference(it)
            }

            registerProvider(javaClass<JetForExpression>()) {
                JetForLoopInReference(it)
            }

            registerProvider(javaClass<JetPropertyDelegate>()) {
                JetPropertyDelegationMethodsReference(it)
            }

            registerProvider(javaClass<JetMultiDeclaration>()) {
                JetMultiDeclarationReference(it)
            }

            registerProvider(javaClass<KDocName>()) {
                KDocReference(it)
            }
        }
    }

    private fun <E : JetElement> PsiReferenceRegistrar.registerProvider(elementClass: Class<E>, factory: (E) -> JetReference) {
        registerMultiProvider(elementClass, { arrayOf(factory(it)) })
    }

    private fun <E : JetElement> PsiReferenceRegistrar.registerMultiProvider(elementClass: Class<E>, factory: (E) -> Array<PsiReference>) {
        registerReferenceProvider(PlatformPatterns.psiElement(elementClass), object: PsiReferenceProvider() {
            override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                @Suppress("UNCHECKED_CAST")
                return factory(element as E)
            }
        })
    }
}
