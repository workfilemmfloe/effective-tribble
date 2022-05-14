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

package org.jetbrains.kotlin.js.translate.declaration

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.bridges.Bridge
import org.jetbrains.kotlin.backend.common.bridges.generateBridgesForFunctionDescriptor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.descriptorUtils.hasPrimaryConstructor
import org.jetbrains.kotlin.js.translate.callTranslator.CallTranslator
import org.jetbrains.kotlin.js.translate.context.*
import org.jetbrains.kotlin.js.translate.expression.translateAndAliasParameters
import org.jetbrains.kotlin.js.translate.expression.translateFunction
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator
import org.jetbrains.kotlin.js.translate.initializer.ClassInitializerTranslator
import org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator
import org.jetbrains.kotlin.js.translate.utils.*
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.getClassDescriptor
import org.jetbrains.kotlin.js.translate.utils.BindingUtils.getPropertyDescriptorForConstructorParameter
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.pureFqn
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getSupertypesWithoutFakes
import org.jetbrains.kotlin.js.translate.utils.PsiUtils.getPrimaryConstructorParameters
import org.jetbrains.kotlin.js.translate.utils.jsAstUtils.toInvocationWith
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.getClassDescriptorForType
import org.jetbrains.kotlin.resolve.DescriptorUtils.getClassDescriptorForTypeConstructor
import org.jetbrains.kotlin.types.CommonSupertypes.topologicallySortSuperclassesAndRecordAllInstances
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.identity

/**
 * Generates a definition of a single class.
 */
class ClassTranslator private constructor(
        private val classDeclaration: KtClassOrObject,
        context: TranslationContext,
        private val enumInitializerName: JsName?,
        private val ordinal: Int?
) : AbstractTranslator(context) {

    private val descriptor = getClassDescriptor(context.bindingContext(), classDeclaration)
    private val secondaryConstructors = mutableListOf<ConstructorInfo>()
    private var primaryConstructor: ConstructorInfo? = null
    private lateinit var cachedInstanceName: JsName
    private val metadataLiteral = JsObjectLiteral(true)

    private fun isTrait(): Boolean = descriptor.kind == ClassKind.INTERFACE

    private fun isAnnotation(): Boolean = descriptor.kind == ClassKind.ANNOTATION_CLASS

    private fun translate() {
        val scope = context().getScopeForDescriptor(descriptor)
        val context = context().newDeclaration(descriptor)

        val constructorFunction = descriptor.unsubstitutedPrimaryConstructor?.let { context.getFunctionObject(it) } ?:
                                  context.createRootScopedFunction(descriptor)
        constructorFunction.name = context.getInnerNameForDescriptor(descriptor)
        context.addDeclarationStatement(constructorFunction.makeStmt())
        val enumInitFunction = if (descriptor.kind == ClassKind.ENUM_CLASS) createEnumInitFunction() else null

        val nonConstructorContext = context.withUsageTrackerIfNecessary(scope, descriptor)
        nonConstructorContext.startDeclaration()
        val delegationTranslator = DelegationTranslator(classDeclaration, nonConstructorContext)
        translatePropertiesAsConstructorParameters(nonConstructorContext)
        val bodyVisitor = DeclarationBodyVisitor(descriptor, nonConstructorContext, enumInitFunction)
        bodyVisitor.traverseContainer(classDeclaration, nonConstructorContext)
        mayBeAddThrowableProperties(context)
        constructorFunction.body.statements += bodyVisitor.initializerStatements
        delegationTranslator.generateDelegated()

        translatePrimaryConstructor(constructorFunction, context, delegationTranslator)
        addMetadataObject()
        addMetadataType()
        context.addClass(descriptor)
        addSuperclassReferences()
        classDeclaration.secondaryConstructors.forEach { generateSecondaryConstructor(context, it) }

        generatedBridgeMethods()

        if (descriptor.isData) {
            JsDataClassGenerator(classDeclaration, context).generate()
        }

        emitConstructors(nonConstructorContext, nonConstructorContext.endDeclaration())
        for (constructor in allConstructors) {
            addClosureParameters(constructor, nonConstructorContext)
        }

        if (isObjectLike()) {
            addObjectMethods()
        }

        if (descriptor.kind == ClassKind.ENUM_CLASS) {
            generateEnumStandardMethods(bodyVisitor.enumEntries)
        }
    }

    private fun TranslationContext.withUsageTrackerIfNecessary(scope: JsScope, innerDescriptor: MemberDescriptor): TranslationContext {
        return if (isLocalClass) {
            innerWithUsageTracker(scope, innerDescriptor)
        }
        else {
            inner(innerDescriptor)
        }
    }

    private val isLocalClass
        get() = descriptor.containingDeclaration !is ClassOrPackageFragmentDescriptor

    private fun translatePrimaryConstructor(
            constructorFunction: JsFunction,
            classContext: TranslationContext,
            delegationTranslator: DelegationTranslator
    ) {
        if (!isTrait()) {
            val constructorContext = classContext.innerWithUsageTracker(constructorFunction.scope, descriptor)
            if (isObjectLike()) {
                addObjectCache(constructorFunction.body.statements)
            }
            ClassInitializerTranslator(classDeclaration, constructorContext, constructorFunction).apply {
                if (ordinal != null) {
                    setOrdinal(ordinal)
                }
                generateInitializeMethod(delegationTranslator)
            }

            primaryConstructor = ConstructorInfo(constructorFunction, constructorContext, descriptor)
        }
    }

    private fun createEnumInitFunction(): JsFunction {
        val function = context().createRootScopedFunction(descriptor)
        function.name = context().createGlobalName(StaticContext.getSuggestedName(descriptor) + "_initFields")
        val emptyFunction = context().createRootScopedFunction(descriptor)
        function.body.statements += JsAstUtils.assignment(JsAstUtils.pureFqn(function.name, null), emptyFunction).makeStmt()
        context().addDeclarationStatement(function.makeStmt())
        return function
    }

    private fun isObjectLike() = when (descriptor.kind) {
        ClassKind.OBJECT,
        ClassKind.ENUM_ENTRY -> true
        else -> false
    }

    private fun addMetadataObject() {
        context().addDeclarationStatement(JsAstUtils.assignment(createMetadataRef(), metadataLiteral).makeStmt())
    }

    private fun createMetadataRef() = JsNameRef(Namer.METADATA, context().getInnerReference(descriptor))

    private fun addMetadataType() {
        val kotlinType = JsNameRef(Namer.CLASS_KIND_ENUM, Namer.KOTLIN_NAME)
        val typeRef = when {
            DescriptorUtils.isInterface(descriptor) -> JsNameRef(Namer.CLASS_KIND_INTERFACE, kotlinType)
            DescriptorUtils.isObject(descriptor) -> JsNameRef(Namer.CLASS_KIND_OBJECT, kotlinType)
            else -> JsNameRef(Namer.CLASS_KIND_CLASS, kotlinType)
        }

        metadataLiteral.propertyInitializers += JsPropertyInitializer(JsNameRef(Namer.METADATA_CLASS_KIND), typeRef)

        val simpleName = descriptor.name
        if (!simpleName.isSpecial) {
            val simpleNameProp = JsPropertyInitializer(JsNameRef(Namer.METADATA_SIMPLE_NAME), program().getStringLiteral(simpleName.identifier))
            metadataLiteral.propertyInitializers += simpleNameProp
        }
    }

    private fun generateSecondaryConstructor(classContext: TranslationContext, constructor: KtSecondaryConstructor) {
        // Prepare
        val constructorDescriptor = BindingUtils.getDescriptorForElement(classContext.bindingContext(), constructor)
                as ClassConstructorDescriptor
        val classDescriptor = constructorDescriptor.containingDeclaration

        val constructorScope = classContext.getScopeForDescriptor(constructorDescriptor)

        val thisName = constructorScope.declareName(Namer.ANOTHER_THIS_PARAMETER_NAME)
        val thisNameRef = thisName.makeRef()
        val receiverDescriptor = classDescriptor.thisAsReceiverParameter

        var context = classContext
                .innerWithUsageTracker(constructorScope, constructorDescriptor)
                .innerContextWithAliased(receiverDescriptor, thisNameRef)

        val outerClassName = context.getOuterClassReference(classDescriptor)
        val outerClass = DescriptorUtils.getContainingClass(classDescriptor)
        if (outerClassName != null) {
            val outerClassReceiver = outerClass!!.thisAsReceiverParameter
            context = context.innerContextWithAliased(outerClassReceiver, outerClassName.makeRef())
        }

        // Translate constructor body
        val constructorInitializer = context.getFunctionObject(constructorDescriptor)
        constructorInitializer.name = context.getInnerNameForDescriptor(constructorDescriptor)
        context.addDeclarationStatement(constructorInitializer.makeStmt())
        context.translateAndAliasParameters(constructorDescriptor, constructorInitializer.parameters)
                .translateFunction(constructor, constructorInitializer)

        // Translate super/this call
        val superCallGenerators = mutableListOf<(MutableList<JsStatement>) -> Unit>()
        val referenceToClass = context.getInnerReference(classDescriptor)
        context = context.contextWithScope(constructorInitializer)

        superCallGenerators += { it += FunctionBodyTranslator.setDefaultValueForArguments(constructorDescriptor, context) }

        val createInstance = Namer.createObjectWithPrototypeFrom(referenceToClass)
        val instanceVar = JsAstUtils.assignment(thisNameRef, JsAstUtils.or(thisNameRef, createInstance)).makeStmt()
        superCallGenerators += { it += instanceVar }

        // Add parameter for outer instance
        val leadingArgs = mutableListOf<JsExpression>()

        if (descriptor.kind == ClassKind.ENUM_CLASS) {
            val nameParamName = constructorInitializer.scope.declareTemporaryName("name")
            val ordinalParamName = constructorInitializer.scope.declareTemporaryName("ordinal")
            constructorInitializer.parameters.addAll(0, listOf(JsParameter(nameParamName), JsParameter(ordinalParamName)))
            leadingArgs += listOf(nameParamName.makeRef(), ordinalParamName.makeRef())
        }
        if (outerClassName != null) {
            constructorInitializer.parameters.add(0, JsParameter(outerClassName))
            leadingArgs += outerClassName.makeRef()
        }

        constructorInitializer.parameters += JsParameter(thisName)

        // Generate super/this call to insert to beginning of the function
        val resolvedCall = BindingContextUtils.getDelegationConstructorCall(context.bindingContext(), constructorDescriptor)
        val delegationClassDescriptor = (resolvedCall?.resultingDescriptor as? ClassConstructorDescriptor)?.constructedClass

        if (resolvedCall != null && !KotlinBuiltIns.isAny(delegationClassDescriptor!!)) {
            if (JsDescriptorUtils.isImmediateSubtypeOfError(classDescriptor)) {
                superCallGenerators += {
                    val innerContext = context().innerBlock()
                    ClassInitializerTranslator.emulateSuperCallToNativeError(
                            innerContext, classDescriptor, resolvedCall, thisNameRef.deepCopy())
                    it += innerContext.currentBlock.statements
                }
            }
            else {
                superCallGenerators += {
                    val delegationConstructor = resolvedCall.resultingDescriptor
                    val innerContext = context.innerBlock()
                    val statement = CallTranslator.translate(innerContext, resolvedCall)
                            .toInvocationWith(leadingArgs, delegationConstructor.valueParameters.size, thisNameRef).makeStmt()
                    it += innerContext.currentBlock.statements
                    it += statement
                }
            }
        }

        val delegationCtorInTheSameClass = delegationClassDescriptor == classDescriptor
        if (!delegationCtorInTheSameClass && !classDescriptor.hasPrimaryConstructor()) {
            superCallGenerators += {
                val usageTracker = context.usageTracker()!!
                val closure = context.getClassOrConstructorClosure(classDescriptor).orEmpty().map {
                    usageTracker.getNameForCapturedDescriptor(it)!!.makeRef()
                }
                it += JsInvocation(Namer.getFunctionCallRef(referenceToClass), listOf(thisNameRef) + closure + leadingArgs).makeStmt()
            }
        }

        constructorInitializer.body.statements += JsReturn(thisNameRef)

        val compositeSuperCallGenerator: () -> Unit = {
            val additionalStatements = mutableListOf<JsStatement>()
            for (partGenerator in superCallGenerators) {
                partGenerator(additionalStatements)
            }
            constructorInitializer.body.statements.addAll(0, additionalStatements)
        }

        secondaryConstructors += ConstructorInfo(constructorInitializer, context, constructorDescriptor, compositeSuperCallGenerator)

        context.export(constructorDescriptor)
    }

    private val allConstructors: Sequence<ConstructorInfo>
        get() {
            val primary = primaryConstructor
            return if (primary != null) sequenceOf(primary) + secondaryConstructors else secondaryConstructors.asSequence()
        }

    private fun emitConstructors(nonConstructorContext: TranslationContext, callSites: List<DeferredCallSite>) {
        // Build map that maps constructor to all constructors called via this()
        val constructorMap = allConstructors.map { it.descriptor to it }.toMap()

        val callSiteMap = callSites.groupBy {
            val constructor =  it.constructor
            if (constructor.isPrimary) constructor.containingDeclaration else constructor
        }

        val thisCalls = secondaryConstructors.map {
            val set = mutableSetOf<ConstructorInfo>()
            val descriptor = it.descriptor
            if (descriptor is ConstructorDescriptor) {
                val resolvedCall = BindingContextUtils.getDelegationConstructorCall(context().bindingContext(), descriptor)
                if (resolvedCall != null) {
                    val callee = constructorMap[resolvedCall.resultingDescriptor]
                    if (callee != null) {
                        set += callee
                    }
                }
            }
            Pair(it, set)
        }.toMap()

        val sortedConstructors = DFS.topologicalOrder(allConstructors.asIterable()) { thisCalls[it].orEmpty() }.reversed()
        for (constructor in sortedConstructors) {
            constructor.superCallGenerator()

            val nonConstructorUsageTracker = nonConstructorContext.usageTracker()
            val usageTracker = constructor.context.usageTracker()!!

            val nonConstructorCapturedVars = if (isLocalClass) nonConstructorUsageTracker!!.capturedDescriptors else setOf()
            val constructorCapturedVars = usageTracker.capturedDescriptors

            val capturedVars = (nonConstructorCapturedVars + constructorCapturedVars).distinct()

            val descriptor = constructor.descriptor
            val classDescriptor = DescriptorUtils.getParentOfType(descriptor, ClassDescriptor::class.java, false)!!
            nonConstructorContext.putClassOrConstructorClosure(descriptor, capturedVars)

            val constructorCallSites = callSiteMap[constructor.descriptor].orEmpty()

            for (callSite in constructorCallSites) {
                val closureQualifier = callSite.context.getArgumentForClosureConstructor(classDescriptor.thisAsReceiverParameter)
                capturedVars.forEach { nonConstructorUsageTracker!!.used(it) }
                val closureArgs = capturedVars.map {
                    val name = nonConstructorUsageTracker!!.getNameForCapturedDescriptor(it)!!
                    JsAstUtils.pureFqn(name, closureQualifier)
                }
                callSite.invocationArgs.addAll(0, closureArgs)
            }
        }
    }

    private fun addClosureParameters(constructor: ConstructorInfo, nonConstructorContext: TranslationContext) {
        val usageTracker = constructor.context.usageTracker()!!
        val capturedVars = context().getClassOrConstructorClosure(constructor.descriptor) ?: return
        val nonConstructorUsageTracker = nonConstructorContext.usageTracker()

        val function = constructor.function
        val additionalStatements = mutableListOf<JsStatement>()
        for ((i, capturedVar) in capturedVars.withIndex()) {
            val fieldName = nonConstructorUsageTracker?.capturedDescriptorToJsName?.get(capturedVar)
            val name = usageTracker.capturedDescriptorToJsName[capturedVar] ?: fieldName!!

            function.parameters.add(i, JsParameter(name))
            if (fieldName != null && constructor == primaryConstructor) {
                additionalStatements += JsAstUtils.defineSimpleProperty(fieldName.ident, name.makeRef())
            }
        }

        function.body.statements.addAll(0, additionalStatements)
    }

    private fun addSuperclassReferences() {
        val supertypeReferences = JsArrayLiteral(getSupertypesNameReferences())
        metadataLiteral.propertyInitializers += JsPropertyInitializer(JsNameRef(Namer.METADATA_SUPERTYPES), supertypeReferences)
    }

    private fun getSupertypesNameReferences(): List<JsExpression> {
        val supertypes = getSupertypesWithoutFakes(descriptor)
                .filter { it.constructor.declarationDescriptor !is FunctionClassDescriptor }
        if (supertypes.isEmpty()) {
            return emptyList()
        }
        if (supertypes.size == 1) {
            val type = supertypes[0]
            val supertypeDescriptor = getClassDescriptorForType(type)
            return if (!AnnotationsUtils.isNativeObject(supertypeDescriptor)) {
                listOf(ReferenceTranslator.translateAsTypeReference(supertypeDescriptor, context()))
            }
            else {
                listOf()
            }
        }

        val supertypeConstructors = mutableSetOf<TypeConstructor>()
        for (type in supertypes) {
            supertypeConstructors += type.constructor
        }
        val sortedAllSuperTypes = topologicallySortSuperclassesAndRecordAllInstances(
            descriptor.defaultType,
            mutableMapOf<TypeConstructor, Set<SimpleType>>(),
            mutableSetOf<TypeConstructor>()
        )
        val supertypesRefs = mutableListOf<JsExpression>()
        for (typeConstructor in sortedAllSuperTypes) {
            if (supertypeConstructors.contains(typeConstructor)) {
                val supertypeDescriptor = getClassDescriptorForTypeConstructor(typeConstructor)
                if (!AnnotationsUtils.isNativeObject(supertypeDescriptor)) {
                    supertypesRefs += ReferenceTranslator.translateAsTypeReference(supertypeDescriptor, context())
                }
            }
        }
        return supertypesRefs
    }

    private fun translatePropertiesAsConstructorParameters(classDeclarationContext: TranslationContext) {
        for (parameter in getPrimaryConstructorParameters(classDeclaration)) {
            val descriptor = getPropertyDescriptorForConstructorParameter(bindingContext(), parameter)
            if (descriptor != null) {
                val literal = JsObjectLiteral(true)
                translateAccessors(descriptor, literal.propertyInitializers, classDeclarationContext)
                if (literal.propertyInitializers.isNotEmpty()) {
                    classDeclarationContext.addAccessorsToPrototype(this.descriptor, descriptor, literal)
                }
            }
        }
    }

    private fun addObjectCache(statements: MutableList<JsStatement>) {
        cachedInstanceName = context().createGlobalName(StaticContext.getSuggestedName(descriptor) + Namer.OBJECT_INSTANCE_VAR_SUFFIX)
        statements += JsAstUtils.assignment(cachedInstanceName.makeRef(), JsObjectLiteral.THIS).makeStmt()
    }

    private fun addObjectMethods() {
        context().addDeclarationStatement(JsAstUtils.newVar(cachedInstanceName, JsLiteral.NULL))

        val instanceFun = context().createRootScopedFunction("Instance function: " + descriptor)
        instanceFun.name = context().getNameForObjectInstance(descriptor)

        if (enumInitializerName == null) {
            val instanceCreatedCondition = JsAstUtils.equality(cachedInstanceName.makeRef(), JsLiteral.NULL)
            val instanceCreationBlock = JsBlock()
            val instanceCreatedGuard = JsIf(instanceCreatedCondition, instanceCreationBlock)
            instanceFun.body.statements += instanceCreatedGuard

            val objectRef = context().getInnerReference(descriptor)
            instanceCreationBlock.statements += JsAstUtils.assignment(cachedInstanceName.makeRef(), JsNew(objectRef)).makeStmt()
        }
        else {
            instanceFun.body.statements += JsInvocation(pureFqn(enumInitializerName, null)).makeStmt()
        }

        instanceFun.body.statements += JsReturn(cachedInstanceName.makeRef())

        context().addDeclarationStatement(instanceFun.makeStmt())
    }

    private fun generateEnumStandardMethods(entries: List<ClassDescriptor>) {
        EnumTranslator(context(), descriptor, entries).generateStandardMethods()
    }

    private fun mayBeAddThrowableProperties(context: TranslationContext) {
        if (!JsDescriptorUtils.isImmediateSubtypeOfError(descriptor)) return

        val properties = listOf("message", "cause")
                .map { Name.identifier(it) }
                .map { DescriptorUtils.getPropertyByName(descriptor.unsubstitutedMemberScope, it) }
                .filter { !it.kind.isReal }
        for (property in properties) {
            val propertyTranslator = DefaultPropertyTranslator(property, context, JsLiteral.NULL)
            val literal = JsObjectLiteral(true)
            val getterFunction = context.getFunctionObject(property.getter!!)
            propertyTranslator.generateDefaultGetterFunction(property.getter!!, getterFunction)
            literal.propertyInitializers += JsPropertyInitializer(context.program().getStringLiteral("get"), getterFunction)
            context.addAccessorsToPrototype(descriptor, property, literal)
        }
    }

    private fun generatedBridgeMethods() {
        if (isAnnotation()) return

        generateBridgesToTraitImpl()

        generateOtherBridges()
    }

    private fun generateBridgesToTraitImpl() {
        for ((key, value) in CodegenUtil.getNonPrivateTraitMethods(descriptor)) {
            if (!areNamesEqual(key, value)) {
                generateDelegateCall(descriptor, value, key, JsLiteral.THIS, context())
            }
        }
    }

    private fun generateOtherBridges() {
        for (memberDescriptor in descriptor.defaultType.memberScope.getContributedDescriptors()) {
            if (memberDescriptor is FunctionDescriptor) {
                val bridgesToGenerate = generateBridgesForFunctionDescriptor(memberDescriptor, identity()) {
                    //There is no DefaultImpls in js backend so if method non-abstract it should be recognized as non-abstract on bridges calculation
                    false
                }

                for (bridge in bridgesToGenerate) {
                    generateBridge(bridge)
                }
            }
        }
    }

    private fun generateBridge(bridge: Bridge<FunctionDescriptor>) {
        val fromDescriptor = bridge.from
        val toDescriptor = bridge.to
        if (areNamesEqual(fromDescriptor, toDescriptor)) return

        if (fromDescriptor.kind.isReal && fromDescriptor.modality != Modality.ABSTRACT && !toDescriptor.kind.isReal)
            return

        generateDelegateCall(descriptor, fromDescriptor, toDescriptor, JsLiteral.THIS, context())
    }

    private fun areNamesEqual(first: FunctionDescriptor, second: FunctionDescriptor): Boolean {
        val firstName = context().getNameForDescriptor(first)
        val secondName = context().getNameForDescriptor(second)
        return firstName.ident == secondName.ident
    }

    companion object {
        @JvmStatic fun translate(classDeclaration: KtClassOrObject, context: TranslationContext) {
            return ClassTranslator(classDeclaration, context, null, null).translate()
        }

        @JvmStatic fun translate(classDeclaration: KtEnumEntry, context: TranslationContext, enumInitializerName: JsName, ordinal: Int) {
            return ClassTranslator(classDeclaration, context, enumInitializerName, ordinal).translate()
        }
    }

    private class ConstructorInfo(
            val function: JsFunction,
            val context: TranslationContext,
            val descriptor: MemberDescriptor,
            val superCallGenerator: (() -> Unit) = { }
    )
}
