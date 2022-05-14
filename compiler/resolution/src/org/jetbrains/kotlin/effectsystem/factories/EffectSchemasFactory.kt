/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.effectsystem.factories

import org.jetbrains.kotlin.effectsystem.effects.ESReturns
import org.jetbrains.kotlin.effectsystem.impls.ESBooleanConstant
import org.jetbrains.kotlin.effectsystem.impls.ESConstant
import org.jetbrains.kotlin.effectsystem.impls.ESVariable
import org.jetbrains.kotlin.effectsystem.impls.EffectSchemaImpl
import org.jetbrains.kotlin.effectsystem.structure.*

object EffectSchemasFactory {
    /**
     * Creates more specific schemas for some particular cases (e.g. explicitly specifying
     * that schema for 'false' never returns 'true', etc.)
     */
    fun schemaForConstant(constant: ESConstant): EffectSchema {
        if (constant is ESBooleanConstant) {
            return clauses(
                    listOf(
                            ClausesFactory.create(true.lift(), ESReturns(constant)),
                            ClausesFactory.create(false.lift(), ESReturns(constant.negate()))
                    ),
                    listOf()
            )
        }

        if (constant == null.lift()) {
            return clauses(
                    listOf(
                            ClausesFactory.create(true.lift(), ESReturns(constant)),
                            ClausesFactory.create(false.lift(), ESReturns(ValuesFactory.NOT_NULL_CONSTANT))
                    ),
                    listOf()
            )
        }

        if (constant == ValuesFactory.NOT_NULL_CONSTANT) {
            return clauses(
                    listOf(
                            ClausesFactory.create(true.lift(), ESReturns(constant)),
                            ClausesFactory.create(false.lift(), ESReturns(null.lift()))
                    ),
                    listOf()
            )
        }

        return pureReturns(constant)
    }

    fun pureReturns(value: ESValue): EffectSchema = clauses(listOf(ClausesFactory.always(ESReturns(value))), listOf())
    fun singleClause(premise: ESBooleanExpression, effect: ESEffect, variables: List<ESVariable>) = clauses(listOf(ClausesFactory.create(premise, effect)), variables)
    fun clauses(clauses: List<ESClause>, params: List<ESVariable>): EffectSchema = EffectSchemaImpl(clauses, params)
}