/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java.lazy.descriptors

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.load.java.components.TypeUsage
import org.jetbrains.kotlin.descriptors.impl.ConstructorDescriptorImpl
import java.util.Collections
import org.jetbrains.kotlin.utils.*
import java.util.ArrayList
import org.jetbrains.kotlin.load.java.lazy.types.toAttributes
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.descriptors.impl.EnumEntrySyntheticClassDescriptor
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.load.java.lazy.resolveAnnotations
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.descriptors.JavaConstructorDescriptor
import org.jetbrains.kotlin.load.java.components.DescriptorResolverUtils
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import java.util.LinkedHashSet

public class LazyJavaClassMemberScope(
        c: LazyJavaResolverContext,
        containingDeclaration: ClassDescriptor,
        private val jClass: JavaClass
) : LazyJavaMemberScope(c, containingDeclaration) {

    override fun computeMemberIndex(): MemberIndex {
        return object : ClassMemberIndex(jClass, { !it.isStatic() }) {
            // For SAM-constructors
            override fun getMethodNames(nameFilter: (Name) -> Boolean): Collection<Name>
                    = super.getMethodNames(nameFilter) + getClassNames(DescriptorKindFilter.CLASSIFIERS, nameFilter)
        }
    }

    internal val constructors = c.storageManager.createLazyValue {
        val constructors = jClass.getConstructors()
        val result = ArrayList<JavaConstructorDescriptor>(constructors.size())
        for (constructor in constructors) {
            val descriptor = resolveConstructor(constructor)
            result.add(descriptor)
            result.addIfNotNull(c.samConversionResolver.resolveSamAdapter(descriptor))
        }
        result ifEmpty { createDefaultConstructors() }
    }

    override fun computeNonDeclaredFunctions(result: MutableCollection<SimpleFunctionDescriptor>, name: Name) {
        val functionsFromSupertypes = getFunctionsFromSupertypes(name, getContainingDeclaration())
        result.addAll(DescriptorResolverUtils.resolveOverrides(name, functionsFromSupertypes, result, getContainingDeclaration(), c.errorReporter))
    }

    private fun getFunctionsFromSupertypes(name: Name, descriptor: ClassDescriptor): Set<SimpleFunctionDescriptor> {
          return descriptor.getTypeConstructor().getSupertypes().flatMap {
              it.getMemberScope().getFunctions(name).map { f -> f as SimpleFunctionDescriptor }
          }.toSet()
      }

    override fun computeNonDeclaredProperties(name: Name, result: MutableCollection<PropertyDescriptor>) {
        val propertiesFromSupertypes = getPropertiesFromSupertypes(name, getContainingDeclaration())

        result.addAll(DescriptorResolverUtils.resolveOverrides(name, propertiesFromSupertypes, result, getContainingDeclaration(),
                                                                   c.errorReporter))
    }

    private fun getPropertiesFromSupertypes(name: Name, descriptor: ClassDescriptor): Set<PropertyDescriptor> {
        return descriptor.getTypeConstructor().getSupertypes().flatMap {
            it.getMemberScope().getProperties(name).map { p -> p as PropertyDescriptor }
        }.toSet()
    }

    override fun resolveMethodSignature(
            method: JavaMethod, methodTypeParameters: List<TypeParameterDescriptor>, returnType: JetType,
            valueParameters: LazyJavaMemberScope.ResolvedValueParameters
    ): LazyJavaMemberScope.MethodSignatureData {
        val propagated = c.externalSignatureResolver.resolvePropagatedSignature(
                method, getContainingDeclaration(), returnType, null, valueParameters.descriptors, methodTypeParameters)
        val superFunctions = propagated.getSuperMethods()
        val effectiveSignature = c.externalSignatureResolver.resolveAlternativeMethodSignature(
                method, !superFunctions.isEmpty(), propagated.getReturnType(),
                propagated.getReceiverType(), propagated.getValueParameters(), propagated.getTypeParameters(),
                propagated.hasStableParameterNames())

        return LazyJavaMemberScope.MethodSignatureData(effectiveSignature, superFunctions, propagated.getErrors() + effectiveSignature.getErrors())
    }

    private fun resolveConstructor(constructor: JavaConstructor): JavaConstructorDescriptor {
        val classDescriptor = getContainingDeclaration()

        val constructorDescriptor = JavaConstructorDescriptor.createJavaConstructor(
                classDescriptor, c.resolveAnnotations(constructor), false, c.sourceElementFactory.source(constructor),
                CallableMemberDescriptor.Kind.DECLARATION
        )

        val valueParameters = resolveValueParameters(c, constructorDescriptor, constructor.getValueParameters())
        val effectiveSignature = c.externalSignatureResolver.resolveAlternativeMethodSignature(
                constructor, false, null, null, valueParameters.descriptors, Collections.emptyList(), false)

        constructorDescriptor.initialize(
                classDescriptor.getTypeConstructor().getParameters(),
                effectiveSignature.getValueParameters(),
                constructor.getVisibility()
        )
        constructorDescriptor.setHasStableParameterNames(effectiveSignature.hasStableParameterNames())
        constructorDescriptor.setHasSynthesizedParameterNames(valueParameters.hasSynthesizedNames)

        constructorDescriptor.setReturnType(classDescriptor.getDefaultType())

        val signatureErrors = effectiveSignature.getErrors()
        if (!signatureErrors.isEmpty()) {
            c.externalSignatureResolver.reportSignatureErrors(constructorDescriptor, signatureErrors)
        }

        c.javaResolverCache.recordConstructor(constructor, constructorDescriptor)

        return constructorDescriptor
    }

    private fun createDefaultConstructors(): List<ConstructorDescriptor> {
        val isAnnotation = jClass.isAnnotationType()
        if (jClass.isInterface() && !isAnnotation)
            return emptyList()

        val defaultConstructor = createDefaultConstructor(true)

        val additionalConstructor =
            if (isAnnotation && defaultConstructor.getValueParameters().any { it.getType().isKClassOrArray() })
                createDefaultConstructor(false)
            else null

        return listOf(defaultConstructor) + additionalConstructor.singletonOrEmptyList()
    }

    private fun JetType.isKClassOrArray() = isKClass() || (KotlinBuiltIns.isArray(this) && getArguments().first().getType().isKClass())
    private fun JetType.isKClass() =
            (getConstructor().getDeclarationDescriptor() as? ClassDescriptor)?.let { KotlinBuiltIns.isKClass(it) } ?: false

    private fun createDefaultConstructor(isPrimary: Boolean): JavaConstructorDescriptor {
        val isAnnotation = jClass.isAnnotationType()

        val classDescriptor = getContainingDeclaration()
        val constructorDescriptor = JavaConstructorDescriptor.createJavaConstructor(
                classDescriptor, Annotations.EMPTY, isPrimary, c.sourceElementFactory.source(jClass),
                if (isPrimary) CallableMemberDescriptor.Kind.DECLARATION else CallableMemberDescriptor.Kind.SYNTHESIZED
        )
        val typeParameters = classDescriptor.getTypeConstructor().getParameters()
        val valueParameters = if (isAnnotation) createAnnotationConstructorParameters(constructorDescriptor, isPrimary)
                              else Collections.emptyList<ValueParameterDescriptor>()

        constructorDescriptor.setHasSynthesizedParameterNames(false)

        constructorDescriptor.initialize(typeParameters, valueParameters, getConstructorVisibility(classDescriptor))
        constructorDescriptor.setHasStableParameterNames(true)
        constructorDescriptor.setReturnType(classDescriptor.getDefaultType())
        if (isPrimary) {
            c.javaResolverCache.recordConstructor(jClass, constructorDescriptor)
        }
        return constructorDescriptor
    }

    private fun getConstructorVisibility(classDescriptor: ClassDescriptor): Visibility {
        val visibility = classDescriptor.getVisibility()
        if (visibility == JavaVisibilities.PROTECTED_STATIC_VISIBILITY) {
            return JavaVisibilities.PROTECTED_AND_PACKAGE
        }
        return visibility
    }

    private fun createAnnotationConstructorParameters(
            constructor: ConstructorDescriptorImpl,
            loadJavaClassAsKClass: Boolean
    ): List<ValueParameterDescriptor> {
        val methods = jClass.getMethods()
        val result = ArrayList<ValueParameterDescriptor>(methods.size())

        // Using MEMBER_SIGNATURE_CONTRAVARIANT is just a hack to make overload resolution work in cases like
        // Ann(arg = array(javaClass<Int>())) class Annotated
        //
        // If we load constructor parameters' types as invariant: Ann(arg: Array<Class<*>>), Ann(arg: Array<KClass<*>>), then
        // when resolving `Ann(arg = array(javaClass<Int>()))` type of array is inferred as Array<Class<Int>>
        // which is neither subtype of Array<Class<*>> nor Array<KClass<*>> (similar case is KT-7410)
        //
        // Hack should be removed if support of Class<*> in annotations is dropped or KT-7410 is fixed
        // Also see tests `AnnotationsWithClassParameterOverload`
        val attr = TypeUsage.MEMBER_SIGNATURE_CONTRAVARIANT.toAttributes(
                allowFlexible = false, isForAnnotationParameter = loadJavaClassAsKClass)

        val (methodsNamedValue, otherMethods) = methods.
                partition { it.getName() == JvmAnnotationNames.DEFAULT_ANNOTATION_MEMBER_NAME }

        assert(methodsNamedValue.size() <= 1, "There can't be to methods named 'value' in annotation class: " + jClass)
        val methodNamedValue = methodsNamedValue.firstOrNull()
        if (methodNamedValue != null) {
            val parameterNamedValueJavaType = methodNamedValue.getAnnotationMethodReturnJavaType()
            val (parameterType, varargType) =
                    if (parameterNamedValueJavaType is JavaArrayType)
                        Pair(c.typeResolver.transformArrayType(parameterNamedValueJavaType, attr, isVararg = true),
                             c.typeResolver.transformJavaType(parameterNamedValueJavaType.getComponentType(), attr))
                    else
                        Pair(c.typeResolver.transformJavaType(parameterNamedValueJavaType, attr), null)

            result.addAnnotationValueParameter(constructor, 0, methodNamedValue, parameterType, varargType)
        }

        val startIndex = if (methodNamedValue != null) 1 else 0
        for ((index, method) in otherMethods.withIndex()) {
            val parameterType = c.typeResolver.transformJavaType(method.getAnnotationMethodReturnJavaType(), attr)
            result.addAnnotationValueParameter(constructor, index + startIndex, method, parameterType, null)
        }

        return result
    }

    private fun JavaMethod.getAnnotationMethodReturnJavaType(): JavaType {
        assert(getValueParameters().isEmpty(), "Annotation method can't have parameters: " + this)
        return getReturnType() ?: throw AssertionError("Annotation method has no return type: " + this)
    }

    private fun MutableList<ValueParameterDescriptor>.addAnnotationValueParameter(
            constructor: ConstructorDescriptor,
            index: Int,
            method: JavaMethod,
            returnType: JetType,
            varargElementType: JetType?
    ) {
        add(ValueParameterDescriptorImpl(
                constructor,
                null,
                index,
                Annotations.EMPTY,
                method.getName(),
                // Parameters of annotation constructors in Java are never nullable
                TypeUtils.makeNotNullable(returnType),
                method.hasAnnotationParameterDefaultValue(),
                // Nulls are not allowed in annotation arguments in Java
                varargElementType?.let { TypeUtils.makeNotNullable(it) },
                c.sourceElementFactory.source(method)
        ))
    }

    private val nestedClassIndex = c.storageManager.createLazyValue {
        jClass.getInnerClasses().valuesToMap { c -> c.getName() }
    }

    private val enumEntryIndex = c.storageManager.createLazyValue {
        jClass.getFields().filter { it.isEnumEntry() }.valuesToMap { f -> f.getName() }
    }

    private val nestedClasses = c.storageManager.createMemoizedFunctionWithNullableValues {
        name: Name ->
        val jNestedClass = nestedClassIndex()[name]
        if (jNestedClass == null) {
            val field = enumEntryIndex()[name]
            if (field != null) {
                EnumEntrySyntheticClassDescriptor.create(c.storageManager, getContainingDeclaration(), name,
                                                         c.storageManager.createLazyValue {
                                                             memberIndex().getAllFieldNames() + memberIndex().getMethodNames({true})
                                                         }, c.sourceElementFactory.source(field))
            }
            else null
        }
        else {
            LazyJavaClassDescriptor(
                    c, getContainingDeclaration(), DescriptorUtils.getFqName(getContainingDeclaration()).child(name).toSafe(), jNestedClass
            )
        }
    }

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? =
            DescriptorUtils.getDispatchReceiverParameterIfNeeded(getContainingDeclaration())

    override fun getClassifier(name: Name): ClassifierDescriptor? = nestedClasses(name)

    override fun getClassNames(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<Name>
            = nestedClassIndex().keySet() + enumEntryIndex().keySet()

    override fun getPropertyNames(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<Name> =
            memberIndex().getAllFieldNames() +
            getContainingDeclaration().getTypeConstructor().getSupertypes().flatMapTo(LinkedHashSet<Name>()) { supertype ->
                supertype.getMemberScope().getDescriptors(kindFilter, nameFilter).map { variable ->
                    variable.getName()
                }
            }

    // TODO
    override fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> = listOf()


    override fun getContainingDeclaration() = super.getContainingDeclaration() as ClassDescriptor

    // namespaces should be resolved elsewhere
    override fun getPackage(name: Name) = null

    override fun toString() = "Lazy java member scope for " + jClass.getFqName()
}
