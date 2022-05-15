/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.tree.generator.FieldSets.annotations
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.arguments
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.body
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.calleeReference
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.classKind
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.controlFlowGraphReferenceField
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.declarations
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.initializer
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.modality
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.name
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.receivers
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.returnTypeRef
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.status
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.superTypeRefs
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.symbol
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.symbolWithPackage
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.typeArguments
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.typeParameters
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.typeRefField
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.valueParameters
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.visibility
import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFieldConfigurator
import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.context.type
import org.jetbrains.kotlin.fir.tree.generator.model.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

object NodeConfigurator : AbstractFieldConfigurator() {
    fun configureFields() = with(FirTreeBuilder) {
        AbstractFirTreeBuilder.baseFirElement.configure {
            +field("psi", psiElementType, nullable = true)
        }

        annotationContainer.configure {
            +annotations
        }

        symbolOwner.configure {
            withArg("E", symbolOwner, declaration)
            +symbolWithPackage("fir.symbols", "AbstractFirBasedSymbol", "E")
        }

        controlFlowGraphOwner.configure {
            +controlFlowGraphReferenceField.withTransform()
        }

        typeParametersOwner.configure {
            +typeParameters
        }

        resolvable.configure {
            +field("calleeReference", reference).withTransform()
        }

        declaration.configure {
            +field("session", firSessionType)
            +field("resolvePhase", resolvePhaseType, withReplace = true).apply { isMutable = true }
        }

        typedDeclaration.configure {
            +field("returnTypeRef", typeRef).withTransform()
        }

        callableDeclaration.configure {
            withArg("F", "FirCallableDeclaration<F>")
            parentArg(symbolOwner, "E", "F")
            +field("receiverTypeRef", typeRef, nullable = true)
            +symbol("FirCallableSymbol", "F")
        }

        callableMemberDeclaration.configure {
            withArg("F", "FirCallableMemberDeclaration<F>")
            parentArg(callableDeclaration, "F", "F")
            +field("containerSource", type(DeserializedContainerSource::class), nullable = true)
        }

        namedDeclaration.configure {
            +name
        }

        function.configure {
            withArg("F", "FirFunction<F>")
            parentArg(callableDeclaration, "F", "F")
            +symbol("FirFunctionSymbol", "F")
            +valueParameters.withTransform()
            +body(nullable = true)
        }

        errorFunction.configure {
            parentArg(function, "F", errorFunction)
            +stringField("reason")
            +symbol("FirErrorFunctionSymbol")
        }

        memberDeclaration.configure {
            +typeParameters
            +status
        }

        expression.configure {
            +typeRefField
            +annotations
        }

        call.configure {
            +arguments.withTransform()
        }

        block.configure {
            +fieldList(statement)
            +typeRefField
        }

        binaryLogicExpression.configure {
            +field("leftOperand", expression).withTransform()
            +field("rightOperand", expression).withTransform()
            +field("kind", operationKindType)
            needTransformOtherChildren()
        }

        jump.configure {
            withArg("E", targetElement)
            +field("target", jumpTargetType.withArgs("E"))
        }

        loopJump.configure {
            parentArg(jump, "E", loop)
        }

        returnExpression.configure {
            parentArg(jump, "E", function.withArgs("F" to "*"))
            +field("result", expression)
        }

        label.configure {
            +stringField("name")
        }

        loop.configure {
            +field(block).withTransform()
            +field("condition", expression).withTransform()
            +field(label, nullable = true)
            needTransformOtherChildren()
        }

        whileLoop.configure {
            +field("condition", expression).withTransform()
            +field(block).withTransform()
        }

        catchClause.configure {
            +field("parameter", valueParameter).withTransform()
            +field(block).withTransform()
            needTransformOtherChildren()
        }

        tryExpression.configure {
            +field("tryBlock", block).withTransform()
            +fieldList("catches", catchClause).withTransform()
            +field("finallyBlock", block, nullable = true).withTransform()
            needTransformOtherChildren()
        }

        qualifiedAccessWithoutCallee.configure {
            +booleanField("safe")
            +receivers
        }

        constExpression.configure {
            withArg("T")
            +field("kind", constKindType.withArgs("T"))
            +field("value", "T", null)
        }

        functionCall.configure {
            +typeArguments
            +field("calleeReference", namedReference)
        }

        operatorCall.configure {
            +field("operation", operationType)
        }

        typeOperatorCall.configure {
            +field("operation", operationType)
            +field("conversionTypeRef", typeRef)
        }

        whenBranch.configure {
            +field("condition", expression).withTransform()
            +field("result", block).withTransform()
            needTransformOtherChildren()
        }

        klass.configure {
            +classKind
            +superTypeRefs(withReplace = true)
            +declarations
            +annotations
        }

        classLikeDeclaration.configure {
            withArg("F", "FirClassLikeDeclaration<F>")
            parentArg(symbolOwner, "F", "F")
            +field("supertypesComputationStatus", type("fir.declarations", "SupertypesComputationStatus"), withReplace = true).apply { isMutable = true }
            +symbol("FirClassLikeSymbol", "F")
        }

        regularClass.configure {
            parentArg(classLikeDeclaration, "F", regularClass)
            +symbol("FirClassSymbol")
            +field("companionObject", regularClass, nullable = true)
            +superTypeRefs(withReplace = true)
        }

        sealedClass.configure {
            +fieldList("inheritors", classIdType, withReplace = true)
        }

        typeAlias.configure {
            parentArg(classLikeDeclaration, "F", typeAlias)
            +symbol("FirTypeAliasSymbol")
            +field("expandedTypeRef", typeRef, withReplace = true)
            +annotations
        }

        enumEntry.configure {
            +arguments.withTransform()
            +field(typeRef)
        }

        anonymousFunction.configure {
            parentArg(function, "F", anonymousFunction)
            +symbol("FirAnonymousFunctionSymbol")
            +field(label, nullable = true)
            +field(invocationKindType, nullable = true, withReplace = true).apply {
                isMutable = true
            }
        }

        typeParameter.configure {
            parentArg(symbolOwner, "F", typeParameter)
            +symbol("FirTypeParameterSymbol")
            +field(varianceType)
            +booleanField("isReified")
            +fieldList("bounds", typeRef)
            +annotations
        }

        memberFunction.configure {
            withArg("F", memberFunction)
            parentArg(function, "F", "F")
            parentArg(callableMemberDeclaration, "F", "F")
            +symbol("FirFunctionSymbol", "F")
            +annotations
        }

        simpleFunction.configure {
            parentArg(memberFunction, "F", simpleFunction)
        }

        property.configure {
            parentArg(variable, "F", property)
            parentArg(callableMemberDeclaration, "F", property)
            +symbol("FirPropertySymbol")
            +field("backingFieldSymbol", backingFieldSymbolType)
            +booleanField("isLocal")
            +typeParameters
            +status
        }

        propertyAccessor.configure {
            parentArg(function, "F", propertyAccessor)
            +symbol("FirPropertyAccessorSymbol")
            +booleanField("isGetter")
            +booleanField("isSetter")
            +status
            +annotations
        }

        declarationStatus.configure {
            +visibility
            +modality
            generateBooleanFields(
                "expect", "actual", "override", "operator", "infix", "inline", "tailRec",
                "external", "const", "lateInit", "inner", "companion", "data", "suspend", "static"
            )
        }

        resolvedDeclarationStatus.configure {
            shouldBeAnInterface()
        }

        constructor.configure {
            parentArg(memberFunction, "F", constructor)
            +symbol("FirConstructorSymbol")
            +field("delegatedConstructor", delegatedConstructorCall, nullable = true)
            +booleanField("isPrimary")
        }

        delegatedConstructorCall.configure {
            +field("constructedTypeRef", typeRef)
            generateBooleanFields("this", "super")
            +calleeReference.withTransform()
        }

        valueParameter.configure {
            parentArg(variable, "F", valueParameter)
            +field("defaultValue", expression, nullable = true)
            generateBooleanFields("crossinline", "noinline", "vararg")
        }

        variable.configure {
            withArg("F", variable)
            parentArg(callableDeclaration, "F", "F")
            +symbol("FirVariableSymbol", "F")
            +initializer
            +field("delegate", expression, nullable = true)
            +field("delegateFieldSymbol", delegateFieldSymbolType, "F", nullable = true)
            generateBooleanFields("var", "val")
            +field("getter", propertyAccessor, nullable = true).withTransform()
            +field("setter", propertyAccessor, nullable = true).withTransform()
            +annotations
            needTransformOtherChildren()
        }

        field.configure {
            parentArg(variable, "F", field)
            parentArg(callableMemberDeclaration, "F", field)
        }

        anonymousInitializer.configure {
            +body(nullable = true)
        }

        file.configure {
            +fieldList(import)
            +declarations
            +stringField("name")
            +field("packageFqName", fqNameType)
        }

        import.configure {
            +field("importedFqName", fqNameType, nullable = true)
            +booleanField("isAllUnder")
            +field("aliasName", nameType, nullable = true)
        }

        resolvedImport.configure {
            +field("delegate", import)
            +field("packageFqName", fqNameType)
            +field("relativeClassName", fqNameType, nullable = true)
            +field("resolvedClassId", classIdType, nullable = true)
            +field(
                "importedName",
                nameType,
                nullable = true
            )
        }

        annotationCall.configure {
            +field("useSiteTarget", annotationUseSiteTargetType, nullable = true)
            +field("annotationTypeRef", typeRef)
        }

        arraySetCall.configure {
            +field("rValue", expression).withTransform()
            +field("operation", operationType)
            +field("lValue", reference)
            +fieldList("indexes", expression).withTransform()
        }

        classReferenceExpression.configure {
            +field("classTypeRef", typeRef)
        }

        componentCall.configure {
            +field("explicitReceiver", expression)
            +intField("componentIndex")
        }

        errorExpression.configure {
            +stringField("reason")
        }

        expressionWithSmartcast.configure {
            +field("originalExpression", qualifiedAccessExpression)
            +field("typesFromSmartcast", "Collection<ConeKotlinType>", null, customType = coneKotlinTypeType)
            +field("originalType", typeRef)
        }

        getClassCall.configure {
            +field("argument", expression)
        }

        wrappedArgumentExpression.configure {
            +booleanField("isSpread")
        }

        namedArgumentExpression.configure {
            +name
        }

        resolvedQualifier.configure {
            +field("packageFqName", fqNameType)
            +field("relativeClassFqName", fqNameType, nullable = true)
            +field("classId", classIdType, nullable = true)
        }

        stringConcatenationCall.configure {
        }

        throwExpression.configure {
            +field("exception", expression)
        }

        variableAssignment.configure {
            +field("lValue", reference)
            +field("rValue", expression).withTransform()
            +field("operation", operationType)
        }

        whenSubjectExpression.configure {
            +field("whenSubject", whenSubjectType)
        }

        wrappedExpression.configure {
            +field(expression)
        }

        wrappedDelegateExpression.configure {
            +field("delegateProvider", expression)
        }

        namedReference.configure {
            +name
            +field("candidateSymbol", abstractFirBasedSymbolType, "*", nullable = true)
        }

        resolvedCallableReference.configure {
            +field("resolvedSymbol", abstractFirBasedSymbolType, "*")
        }

        delegateFieldReference.configure {
            +field("resolvedSymbol", delegateFieldSymbolType.withArgs("*"))
        }

        backingFieldReference.configure {
            +field("resolvedSymbol", backingFieldSymbolType)
        }

        superReference.configure {
            +field("superTypeRef", typeRef, withReplace = true)
        }

        thisReference.configure {
            +stringField("labelName", nullable = true)
            +field("boundSymbol", abstractFirBasedSymbolType, "*", nullable = true, withReplace = true)
        }

        typeRef.configure {
            +annotations
        }

        resolvedTypeRef.configure {
            +field("type", coneKotlinTypeType)
        }

        errorTypeRef.configure {
            +stringField("reason")
        }

        delegatedTypeRef.configure {
            +field("delegate", expression, nullable = true)
            +field(typeRef)
        }

        typeRefWithNullability.configure {
            +booleanField("isMarkedNullable")
        }

        userTypeRef.configure {
            +fieldList("qualifier", firQualifierPartType)
        }

        functionTypeRef.configure {
            +field("receiverTypeRef", typeRef, nullable = true)
            +valueParameters
            +returnTypeRef
        }

        thisReceiverExpression.configure {
            +field("calleeReference", thisReference)
        }

        whenExpression.configure {
            +field("subject", expression, nullable = true).withTransform()
            +field("subjectVariable", variable.withArgs("F" to "*"), nullable = true)
            +fieldList("branches", whenBranch).withTransform()
            +booleanField("isExhaustive", withReplace = true)
            needTransformOtherChildren()
        }

        typeProjectionWithVariance.configure {
            +field(typeRef)
            +field(varianceType)
        }

        errorNamedReference.configure {
            +stringField("errorReason")
        }
    }
}

private fun Element.withArgs(vararg replacements: Pair<String, String>): AbstractElement {
    val replaceMap = replacements.toMap()
    val newArguments = typeArguments.map { replaceMap[it.name]?.let { SimpleTypeArgument(it, null) } ?: it }
    return ElementWithArguments(this, newArguments)
}
