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

package org.jetbrains.kotlin.idea.search.ideaExtensions

import com.intellij.codeInsight.TargetElementEvaluator
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.psiUtil.isAbstract
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.psi.JetClass

public class JetTargetElementEvaluator : TargetElementEvaluator {
    override fun includeSelfInGotoImplementation(element: PsiElement): Boolean = !(element is JetClass && element.isAbstract())
    override fun getElementByReference(ref: PsiReference, flags: Int): PsiElement? = null
}
