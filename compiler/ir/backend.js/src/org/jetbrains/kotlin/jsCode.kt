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

package org.jetbrains.kotlin

import com.google.gwt.dev.js.ThrowExceptionOnErrorReporter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.parser.parse

fun translateJsCode(call: IrCall, scope: JsScope): JsNode {
    val statements = parseJsCode(call.getValueArgument(0)!!, JsFunctionScope(scope, "tmp"))
    val size = statements.size

    if (size == 0) {
        return JsEmpty
    }
    else if (size > 1) {
        return JsBlock(statements)
    }
    else {
        val resultStatement = statements[0]
        if (resultStatement is JsExpressionStatement) {
            return resultStatement.expression
        }

        return resultStatement
    }
}

private fun parseJsCode(jsCodeExpression: IrExpression, currentScope: JsScope): List<JsStatement> {
    //TODO check non simple compile time constants (expressions)

    if (jsCodeExpression !is IrConst<*>) error("")

    val kind = jsCodeExpression.kind
    if (kind !is IrConstKind.String) error("")

    val jsCode = kind.valueOf(jsCodeExpression)

    // Parser can change local or global scope.
    // In case of js we want to keep new local names,
    // but no new global ones.
    assert(currentScope is JsFunctionScope) { "Usage of js outside of function is unexpected" }
    val temporaryRootScope = JsRootScope(JsProgram())
    val scope = DelegatingJsFunctionScopeWithTemporaryParent(currentScope as JsFunctionScope, temporaryRootScope)
    return parse(jsCode, ThrowExceptionOnErrorReporter, scope, "<js code>")
}
