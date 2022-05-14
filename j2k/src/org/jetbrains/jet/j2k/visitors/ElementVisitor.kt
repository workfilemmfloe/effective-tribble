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

package org.jetbrains.jet.j2k.visitors

import com.intellij.psi.*
import org.jetbrains.jet.j2k.*
import org.jetbrains.jet.j2k.ast.*
import org.jetbrains.jet.j2k.ast.types.Type

open class ElementVisitor(val myConverter: Converter) : JavaElementVisitor() {
    protected var myResult: Element = Element.Empty

    fun getConverter(): Converter {
        return myConverter
    }

    open fun getResult(): Element {
        return myResult
    }

    override fun visitLocalVariable(variable: PsiLocalVariable?) {
        val theVariable = variable!!
        var kType = myConverter.convertType(theVariable.getType(), isAnnotatedAsNotNull(theVariable.getModifierList()))
        if (theVariable.hasModifierProperty(PsiModifier.FINAL) && isDefinitelyNotNull(theVariable.getInitializer())) {
            kType = kType.convertedToNotNull();
        }
        myResult = LocalVariable(Identifier(theVariable.getName()!!),
                                 myConverter.convertModifierList(theVariable.getModifierList()),
                                 kType,
                                 myConverter.convertExpression(theVariable.getInitializer(), theVariable.getType()),
                                 myConverter)
    }

    override fun visitExpressionList(list: PsiExpressionList?) {
        myResult = ExpressionList(myConverter.convertExpressions(list!!.getExpressions()))
    }

    override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement?) {
        val theReference = reference!!
        val types: List<Type> = myConverter.convertTypes(theReference.getTypeParameters())
        if (!theReference.isQualified()) {
            myResult = ReferenceElement(Identifier(theReference.getReferenceName()!!), types)
        }
        else {
            var result: String = Identifier(reference.getReferenceName()!!).toKotlin()
            var qualifier: PsiElement? = theReference.getQualifier()
            while (qualifier != null)
            {
                val p: PsiJavaCodeReferenceElement = (qualifier as PsiJavaCodeReferenceElement)
                result = Identifier(p.getReferenceName()!!).toKotlin() + "." + result
                qualifier = p.getQualifier()
            }
            myResult = ReferenceElement(Identifier(result), types)
        }
    }

    override fun visitTypeElement(`type`: PsiTypeElement?) {
        myResult = TypeElement(myConverter.convertType(`type`!!.getType()))
    }

    override fun visitTypeParameter(classParameter: PsiTypeParameter?) {
        myResult = TypeParameter(Identifier(classParameter!!.getName()!!),
                                 classParameter.getExtendsListTypes().map { myConverter.convertType(it) })
    }

    override fun visitParameterList(list: PsiParameterList?) {
        myResult = ParameterList(myConverter.convertParameterList(list!!.getParameters()).requireNoNulls())
    }

    override fun visitComment(comment: PsiComment?) {
        myResult = Comment(comment!!.getText()!!)
    }

    override fun visitWhiteSpace(space: PsiWhiteSpace?) {
        myResult = WhiteSpace(space!!.getText()!!)
    }
}
