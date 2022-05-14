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

package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.actions.MethodSmartStepTarget
import com.intellij.debugger.actions.SmartStepTarget
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.util.Range
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunctionLiteralExpression
import org.jetbrains.kotlin.util.OperatorNameConventions
import javax.swing.Icon

public class KotlinLambdaSmartStepTarget(
        label: String,
        highlightElement: KtFunctionLiteralExpression,
        lines: Range<Int>
): SmartStepTarget(label, highlightElement, true, lines) {
    override fun getIcon() = KotlinIcons.LAMBDA

    fun getLambda() = getHighlightElement() as KtFunctionLiteralExpression

    companion object {
        fun calcLabel(descriptor: DeclarationDescriptor, paramName: Name): String {
            return "${descriptor.getName().asString()}: ${paramName.asString()}.${OperatorNameConventions.INVOKE.asString()}()"
        }
    }
}