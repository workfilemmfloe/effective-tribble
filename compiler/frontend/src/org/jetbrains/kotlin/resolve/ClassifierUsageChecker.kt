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

package org.jetbrains.kotlin.resolve

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

interface ClassifierUsageChecker {
    fun check(targetDescriptor: ClassifierDescriptor, trace: BindingTrace, element: PsiElement)

    companion object {
        fun check(declarations: Collection<PsiElement>, trace: BindingTrace, checkers: Iterable<ClassifierUsageChecker>) {
            val visitor = object : KtTreeVisitorVoid() {
                override fun visitReferenceExpression(expression: KtReferenceExpression) {
                    super.visitReferenceExpression(expression)

                    if (expression is KtNameReferenceExpression && expression.getReferencedNameElementType() == KtTokens.SUPER_KEYWORD) {
                        // Do not run checkers here because super expressions is not a proper expression. For example,
                        // it doesn't make sense to report deprecation on "super" in "super.foo()" if the super class is deprecated.
                        return
                    }

                    val target = trace.bindingContext.get(BindingContext.REFERENCE_TARGET, expression) as? ClassifierDescriptor ?: return

                    for (checker in checkers) {
                        checker.check(target, trace, expression)
                    }

                    if (isReferenceToCompanionViaOuterClass(expression, target)) {
                        val outerClass = target.containingDeclaration as ClassDescriptor
                        for (checker in checkers) {
                            checker.check(outerClass, trace, expression)
                        }
                    }
                }

                /**
                 * @return true iff [expression] references the companion of a class Foo via the name Foo, e.g. in `Foo.bar()`
                 */
                private fun isReferenceToCompanionViaOuterClass(expression: KtReferenceExpression, target: ClassifierDescriptor?): Boolean {
                    return DescriptorUtils.isCompanionObject(target) &&
                           trace.bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, expression] != null
                }
            }

            for (declaration in declarations) {
                declaration.accept(visitor)
            }
        }
    }
}
