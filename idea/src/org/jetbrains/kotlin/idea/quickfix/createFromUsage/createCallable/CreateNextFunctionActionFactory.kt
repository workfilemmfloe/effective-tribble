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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.FunctionInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetForExpression
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions

object CreateNextFunctionActionFactory : CreateCallableMemberFromUsageFactory<JetForExpression>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): JetForExpression? {
        return QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetForExpression>())
    }

    override fun createCallableInfo(element: JetForExpression, diagnostic: Diagnostic): CallableInfo? {
        val diagnosticWithParameters = DiagnosticFactory.cast(diagnostic, Errors.NEXT_MISSING, Errors.NEXT_NONE_APPLICABLE)
        val ownerType = TypeInfo(diagnosticWithParameters.a, Variance.IN_VARIANCE)

        val variableExpr = element.loopParameter ?: element.multiParameter ?: return null
        val returnType = TypeInfo(variableExpr as JetExpression, Variance.OUT_VARIANCE)
        return FunctionInfo(OperatorNameConventions.NEXT.asString(), ownerType, returnType, isOperator = true)
    }
}
