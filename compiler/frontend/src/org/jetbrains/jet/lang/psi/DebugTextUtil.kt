/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi.debugText

import org.jetbrains.jet.lang.psi.JetElementImplStub
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.psi.JetClass
import org.jetbrains.jet.lang.psi.JetVisitor
import org.jetbrains.jet.lang.psi.JetModifierList
import org.jetbrains.jet.lang.psi.JetTypeParameterList
import org.jetbrains.jet.lang.psi.JetParameterList
import org.jetbrains.jet.lang.psi.JetDelegationSpecifierList
import org.jetbrains.jet.lang.psi.JetClassBody
import org.jetbrains.jet.lang.psi.JetStubbedPsiUtil
import org.jetbrains.jet.lang.psi.JetAnnotation
import org.jetbrains.jet.lang.psi.JetAnnotationEntry
import org.jetbrains.jet.lang.psi.JetClassInitializer
import org.jetbrains.jet.lang.psi.JetClassObject
import org.jetbrains.jet.lang.psi.JetConstructorCalleeExpression
import org.jetbrains.jet.lang.psi.JetDelegationSpecifier
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lang.psi.JetEnumEntry
import org.jetbrains.jet.lang.psi.JetInitializerList
import org.jetbrains.jet.lang.psi.JetFunctionType
import org.jetbrains.jet.lang.psi.JetTypeReference
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.lang.psi.JetTypeConstraintList
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.psi.JetNullableType
import org.jetbrains.jet.lang.psi.JetObjectDeclaration
import org.jetbrains.jet.lang.psi.JetParameter
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.JetPropertyAccessor
import org.jetbrains.jet.lang.psi.JetTypeArgumentList
import org.jetbrains.jet.lang.psi.JetTypeConstraint
import org.jetbrains.jet.lang.psi.JetTypeParameter
import org.jetbrains.jet.lang.psi.JetTypeProjection
import org.jetbrains.jet.lang.psi.JetUserType
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.psi.JetPackageDirective
import org.jetbrains.jet.lang.psi.JetImportDirective
import org.jetbrains.jet.lang.psi.JetImportList

// invoke this instead of getText() when you need debug text to identify some place in PSI without storing the element itself
// this is need to avoid unnecessary file parses
// this defaults to get text if the element is not stubbed
public fun JetElement.getDebugText(): String? {
    if (this !is JetElementImplStub<*> || this.getStub() == null) {
        return getText()
    }
    if (this is JetPackageDirective) {
        val fqName = getFqName()
        if (fqName.isRoot()) {
            return ""
        }
        return "package " + fqName.asString()
    }
    return accept(DebugTextBuildingVisitor, Unit)
}


private object DebugTextBuildingVisitor : JetVisitor<String, Unit>() {

    private val LOG = Logger.getInstance(this.javaClass)

    override fun visitJetFile(file: JetFile, data: Unit?): String? {
        return "STUB file: ${file.getName()}"
    }

    override fun visitJetElement(element: JetElement, data: Unit?): String? {
        if (element is JetElementImplStub<*>) {
            LOG.error("getDebugText() is not defined for ${element.javaClass}")
        }
        return element.getText()
    }

    override fun visitImportDirective(importDirective: JetImportDirective, data: Unit?): String? {
        val importPath = importDirective.getImportPath()
        if (importPath == null) {
            return "import <invalid>"
        }
        val aliasStr = if (importPath.hasAlias()) " as " + importPath.getAlias()!!.asString() else ""
        return "import ${importPath.getPathStr()}" + aliasStr
    }

    override fun visitImportList(importList: JetImportList, data: Unit?): String? {
        return renderChildren(importList, separator = "\n")
    }

    override fun visitAnnotationEntry(annotationEntry: JetAnnotationEntry, data: Unit?): String? {
        return render(annotationEntry, annotationEntry.getCalleeExpression(), annotationEntry.getTypeArgumentList())
    }

    override fun visitTypeReference(typeReference: JetTypeReference, data: Unit?): String? {
        return renderChildren(typeReference, " ")
    }

    override fun visitTypeArgumentList(typeArgumentList: JetTypeArgumentList, data: Unit?): String? {
        return renderChildren(typeArgumentList, ", ", "<", ">")
    }

    override fun visitTypeConstraintList(list: JetTypeConstraintList, data: Unit?): String? {
        return renderChildren(list, ", ", "where ", "")
    }

    override fun visitUserType(userType: JetUserType, data: Unit?): String? {
        return render(userType, userType.getQualifier(), userType.getReferenceExpression(), userType.getTypeArgumentList())
    }

    override fun visitAnnotation(annotation: JetAnnotation, data: Unit?): String? {
        return renderChildren(annotation, " ", "[", "]")
    }

    override fun visitConstructorCalleeExpression(constructorCalleeExpression: JetConstructorCalleeExpression, data: Unit?): String? {
        return render(constructorCalleeExpression, constructorCalleeExpression.getConstructorReferenceExpression())
    }

    override fun visitDelegationSpecifier(specifier: JetDelegationSpecifier, data: Unit?): String? {
        return render(specifier, specifier.getTypeReference())
    }

    override fun visitDelegationSpecifierList(list: JetDelegationSpecifierList, data: Unit?): String? {
        return renderChildren(list, ", ")
    }

    override fun visitTypeParameterList(list: JetTypeParameterList, data: Unit?): String? {
        return renderChildren(list, ", ", "<", ">")
    }

    override fun visitDotQualifiedExpression(expression: JetDotQualifiedExpression, data: Unit?): String? {
        return renderChildren(expression, ".")
    }

    override fun visitInitializerList(list: JetInitializerList, data: Unit?): String? {
        return renderChildren(list, ", ")
    }

    override fun visitParameterList(list: JetParameterList, data: Unit?): String? {
        return renderChildren(list, ", ", "(", ")")
    }

    override fun visitEnumEntry(enumEntry: JetEnumEntry, data: Unit?): String? {
        return buildText {
            append("STUB: ")
            appendInn(enumEntry.getModifierList(), suffix = " ")
            append("enum entry ")
            appendInn(enumEntry.getNameAsName())
            appendInn(enumEntry.getInitializerList(), prefix = " : ")
        }
    }

    override fun visitFunctionType(functionType: JetFunctionType, data: Unit?): String? {
        return buildText {
            appendInn(functionType.getReceiverTypeReference(), suffix = ".")
            appendInn(functionType.getParameterList())
            appendInn(functionType.getReturnTypeReference(), prefix = " -> ")
        }
    }

    override fun visitTypeParameter(parameter: JetTypeParameter, data: Unit?): String? {
        return buildText {
            appendInn(parameter.getModifierList(), suffix = " ")
            appendInn(parameter.getNameAsName())
            appendInn(parameter.getExtendsBound(), prefix = " : ")
        }
    }

    override fun visitTypeProjection(typeProjection: JetTypeProjection, data: Unit?): String? {
        return buildText {
            val token = typeProjection.getProjectionKind().getToken()
            appendInn(token?.getValue())
            val typeReference = typeProjection.getTypeReference()
            if (token != null && typeReference != null) {
                append(" ")
            }
            appendInn(typeReference)
        }
    }

    override fun visitModifierList(list: JetModifierList, data: Unit?): String? {
        return buildText {
            var first = true
            for (modifierKeywordToken in JetTokens.MODIFIER_KEYWORDS_ARRAY) {
                if (list.hasModifier(modifierKeywordToken)) {
                    if (!first) {
                        append(" ")
                    }
                    append(modifierKeywordToken.getValue())
                    first = false
                }
            }
        }
    }

    override fun visitSimpleNameExpression(expression: JetSimpleNameExpression, data: Unit?): String? {
        return expression.getReferencedName()
    }

    override fun visitNullableType(nullableType: JetNullableType, data: Unit?): String? {
        return renderChildren(nullableType, "", "", "?")
    }

    override fun visitAnonymousInitializer(initializer: JetClassInitializer, data: Unit?): String? {
        val containingDeclaration = JetStubbedPsiUtil.getContainingDeclaration(initializer)
        return "initializer in " + (containingDeclaration?.getDebugText() ?: "...")
    }

    override fun visitClassObject(classObject: JetClassObject, data: Unit?): String? {
        val containingDeclaration = JetStubbedPsiUtil.getContainingDeclaration(classObject)
        return "class object in " + (containingDeclaration?.getDebugText() ?: "...")
    }

    override fun visitClassBody(classBody: JetClassBody, data: Unit?): String? {
        val containingDeclaration = JetStubbedPsiUtil.getContainingDeclaration(classBody)
        return "class body for " + (containingDeclaration?.getDebugText() ?: "...")
    }

    override fun visitPropertyAccessor(accessor: JetPropertyAccessor, data: Unit?): String? {
        val containingProperty = JetStubbedPsiUtil.getContainingDeclaration(accessor, javaClass<JetProperty>())
        val what = (if (accessor.isGetter()) "getter" else "setter")
        return what + " for " + (if (containingProperty != null) containingProperty.getDebugText() else "...")
    }

    override fun visitClass(klass: JetClass, data: Unit?): String? {
        return buildText {
            append("STUB: ")
            appendInn(klass.getModifierList(), suffix = " ")
            append("class ")
            appendInn(klass.getNameAsName())
            appendInn(klass.getTypeParameterList())
            appendInn(klass.getPrimaryConstructorModifierList(), prefix = " ", suffix = " ")
            appendInn(klass.getPrimaryConstructorParameterList())
            appendInn(klass.getDelegationSpecifierList(), prefix = " : ")
        }
    }

    override fun visitNamedFunction(function: JetNamedFunction, data: Unit?): String? {
        return buildText {
            append("STUB: ")
            appendInn(function.getModifierList(), suffix = " ")
            append("fun ")

            val typeParameterList = function.getTypeParameterList()
            if (function.hasTypeParameterListBeforeFunctionName()) {
                appendInn(typeParameterList, suffix = " ")
            }
            appendInn(function.getReceiverTypeReference(), suffix = ".")
            appendInn(function.getNameAsName())
            if (!function.hasTypeParameterListBeforeFunctionName()) {
                appendInn(typeParameterList)
            }
            appendInn(function.getValueParameterList())
            appendInn(function.getTypeReference(), prefix = ": ")
            appendInn(function.getTypeConstraintList(), prefix = " ")
        }
    }

    override fun visitObjectDeclaration(declaration: JetObjectDeclaration, data: Unit?): String? {
        return buildText {
            append("STUB: ")
            appendInn(declaration.getModifierList(), suffix = " ")
            append("object ")
            appendInn(declaration.getNameAsName())
            appendInn(declaration.getDelegationSpecifierList(), prefix = " : ")
        }
    }

    override fun visitParameter(parameter: JetParameter, data: Unit?): String? {
        return buildText {
            if (parameter.hasValOrVarNode()) {
                if (parameter.isMutable()) append("var ") else append("val ")
            }
            val name = parameter.getNameAsName()
            appendInn(name)
            val typeReference = parameter.getTypeReference()
            if (typeReference != null && name != null) {
                append(": ")
            }
            appendInn(typeReference)
        }
    }

    override fun visitProperty(property: JetProperty, data: Unit?): String? {
        return buildText {
            append("STUB: ")
            appendInn(property.getModifierList(), suffix = " ")
            append(if (property.isVar()) "var " else "val ")
            appendInn(property.getNameAsName())
            appendInn(property.getTypeReference(), prefix = ": ")
        }
    }

    override fun visitTypeConstraint(constraint: JetTypeConstraint, data: Unit?): String? {
        return buildText {
            if (constraint.isClassObjectConstraint()) {
                append("class object ")
            }
            appendInn(constraint.getSubjectTypeParameterName())
            appendInn(constraint.getBoundTypeReference(), prefix = " : ")
        }
    }

    fun buildText(body: StringBuilder.() -> Unit): String? {
        val sb = StringBuilder()
        sb.body()
        return sb.toString()
    }

    fun renderChildren(element: JetElementImplStub<*>, separator: String, prefix: String = "", postfix: String = ""): String? {
        val childrenTexts = element.getStub()?.getChildrenStubs()?.map { (it?.getPsi() as? JetElement)?.getDebugText() }
        return childrenTexts?.filterNotNull()?.makeString(separator, prefix, postfix) ?: element.getText()
    }

    fun render(element: JetElementImplStub<*>, vararg relevantChildren: JetElement?): String? {
        if (element.getStub() == null) return element.getText()
        return relevantChildren.filterNotNull().map { it.getDebugText() }.makeString("", "", "")
    }
}

private fun StringBuilder.appendInn(target: Any?, prefix: String = "", suffix: String = "") {
    if (target == null) return
    append(prefix)
    append(when (target) {
               is JetElement -> target.getDebugText()
               else -> target.toString()
           })
    append(suffix)
}
