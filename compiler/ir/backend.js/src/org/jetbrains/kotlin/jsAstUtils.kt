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

import org.jetbrains.kotlin.js.backend.ast.*

fun _identityEquals(a: JsExpression, b: JsExpression) = JsBinaryOperation(JsBinaryOperator.REF_EQ, a, b)
fun _identityNotEquals(a: JsExpression, b: JsExpression) = JsBinaryOperation(JsBinaryOperator.REF_NEQ, a, b)

fun _not(a: JsExpression) = JsPrefixOperation(JsUnaryOperator.NOT, a)

fun _plus(a: JsExpression, b: JsExpression) = JsBinaryOperation(JsBinaryOperator.ADD, a, b)

fun _or(a: JsExpression, b: JsExpression) = JsBinaryOperation(JsBinaryOperator.OR, a, b)

fun _object(vararg entry: Pair<JsExpression, JsExpression>) = JsObjectLiteral(entry.map { JsPropertyInitializer(it.first, it.second) })

fun _var(name: JsName, initializer: JsExpression?) = JsVars(JsVars.JsVar(name, initializer))

fun _assignment(reg: JsNameRef, value: JsExpression) = JsExpressionStatement(JsBinaryOperation(JsBinaryOperator.ASG, reg, value))

fun _ref(name: String, qualifier: JsExpression? = null) = JsNameRef(name, qualifier)

fun _instanceOf(a: JsExpression, b: JsExpression) = JsBinaryOperation(JsBinaryOperator.INSTANCEOF, a, b)
fun throwCCE() = JsInvocation(_ref("kotlin.throwCCE"))//JsThrow(JsNew(JsNameRef("kotlin.CCE")))
fun throwNPE() = JsInvocation(_ref("kotlin.throwNPE"))//JsThrow(JsNew(JsNameRef("kotlin.CCE")))
