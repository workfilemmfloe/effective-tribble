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

package org.jetbrains.kotlin.j2k

import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.*
import com.intellij.psi.JavaTokenType.SUPER_KEYWORD
import com.intellij.psi.JavaTokenType.THIS_KEYWORD
import com.intellij.psi.impl.source.tree.ChildRole
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl
import com.intellij.psi.impl.source.tree.java.PsiNewExpressionImpl
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl
import org.jetbrains.kotlin.j2k.ast.Mutability
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.JKLiteralExpression.LiteralType.*
import org.jetbrains.kotlin.j2k.tree.impl.*


class JavaToJKTreeBuilder(var symbolProvider: JKSymbolProvider) {

    private val expressionTreeMapper = ExpressionTreeMapper()

    private val declarationMapper = DeclarationMapper(expressionTreeMapper)

    private val modifierMapper = ModifierMapper()

    val backAnnotation = mutableMapOf<JKElement, PsiElement>()

    private inner class ExpressionTreeMapper {
        fun PsiExpression?.toJK(): JKExpression {
            return when (this) {
                null -> JKStubExpressionImpl()
                is PsiBinaryExpression -> toJK()
                is PsiPrefixExpression -> toJK()
                is PsiPostfixExpression -> toJK()
                is PsiLiteralExpression -> toJK()
                is PsiMethodCallExpression -> toJK()
                is PsiReferenceExpression -> toJK()
                is PsiNewExpression -> toJK()
                is PsiArrayAccessExpression -> toJK()
                is PsiTypeCastExpression -> toJK()
                is PsiParenthesizedExpression -> toJK()
                is PsiAssignmentExpression -> toJK()
                is PsiInstanceOfExpression -> toJK()
                is PsiThisExpression -> JKThisExpressionImpl()
                is PsiSuperExpression -> JKSuperExpressionImpl()
                is PsiConditionalExpression -> JKIfElseExpressionImpl(
                    condition.toJK(), thenExpression.toJK(), elseExpression.toJK()
                )
                is PsiPolyadicExpression -> JKJavaPolyadicExpressionImpl(
                    operands.map { it.toJK() },
                    Array(operands.lastIndex) { getTokenBeforeOperand(operands[it + 1]) }.map { it?.toJK() ?: TODO() }
                )
                is PsiArrayInitializerExpression -> toJK()
                is PsiLambdaExpression -> toJK()
                else -> {
                    throw RuntimeException("Not supported: ${this::class}")
                }
            }
        }

        fun PsiInstanceOfExpression.toJK(): JKJavaInstanceOfExpression {
            return JKJavaInstanceOfExpressionImpl(operand.toJK(), checkType?.toJK() ?: TODO())
        }

        fun PsiAssignmentExpression.toJK(): JKJavaAssignmentExpression {
            return JKJavaAssignmentExpressionImpl(
                lExpression.toJK() as? JKAssignableExpression ?: error("Its possible? ${lExpression.toJK().prettyDebugPrintTree()}"),
                rExpression.toJK(),
                operationSign.toJK()
            )
        }

        fun PsiBinaryExpression.toJK(): JKExpression {
            return JKBinaryExpressionImpl(lOperand.toJK(), rOperand.toJK(), operationSign.toJK())
        }

        fun PsiLiteralExpression.toJK(): JKLiteralExpression {
            require(this is PsiLiteralExpressionImpl)

            return when (this.literalElementType) {
                JavaTokenType.NULL_KEYWORD -> JKNullLiteral()
                JavaTokenType.TRUE_KEYWORD -> JKBooleanLiteral(true)
                JavaTokenType.FALSE_KEYWORD -> JKBooleanLiteral(false)
                JavaTokenType.STRING_LITERAL -> JKJavaLiteralExpressionImpl(text, STRING)
                JavaTokenType.CHARACTER_LITERAL -> JKJavaLiteralExpressionImpl(text, CHAR)
                JavaTokenType.INTEGER_LITERAL -> JKJavaLiteralExpressionImpl(text, INT)
                JavaTokenType.LONG_LITERAL -> JKJavaLiteralExpressionImpl(text, LONG)
                JavaTokenType.FLOAT_LITERAL -> JKJavaLiteralExpressionImpl(text, FLOAT)
                JavaTokenType.DOUBLE_LITERAL -> JKJavaLiteralExpressionImpl(text, DOUBLE)
                else -> error("Unknown literal element type: ${this.literalElementType}")
            }
        }

        fun PsiJavaToken.toJK(): JKOperator = JKJavaOperatorImpl.tokenToOperator[tokenType] ?: error("Unsupported token-type: $tokenType")

        fun PsiPrefixExpression.toJK(): JKExpression {
            return JKPrefixExpressionImpl(operand.toJK(), operationSign.toJK())
        }

        fun PsiPostfixExpression.toJK(): JKExpression {
            return JKPostfixExpressionImpl(operand.toJK(), operationSign.toJK())
        }

        fun PsiLambdaExpression.toJK(): JKExpression {
            return JKLambdaExpressionImpl(
                with(declarationMapper) { parameterList.parameters.map { it.toJK() } },
                JKTypeElementImpl(JKJavaVoidType),
                body.let {
                    when (it) {
                        is PsiExpression -> JKExpressionStatementImpl(it.toJK())
                        is PsiCodeBlock -> JKBlockStatementImpl(with(declarationMapper) { it.toJK() })
                        else -> JKBlockStatementImpl(JKBodyStub)
                    }
                })
        }

        fun PsiMethodCallExpression.toJK(): JKExpression {
            val method = methodExpression as PsiReferenceExpressionImpl
            val referenceNameElement = methodExpression.referenceNameElement
            val symbol = symbolProvider.provideSymbol<JKMethodSymbol>(method)
            return if (referenceNameElement is PsiKeyword) {
                val callee = when (referenceNameElement.tokenType) {
                    SUPER_KEYWORD -> JKSuperExpressionImpl()
                    THIS_KEYWORD -> JKThisExpressionImpl()
                    else -> error("Unknown keyword in callee position")
                }
                JKDelegationConstructorCallImpl(symbol, callee, argumentList.toJK())
            } else {
                val call = JKJavaMethodCallExpressionImpl(symbol, argumentList.toJK())
                if (method.findChildByRole(ChildRole.DOT) != null) {
                    JKQualifiedExpressionImpl((method.qualifier as PsiExpression).toJK(), JKJavaQualifierImpl.DOT, call)
                } else {
                    call
                }
            }
        }

        fun PsiReferenceExpression.toJK(): JKExpression {
            val symbol = symbolProvider.provideSymbol(this)
            val access = when (symbol) {
                is JKClassSymbol -> JKClassAccessExpressionImpl(symbol)
                is JKFieldSymbol -> JKJavaFieldAccessExpressionImpl(symbol)
                else -> TODO()
            }
            return qualifierExpression?.let { JKQualifiedExpressionImpl(it.toJK(), JKJavaQualifierImpl.DOT, access) } ?: access
        }

        fun PsiArrayInitializerExpression.toJK(): JKExpression {
            return JKJavaNewArrayImpl(initializers.map { it.toJK() })
        }

        fun PsiNewExpression.toJK(): JKExpression {
            assert(this is PsiNewExpressionImpl)
            if ((this as PsiNewExpressionImpl).findChildByRole(ChildRole.LBRACKET) != null) {
                return arrayInitializer?.toJK() ?: run {
                    val dimensions = mutableListOf<PsiLiteralExpression?>()
                    var child = firstChild
                    while (child != null) {
                        if (child.node.elementType == JavaTokenType.LBRACKET) {
                            child = child.nextSibling
                            if (child.node.elementType == JavaTokenType.RBRACKET) {
                                dimensions.add(null)
                            } else {
                                dimensions.add(child as PsiLiteralExpression?)
                            }
                        }
                        child = child.nextSibling
                    }
                    JKJavaNewEmptyArrayImpl(dimensions.map { it?.toJK() })
                }
            }
            val constructedClass = classOrAnonymousClassReference?.resolve()
            val constructor = constructorFakeReference.resolve()
            if (constructor == null && constructedClass != null) {
                return JKJavaDefaultNewExpressionImpl(
                    symbolProvider.provideDirectSymbol(constructedClass) as JKClassSymbol

                )
            }

            return JKJavaNewExpressionImpl(
                symbolProvider.provideDirectSymbol(constructor!!) as JKMethodSymbol,
                argumentList.toJK()
            )
        }

        fun PsiArrayAccessExpression.toJK(): JKExpression {
            return JKArrayAccessExpressionImpl(arrayExpression.toJK(), indexExpression?.toJK() ?: TODO())
        }

        fun PsiTypeCastExpression.toJK(): JKExpression {
            return JKTypeCastExpressionImpl(operand?.toJK() ?: TODO(), castType?.toJK() ?: TODO())
        }

        fun PsiParenthesizedExpression.toJK(): JKExpression {
            return JKParenthesizedExpressionImpl(expression?.toJK() ?: TODO())
        }

        fun PsiExpressionList?.toJK(): JKExpressionList {
            return JKExpressionListImpl(this?.expressions?.map { it.toJK() } ?: emptyList())
        }

        fun PsiTypeElement.toJK(): JKTypeElement {
            return JKTypeElementImpl(type.toJK()).also {
                backAnnotation[it] = this
            }
        }

        fun PsiType.toJK(nullability: Nullability = Nullability.Default): JKType {
            return when (this) {
                is PsiClassType -> {
                    val target = resolve()
                    val parameters = parameters.map { it.toJK() }
                    if (target != null) {
                        JKClassTypeImpl(
                            target.let { symbolProvider.provideDirectSymbol(it) as JKClassSymbol },
                            parameters,
                            nullability
                        )
                    } else {
                        JKUnresolvedClassType(this.rawType().canonicalText, parameters, nullability)
                    }
                }
                is PsiArrayType -> JKJavaArrayTypeImpl(componentType.toJK(), nullability)
                is PsiPrimitiveType -> JKJavaPrimitiveTypeImpl.KEYWORD_TO_INSTANCE[presentableText]
                        ?: error("Invalid primitive type $presentableText")
                else -> throw Exception("Invalid PSI")
            }
        }
    }

    private inner class DeclarationMapper(val expressionTreeMapper: ExpressionTreeMapper) {
        fun PsiClass.toJK(): JKClass {
            val classKind: JKClass.ClassKind = when {
                isAnnotationType -> JKClass.ClassKind.ANNOTATION
                isEnum -> JKClass.ClassKind.ENUM
                isInterface -> JKClass.ClassKind.INTERFACE
                else -> JKClass.ClassKind.CLASS
            }

            fun PsiReferenceList.mapTypes() =
                this.referencedTypes.map { with(expressionTreeMapper) { JKTypeElementImpl(it.toJK(Nullability.NotNull)) } }

            val implTypes = this.implementsList?.mapTypes().orEmpty()
            val extTypes = this.extendsList?.mapTypes().orEmpty()
            return JKClassImpl(
                with(modifierMapper) { modifierList.toJK() }, JKNameIdentifierImpl(name!!), JKInheritanceInfoImpl(extTypes + implTypes),
                classKind
            ).also { jkClassImpl ->
                jkClassImpl.declarationList = children.mapNotNull {
                    ElementVisitor().apply { it.accept(this) }.resultElement as? JKDeclaration
                }
                backAnnotation[jkClassImpl] = this
                symbolProvider.provideUniverseSymbol(this, jkClassImpl)
            }
        }

        fun PsiField.toJK(): JKJavaField {
            return JKJavaFieldImpl(
                with(modifierMapper) { modifierList.toJK(finalAsMutability = true) },
                with(expressionTreeMapper) { typeElement?.toJK() } ?: TODO(),
                JKNameIdentifierImpl(name),
                with(expressionTreeMapper) { initializer.toJK() }
            ).also {
                symbolProvider.provideUniverseSymbol(this, it)
            }
        }

        fun PsiMethod.toJK(): JKJavaMethod {
            return JKJavaMethodImpl(
                with(modifierMapper) { modifierList.toJK() },
                with(expressionTreeMapper) {
                    returnTypeElement?.toJK()
                            ?: JKTypeElementImpl(JKJavaVoidType).takeIf { isConstructor }
                            ?: TODO()
                },
                JKNameIdentifierImpl(name),
                parameterList.parameters.map { it.toJK() },
                body?.toJK() ?: JKBodyStub
            ).also {
                backAnnotation[it] = this
                symbolProvider.provideUniverseSymbol(this, it)
            }
        }

        fun PsiMember.toJK(): JKDeclaration? = when (this) {
            is PsiField -> this.toJK()
            is PsiMethod -> this.toJK()
            else -> null
        }

        fun PsiParameter.toJK(): JKParameter {
            return JKParameterImpl(with(expressionTreeMapper) { typeElement?.toJK() } ?: TODO(),
                                   JKNameIdentifierImpl(name!!),
                                   with(modifierMapper) { modifierList.toJK() }).also {
                symbolProvider.provideUniverseSymbol(this, it)
            }
        }

        fun PsiCodeBlock.toJK(): JKBlock {
            return JKBlockImpl(statements.map { it.toJK() })
        }

        fun Array<out PsiElement>.toJK(): List<JKDeclaration> {
            return this.map {
                if (it is PsiLocalVariable) {
                    JKLocalVariableImpl(
                        with(modifierMapper) { it.modifierList.toJK() },
                        with(expressionTreeMapper) { it.typeElement.toJK() },
                        JKNameIdentifierImpl(it.name ?: TODO()),
                        with(expressionTreeMapper) { it.initializer.toJK() }
                    ).also { i -> symbolProvider.provideUniverseSymbol(it, i) }
                } else TODO()
            }
        }

        fun PsiStatement?.toJK(): JKStatement {
            return when (this) {
                null -> JKExpressionStatementImpl(JKStubExpressionImpl())
                is PsiExpressionStatement -> JKExpressionStatementImpl(with(expressionTreeMapper) { expression.toJK() })
                is PsiReturnStatement -> JKReturnStatementImpl(with(expressionTreeMapper) { returnValue.toJK() })
                is PsiDeclarationStatement -> JKDeclarationStatementImpl(declaredElements.toJK())
                is PsiAssertStatement -> JKJavaAssertStatementImpl(with(expressionTreeMapper) { assertCondition.toJK() },
                                                                   with(expressionTreeMapper) { assertDescription.toJK() })
                is PsiIfStatement ->
                    if (elseElement == null)
                        JKIfStatementImpl(with(expressionTreeMapper) { condition.toJK() }, thenBranch.toJK())
                    else
                        JKIfElseStatementImpl(with(expressionTreeMapper) { condition.toJK() }, thenBranch.toJK(), elseBranch.toJK())

                is PsiForStatement -> JKJavaForLoopStatementImpl(
                    initialization.toJK(), with(expressionTreeMapper) { condition.toJK() }, update.toJK(), body.toJK()
                )
                is PsiBlockStatement -> JKBlockStatementImpl(codeBlock.toJK())
                is PsiWhileStatement -> JKWhileStatementImpl(with(expressionTreeMapper) { condition.toJK() }, body.toJK())
                is PsiDoWhileStatement -> JKDoWhileStatementImpl(body.toJK(), with(expressionTreeMapper) { condition.toJK() })
                is PsiSwitchStatement -> JKSwitchStatementImpl(with(expressionTreeMapper) { expression.toJK() }, body?.toJK() ?: TODO())
                is PsiSwitchLabelStatement -> if (isDefaultCase) JKSwitchDefaultLabelStatementImpl() else
                    JKSwitchLabelStatementImpl(with(expressionTreeMapper) { caseValue.toJK() })
                is PsiBreakStatement -> {
                    //labelIdentifier?.also { TODO("Label break") }
                    JKBreakStatementImpl()
                }
                else -> TODO("for ${this::class}")
            }
        }
    }

    private inner class ModifierMapper {
        fun PsiModifierList?.toJK(finalAsMutability: Boolean = false): JKModifierList {

            val modifiers = if (this == null) mutableListOf()
            else PsiModifier.MODIFIERS.filter { hasExplicitModifier(it) }.mapNotNull { modifierToJK(it, finalAsMutability) }.toMutableList()

            modifiers += extractAccess()
            modifiers += extractModality()

            return JKModifierListImpl(
                modifiers
            )
        }

        fun PsiModifierList?.extractModality(): JKModalityModifier {
            val modality = when {
                this == null -> JKModalityModifier.Modality.OPEN
                hasModifierProperty(PsiModifier.FINAL) -> JKModalityModifier.Modality.FINAL
                hasModifierProperty(PsiModifier.ABSTRACT) -> JKModalityModifier.Modality.ABSTRACT
                else -> JKModalityModifier.Modality.OPEN
            }
            return JKModalityModifierImpl(modality)
        }


        fun PsiModifierList?.extractAccess(): JKAccessModifier {
            val visibility = when {
                this == null -> JKAccessModifier.Visibility.PACKAGE_PRIVATE
                hasModifierProperty(PsiModifier.PACKAGE_LOCAL) -> JKAccessModifier.Visibility.PACKAGE_PRIVATE
                hasModifierProperty(PsiModifier.PRIVATE) -> JKAccessModifier.Visibility.PRIVATE
                hasModifierProperty(PsiModifier.PROTECTED) -> JKAccessModifier.Visibility.PROTECTED
                hasModifierProperty(PsiModifier.PUBLIC) -> JKAccessModifier.Visibility.PUBLIC
                else -> JKAccessModifier.Visibility.PACKAGE_PRIVATE
            }
            return JKAccessModifierImpl(visibility)
        }

        fun modifierToJK(name: String, finalAsMutability: Boolean): JKModifier? = when (name) {
            PsiModifier.NATIVE -> JKJavaModifierImpl(JKJavaModifier.JavaModifierType.NATIVE)
            PsiModifier.STATIC -> JKJavaModifierImpl(JKJavaModifier.JavaModifierType.STATIC)
            PsiModifier.STRICTFP -> JKJavaModifierImpl(JKJavaModifier.JavaModifierType.STRICTFP)
            PsiModifier.SYNCHRONIZED -> JKJavaModifierImpl(JKJavaModifier.JavaModifierType.SYNCHRONIZED)
            PsiModifier.TRANSIENT -> JKJavaModifierImpl(JKJavaModifier.JavaModifierType.TRANSIENT)
            PsiModifier.VOLATILE -> JKJavaModifierImpl(JKJavaModifier.JavaModifierType.VOLATILE)

            PsiModifier.PROTECTED -> null
            PsiModifier.PUBLIC -> null
            PsiModifier.PRIVATE -> null

            PsiModifier.FINAL -> if (finalAsMutability) JKMutabilityModifierImpl(Mutability.NonMutable) else null
            PsiModifier.ABSTRACT -> null

            else -> TODO("Not yet supported")
        }

    }

    private inner class ElementVisitor : JavaElementVisitor() {

        var resultElement: JKTreeElement? = null

        override fun visitClass(aClass: PsiClass) {
            resultElement = with(declarationMapper) { aClass.toJK() }
        }

        override fun visitField(field: PsiField) {
            resultElement = with(declarationMapper) { field.toJK() }
        }

        override fun visitMethod(method: PsiMethod) {
            resultElement = with(declarationMapper) { method.toJK() }
        }

        override fun visitFile(file: PsiFile) {
            resultElement = JKFileImpl().apply {
                declarationList += file.children.mapNotNull { ElementVisitor().apply { it.accept(this) }.resultElement as? JKDeclaration }
            }
        }
    }


    fun buildTree(psi: PsiElement): JKTreeElement? {
        assert(psi.language.`is`(JavaLanguage.INSTANCE)) { "Unable to build JK Tree using Java Visitor for language ${psi.language}" }
        val elementVisitor = ElementVisitor()
        psi.accept(elementVisitor)
        return elementVisitor.resultElement
    }
}

