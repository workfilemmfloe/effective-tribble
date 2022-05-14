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

package org.jetbrains.kotlin.serialization.deserialization.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AbstractClassDescriptor
import org.jetbrains.kotlin.descriptors.impl.EnumEntrySyntheticClassDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.KtScope
import org.jetbrains.kotlin.resolve.scopes.StaticScopeForKotlinClass
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.types.AbstractClassTypeConstructor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.upperIfFlexible
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.singletonOrEmptyList
import org.jetbrains.kotlin.utils.toReadOnlyList
import java.util.*

public class DeserializedClassDescriptor(
        outerContext: DeserializationContext,
        val classProto: ProtoBuf.Class,
        nameResolver: NameResolver,
        private val sourceElement: SourceElement
) : ClassDescriptor, AbstractClassDescriptor(
        outerContext.storageManager,
        nameResolver.getClassId(classProto.getFqName()).getShortClassName()
) {
    private val modality = Deserialization.modality(Flags.MODALITY.get(classProto.getFlags()))
    private val visibility = Deserialization.visibility(Flags.VISIBILITY.get(classProto.getFlags()))
    private val kindFromProto = Flags.CLASS_KIND.get(classProto.getFlags())
    private val kind = Deserialization.classKind(kindFromProto)
    private val isCompanion = kindFromProto == ProtoBuf.Class.Kind.COMPANION_OBJECT
    private val isInner = Flags.IS_INNER.get(classProto.getFlags())
    private val isData = Flags.IS_DATA.get(classProto.flags)

    val c = outerContext.childContext(this, classProto.typeParameterList, nameResolver, TypeTable(classProto.typeTable))

    private val classId = nameResolver.getClassId(classProto.getFqName())

    private val staticScope = StaticScopeForKotlinClass(this)
    private val typeConstructor = DeserializedClassTypeConstructor()
    private val memberScope = DeserializedClassMemberScope()
    private val nestedClasses = NestedClassDescriptors()
    private val enumEntries = EnumEntryClassDescriptors()

    private val containingDeclaration = outerContext.containingDeclaration
    private val primaryConstructor = c.storageManager.createNullableLazyValue { computePrimaryConstructor() }
    private val constructors = c.storageManager.createLazyValue { computeConstructors() }
    private val companionObjectDescriptor = c.storageManager.createNullableLazyValue { computeCompanionObjectDescriptor() }

    private val annotations =
            if (!Flags.HAS_ANNOTATIONS.get(classProto.getFlags())) {
                Annotations.EMPTY
            }
            else DeserializedAnnotations(c.storageManager) {
                c.components.annotationAndConstantLoader.loadClassAnnotations(classProto, c.nameResolver)
            }

    override fun getContainingDeclaration(): DeclarationDescriptor = containingDeclaration

    override fun getTypeConstructor() = typeConstructor

    override fun getKind() = kind

    override fun getModality() = modality

    override fun getVisibility() = visibility

    override fun isInner() = isInner

    override fun isData() = isData

    override fun getAnnotations() = annotations

    override fun getUnsubstitutedMemberScope() = memberScope

    override fun getStaticScope() = staticScope

    override fun isCompanionObject(): Boolean = isCompanion

    private fun computePrimaryConstructor(): ConstructorDescriptor? {
        if (kind.isSingleton) {
            return DescriptorFactory.createPrimaryConstructorForObject(this, SourceElement.NO_SOURCE).apply {
                returnType = getDefaultType()
            }
        }

        return classProto.constructorList.firstOrNull { !Flags.IS_SECONDARY.get(it.flags) }?.let { constructorProto ->
            c.memberDeserializer.loadConstructor(constructorProto, true)
        }
    }

    override fun getUnsubstitutedPrimaryConstructor(): ConstructorDescriptor? = primaryConstructor()

    private fun computeConstructors(): Collection<ConstructorDescriptor> =
            computeSecondaryConstructors() + getUnsubstitutedPrimaryConstructor().singletonOrEmptyList()

    private fun computeSecondaryConstructors(): List<ConstructorDescriptor> =
            classProto.constructorList.filter { Flags.IS_SECONDARY.get(it.flags) }.map {
                c.memberDeserializer.loadConstructor(it, false)
            }

    override fun getConstructors() = constructors()

    private fun computeCompanionObjectDescriptor(): ClassDescriptor? {
        if (!classProto.hasCompanionObjectName()) return null

        val companionObjectName = c.nameResolver.getName(classProto.getCompanionObjectName())
        return memberScope.getClassifier(companionObjectName) as? ClassDescriptor
    }

    override fun getCompanionObjectDescriptor(): ClassDescriptor? = companionObjectDescriptor()

    private fun computeSupertypes(): Collection<KotlinType> {
        val result = ArrayList<KotlinType>(classProto.supertypeCount)
        val unresolved = ArrayList<DeserializedType>(0)

        for (supertypeProto in classProto.supertypes(c.typeTable)) {
            val supertype = c.typeDeserializer.type(supertypeProto)
            if (supertype.isError) {
                unresolved.add(supertype.upperIfFlexible() as? DeserializedType ?: error("Not a deserialized type: $supertype"))
            }
            else {
                result.add(supertype)
            }
        }

        result.addAll(c.components.additionalSupertypes.forClass(this))

        if (unresolved.isNotEmpty()) {
            c.components.errorReporter.reportIncompleteHierarchy(this, unresolved.map(DeserializedType::getPresentableText))
        }

        return result.toReadOnlyList()
    }

    internal fun hasNestedClass(name: Name): Boolean {
        return name in nestedClasses.nestedClassNames
    }

    override fun toString() = "deserialized class ${getName().toString()}" // not using descriptor render to preserve laziness

    override fun getSource() = sourceElement

    private inner class DeserializedClassTypeConstructor : AbstractClassTypeConstructor() {
        private val supertypes = c.storageManager.createLazyValue {
            computeSupertypes()
        }

        override fun getParameters() = c.typeDeserializer.ownTypeParameters

        override fun getSupertypes() = supertypes()

        override fun isFinal() = !getModality().isOverridable()

        override fun isDenotable() = true

        override fun getDeclarationDescriptor() = this@DeserializedClassDescriptor

        override fun getAnnotations(): Annotations = Annotations.EMPTY // TODO

        override fun toString() = getName().toString()
    }

    private inner class DeserializedClassMemberScope : DeserializedMemberScope(c, classProto.functionList, classProto.propertyList) {
        private val classDescriptor: DeserializedClassDescriptor get() = this@DeserializedClassDescriptor
        private val allDescriptors = c.storageManager.createLazyValue {
            computeDescriptors(DescriptorKindFilter.ALL, KtScope.ALL_NAME_FILTER, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS)
        }

        override fun getDescriptors(kindFilter: DescriptorKindFilter,
                                    nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> = allDescriptors()

        override fun computeNonDeclaredFunctions(name: Name, functions: MutableCollection<FunctionDescriptor>) {
            val fromSupertypes = ArrayList<FunctionDescriptor>()
            for (supertype in classDescriptor.getTypeConstructor().supertypes) {
                fromSupertypes.addAll(supertype.memberScope.getFunctions(name, NoLookupLocation.FOR_ALREADY_TRACKED))
            }
            generateFakeOverrides(name, fromSupertypes, functions)
        }

        override fun computeNonDeclaredProperties(name: Name, descriptors: MutableCollection<PropertyDescriptor>) {
            val fromSupertypes = ArrayList<PropertyDescriptor>()
            for (supertype in classDescriptor.getTypeConstructor().supertypes) {
                @Suppress("UNCHECKED_CAST")
                fromSupertypes.addAll(supertype.memberScope.getProperties(name, NoLookupLocation.FOR_ALREADY_TRACKED) as Collection<PropertyDescriptor>)
            }
            generateFakeOverrides(name, fromSupertypes, descriptors)
        }

        private fun <D : CallableMemberDescriptor> generateFakeOverrides(name: Name, fromSupertypes: Collection<D>, result: MutableCollection<D>) {
            val fromCurrent = ArrayList<CallableMemberDescriptor>(result)
            OverridingUtil.generateOverridesInFunctionGroup(name, fromSupertypes, fromCurrent, classDescriptor, object : OverridingUtil.DescriptorSink {
                override fun addFakeOverride(fakeOverride: CallableMemberDescriptor) {
                    // TODO: report "cannot infer visibility"
                    OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, null)
                    @Suppress("UNCHECKED_CAST")
                    result.add(fakeOverride as D)
                }

                override fun conflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor) {
                    // TODO report conflicts
                }
            })
        }

        override fun addNonDeclaredDescriptors(result: MutableCollection<DeclarationDescriptor>, location: LookupLocation) {
            for (supertype in classDescriptor.getTypeConstructor().supertypes) {
                for (descriptor in supertype.memberScope.getAllDescriptors()) {
                    if (descriptor is FunctionDescriptor) {
                        result.addAll(getFunctions(descriptor.name, location))
                    }
                    else if (descriptor is PropertyDescriptor) {
                        result.addAll(getProperties(descriptor.name, location))
                    }
                    // Nothing else is inherited
                }
            }
        }

        override fun getImplicitReceiver() = classDescriptor.getThisAsReceiverParameter()

        override fun getClassDescriptor(name: Name): ClassifierDescriptor? =
                classDescriptor.enumEntries.findEnumEntry(name) ?: classDescriptor.nestedClasses.findNestedClass(name)

        override fun addClassDescriptors(result: MutableCollection<DeclarationDescriptor>, nameFilter: (Name) -> Boolean) {
            result.addAll(classDescriptor.nestedClasses.all())
        }

        override fun addEnumEntryDescriptors(result: MutableCollection<DeclarationDescriptor>, nameFilter: (Name) -> Boolean) {
            result.addAll(classDescriptor.enumEntries.all())
        }
    }

    private inner class NestedClassDescriptors {
        internal val nestedClassNames = nestedClassNames()

        internal val findNestedClass = c.storageManager.createMemoizedFunctionWithNullableValues<Name, ClassDescriptor> {
            name ->
            if (name in nestedClassNames) {
                c.components.deserializeClass(classId.createNestedClassId(name))
            }
            else null
        }

        private fun nestedClassNames(): Set<Name> {
            val result = LinkedHashSet<Name>()
            val nameResolver = c.nameResolver
            for (index in classProto.getNestedClassNameList()) {
                result.add(nameResolver.getName(index!!))
            }
            return result
        }

        fun all(): Collection<ClassDescriptor> {
            val result = ArrayList<ClassDescriptor>(nestedClassNames.size())
            nestedClassNames.forEach { name -> result.addIfNotNull(findNestedClass(name)) }
            return result
        }
    }

    private inner class EnumEntryClassDescriptors {
        private val enumEntryNames = enumEntryNames()

        private fun enumEntryNames(): Set<Name> {
            if (getKind() != ClassKind.ENUM_CLASS) return setOf()

            val result = LinkedHashSet<Name>()
            val nameResolver = c.nameResolver
            for (index in classProto.getEnumEntryList()) {
                result.add(nameResolver.getName(index!!))
            }
            return result
        }

        val findEnumEntry = c.storageManager.createMemoizedFunctionWithNullableValues<Name, ClassDescriptor> {
            name ->
            if (name in enumEntryNames) {
                EnumEntrySyntheticClassDescriptor.create(
                        c.storageManager, this@DeserializedClassDescriptor, name, enumMemberNames, SourceElement.NO_SOURCE
                )
            }
            else null
        }

        private val enumMemberNames = c.storageManager.createLazyValue { computeEnumMemberNames() }

        private fun computeEnumMemberNames(): Collection<Name> {
            // NOTE: order of enum entry members should be irrelevant
            // because enum entries are effectively invisible to user (as classes)
            val result = HashSet<Name>()

            for (supertype in getTypeConstructor().getSupertypes()) {
                for (descriptor in supertype.getMemberScope().getAllDescriptors()) {
                    if (descriptor is SimpleFunctionDescriptor || descriptor is PropertyDescriptor) {
                        result.add(descriptor.getName())
                    }
                }
            }

            val nameResolver = c.nameResolver
            return classProto.functionList.mapTo(result) { nameResolver.getName(it.name) } +
                   classProto.propertyList.mapTo(result) { nameResolver.getName(it.name) }
        }

        fun all(): Collection<ClassDescriptor> {
            val result = ArrayList<ClassDescriptor>(enumEntryNames.size())
            enumEntryNames.forEach { name -> result.addIfNotNull(findEnumEntry(name)) }
            return result
        }
    }
}
