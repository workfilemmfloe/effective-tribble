/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.*
import com.intellij.util.IncorrectOperationException

class PrimaryConstructor(annotations: Annotations,
                         modifiers: Modifiers,
                         val parameterList: ParameterList,
                         val body: DeferredElement<Block>)
  :  Member(annotations, modifiers) {

    override fun generateCode(builder: CodeBuilder) { throw IncorrectOperationException() }

    public fun initializer(): Initializer
            = Initializer(body, Modifiers.Empty).assignPrototypesFrom(this, CommentsAndSpacesInheritance(commentsBefore = false))

    public fun createSignature(converter: Converter): PrimaryConstructorSignature {
        val signature = PrimaryConstructorSignature(annotations, modifiers, parameterList)

        // assign prototypes later because we don't know yet whether the body is empty or not
        converter.addPostUnfoldDeferredElementsAction {
            val inheritance = CommentsAndSpacesInheritance(blankLinesBefore = false, commentsAfter = body.isEmpty, commentsInside = body.isEmpty)
            signature.assignPrototypesFrom(this, inheritance)
        }

        return signature
    }
}

class PrimaryConstructorSignature(val annotations: Annotations, val modifiers: Modifiers, val parameterList: ParameterList) : Element() {
    override fun generateCode(builder: CodeBuilder) {
        val accessModifier = modifiers.filter { it in ACCESS_MODIFIERS && it != Modifier.PUBLIC }
        if (accessModifier.isEmpty && annotations.isEmpty && parameterList.parameters.isEmpty()) return

        if (!annotations.isEmpty) {
            builder append " " append annotations.withBrackets()
        }

        if (!accessModifier.isEmpty) {
            builder append " " append accessModifier
        }

        builder append "(" append parameterList append ")"
    }
}

class FactoryFunction(name: Identifier,
                      annotations: Annotations,
                      modifiers: Modifiers,
                      returnType: Type,
                      parameterList: ParameterList,
                      typeParameterList: TypeParameterList,
                      body: DeferredElement<Block>)
: Function(name, annotations, modifiers, returnType, typeParameterList, parameterList, body, false)
