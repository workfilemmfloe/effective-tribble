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

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.uast.*
import org.jetbrains.uast.java.AbstractJavaUClass
import org.jetbrains.uast.kotlin.declarations.KotlinUMethod

class KotlinUClass private constructor(
        psi: KtLightClass,
        override val containingElement: UElement?
) : AbstractJavaUClass(), PsiClass by psi {
    val ktClass = psi.kotlinOrigin
    override val psi = unwrap<UClass, PsiClass>(psi)

    override fun getOriginalElement(): PsiElement? {
        return super.getOriginalElement()
    }

    override val uastAnchor: UElement
        get() = UIdentifier(psi.nameIdentifier, this)
    
    override val uastMethods: List<UMethod> by lz {
        val primaryConstructor = ktClass?.getPrimaryConstructor()?.toLightMethods()?.firstOrNull()
        val initBlocks = ktClass?.getAnonymousInitializers() ?: emptyList()

        psi.methods.map {
            if (it is KtLightMethod && it.isConstructor && initBlocks.isNotEmpty()
                && (primaryConstructor == null || it == primaryConstructor)) {
                object : KotlinUMethod(it, this@KotlinUClass) {
                    override val uastBody by lz {
                        val initializers = ktClass?.getAnonymousInitializers() ?: return@lz UastEmptyExpression
                        val containingMethod = this

                        object : UBlockExpression {
                            override val containingElement: UElement?
                                get() = containingMethod

                            override val annotations: List<UAnnotation>
                                get() = emptyList()

                            override val expressions by lz {
                                initializers.map {
                                    getLanguagePlugin().convertOpt<UExpression>(it.body, this) ?: UastEmptyExpression
                                }
                            }
                        }
                    }
                }
            }
            else {
                getLanguagePlugin().convert<UMethod>(it, this)
            }
        } 
    }

    companion object {
        fun create(psi: KtLightClass, containingElement: UElement?): UClass {
            return if (psi is PsiAnonymousClass)
                KotlinUAnonymousClass(psi, containingElement)
            else
                KotlinUClass(psi, containingElement)
        }
    }
}

class KotlinUAnonymousClass(
        psi: PsiAnonymousClass,
        override val containingElement: UElement?
) : AbstractJavaUClass(), UAnonymousClass, PsiAnonymousClass by psi {
    override val psi: PsiAnonymousClass = unwrap<UAnonymousClass, PsiAnonymousClass>(psi)

    override fun getOriginalElement(): PsiElement? {
        return super<AbstractJavaUClass>.getOriginalElement()
    }

    override val uastAnchor: UElement?
        get() {
            val ktClassOrObject = (psi.originalElement as? KtLightClass)?.kotlinOrigin as? KtObjectDeclaration ?: return null 
            return UIdentifier(ktClassOrObject.getObjectKeyword(), this)
        }
}