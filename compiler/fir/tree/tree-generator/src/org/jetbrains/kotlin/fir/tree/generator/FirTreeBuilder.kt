/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.Element.Kind.*

object FirTreeBuilder : AbstractFirTreeBuilder() {
    val annotationContainer = element("AnnotationContainer", Other)
    val typeRef = element("TypeRef", TypeRef, annotationContainer)
    val reference = element("Reference", Reference)
    val label = element("Label", Other)
    val import = element("Import", Declaration)
    val resolvedImport = element("ResolvedImport", Declaration, import)
    val symbolOwner = element("SymbolOwner", Other)
    val resolvable = element("Resolvable", Expression)

    val controlFlowGraphOwner = element("ControlFlowGraphOwner", Other)
    val targetElement = element("TargetElement", Other)

    val declarationStatus = element("DeclarationStatus", Declaration)
    val resolvedDeclarationStatus = element("ResolvedDeclarationStatus", Declaration, declarationStatus)

    val statement = element("Statement", Expression, annotationContainer)
    val expression = element("Expression", Expression, statement)
    val declaration = element("Declaration", Declaration)
    val anonymousInitializer = element("AnonymousInitializer", Declaration, declaration)
    val typedDeclaration = element("TypedDeclaration", Declaration, declaration, annotationContainer)
    val callableDeclaration = element("CallableDeclaration", Declaration, typedDeclaration, symbolOwner)
    val namedDeclaration = element("NamedDeclaration", Declaration, declaration)
    val typeParameter = element("TypeParameter", Declaration, namedDeclaration, symbolOwner, annotationContainer)
    val typeParametersOwner = element("TypeParametersOwner", Declaration)
    val memberDeclaration = element("MemberDeclaration", Declaration, namedDeclaration, annotationContainer)
    val callableMemberDeclaration = element("CallableMemberDeclaration", Declaration, callableDeclaration, memberDeclaration)

    val variable = element("Variable", Declaration, callableDeclaration, namedDeclaration, statement)
    val valueParameter = element("ValueParameter", Declaration, variable)
    val property = element("Property", Declaration, variable, controlFlowGraphOwner, typeParametersOwner, callableMemberDeclaration)
    val field = element("Field", Declaration, variable, callableMemberDeclaration) // TODO: add noImpl
    val klass = element("Class", Declaration, declaration, statement, annotationContainer)
    val classLikeDeclaration = element("ClassLikeDeclaration", Declaration, statement, memberDeclaration, symbolOwner, typeParametersOwner)
    val regularClass = element("RegularClass", Declaration, namedDeclaration, classLikeDeclaration, klass)
    val typeAlias = element("TypeAlias", Declaration, classLikeDeclaration)
    val enumEntry = element("EnumEntry", Declaration, regularClass)

    val function = element("Function", Declaration, callableDeclaration, controlFlowGraphOwner, targetElement, annotationContainer, typeParametersOwner, statement)

    val memberFunction = element("MemberFunction", Declaration, function, callableMemberDeclaration)
    val simpleFunction = element("SimpleFunction", Declaration, memberFunction)
    val propertyAccessor = element("PropertyAccessor", Declaration, function)
    val constructor = element("Constructor", Declaration, memberFunction)
    val file = element("File", Declaration, annotationContainer, declaration)

    val anonymousFunction = element("AnonymousFunction", Declaration, function, expression)
    val anonymousObject = element("AnonymousObject", Declaration, klass, expression)

    val loop = element("Loop", Expression, statement, targetElement, annotationContainer)
    val doWhileLoop = element("DoWhileLoop", Expression, loop)
    val whileLoop = element("WhileLoop", Expression, loop)

    val block = element("Block", Expression, expression)
    val binaryLogicExpression = element("BinaryLogicExpression", Expression, expression)
    val jump = element("Jump", Expression, expression)
    val loopJump = element("LoopJump", Expression, jump)
    val breakExpression = element("BreakExpression", Expression, loopJump)
    val continueExpression = element("ContinueExpression", Expression, loopJump)
    val catchClause = element("Catch", Expression)
    val tryExpression = element("TryExpression", Expression, expression, resolvable)
    val constExpression = element("ConstExpression", Expression, expression)
    val typeProjection = element("TypeProjection", TypeRef)
    val starProjection = element("StarProjection", TypeRef, typeProjection)
    val typeProjectionWithVariance = element("TypeProjectionWithVariance", TypeRef, typeProjection)
    val call = element("Call", Expression, statement) // TODO: may smth like `CallWithArguments` or `ElementWithArguments`?
    val annotationCall = element("AnnotationCall", Expression, expression, call)
    val operatorCall = element("OperatorCall", Expression, expression, call)
    val typeOperatorCall = element("TypeOperatorCall", Expression, expression, call)
    val whenExpression = element("WhenExpression", Expression, expression, resolvable)
    val whenBranch = element("WhenBranch", Expression)
    val delegatedConstructorCall = element("DelegatedConstructorCall", Expression, call)
    val qualifiedAccessWithoutCallee = element("QualifiedAccessWithoutCallee", Expression, statement)
    val qualifiedAccess = element("QualifiedAccess", Expression, qualifiedAccessWithoutCallee, resolvable)

    val arrayOfCall = element("ArrayOfCall", Expression, expression, call)
    val arraySetCall = element("ArraySetCall", Expression, qualifiedAccess, call)
    val classReferenceExpression = element("ClassReferenceExpression", Expression, expression)
    val errorExpression = element("ErrorExpression", Expression, expression)
    val errorFunction = element("ErrorFunction", Declaration, function)
    val qualifiedAccessExpression = element("QualifiedAccessExpression", Expression, expression, qualifiedAccess)
    val functionCall = element("FunctionCall", Expression, qualifiedAccessExpression, call)
    val componentCall = element("ComponentCall", Expression, functionCall)
    val callableReferenceAccess = element("CallableReferenceAccess", Expression, qualifiedAccessExpression)
    val thisReceiverExpression = element("ThisReceiverExpression", Expression, qualifiedAccessExpression)
    val expressionWithSmartcast = element("ExpressionWithSmartcast", Expression, qualifiedAccessExpression)
    val getClassCall = element("GetClassCall", Expression, expression, call)
    val wrappedExpression = element("WrappedExpression", Expression, expression)
    val wrappedArgumentExpression = element("WrappedArgumentExpression", Expression, wrappedExpression)
    val lambdaArgumentExpression = element("LambdaArgumentExpression", Expression, wrappedArgumentExpression)
    val spreadArgumentExpression = element("SpreadArgumentExpression", Expression, wrappedArgumentExpression)
    val namedArgumentExpression = element("NamedArgumentExpression", Expression, wrappedArgumentExpression)

    val resolvedQualifier = element("ResolvedQualifier", Expression, expression)
    val returnExpression = element("ReturnExpression", Expression, jump)
    val stringConcatenationCall = element("StringConcatenationCall", Expression, call, expression)
    val throwExpression = element("ThrowExpression", Expression, expression)
    val variableAssignment = element("VariableAssignment", Expression, qualifiedAccess)
    val whenSubjectExpression = element("WhenSubjectExpression", Expression, expression)

    val wrappedDelegateExpression = element("WrappedDelegateExpression", Expression, wrappedExpression)

    val namedReference = element("NamedReference", Reference, reference)
    val errorNamedReference = element("ErrorNamedReference", Reference, namedReference)
    val superReference = element("SuperReference", Reference, reference)
    val thisReference = element("ThisReference", Reference, reference)
    val controlFlowGraphReference = element("ControlFlowGraphReference", Reference, reference)

    val resolvedCallableReference = element("ResolvedCallableReference", Reference, namedReference)
    val delegateFieldReference = element("DelegateFieldReference", Reference, resolvedCallableReference)
    val backingFieldReference = element("BackingFieldReference", Reference, resolvedCallableReference)

    val resolvedTypeRef = element("ResolvedTypeRef", TypeRef, typeRef)
    val errorTypeRef = element("ErrorTypeRef", TypeRef, resolvedTypeRef)
    val delegatedTypeRef = element("DelegatedTypeRef", TypeRef, typeRef)
    val typeRefWithNullability = element("TypeRefWithNullability", TypeRef, typeRef)
    val userTypeRef = element("UserTypeRef", TypeRef, typeRefWithNullability)
    val dynamicTypeRef = element("DynamicTypeRef", TypeRef, typeRefWithNullability)
    val functionTypeRef = element("FunctionTypeRef", TypeRef, typeRefWithNullability)
    val resolvedFunctionTypeRef = element("ResolvedFunctionTypeRef", TypeRef, resolvedTypeRef, functionTypeRef)
    val implicitTypeRef = element("ImplicitTypeRef", TypeRef, typeRef)
}