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

package org.jetbrains.kotlin.resolve.descriptorUtil

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.ClassKind.ENUM_CLASS
import org.jetbrains.kotlin.descriptors.ClassKind.ENUM_ENTRY
import org.jetbrains.kotlin.descriptors.ClassKind.OBJECT
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.DFS

public fun ClassDescriptor.getClassObjectReferenceTarget(): ClassDescriptor = getCompanionObjectDescriptor() ?: this

public fun DeclarationDescriptor.getImportableDescriptor(): DeclarationDescriptor {
    return when {
        this is ConstructorDescriptor -> getContainingDeclaration()
        this is PropertyAccessorDescriptor -> getCorrespondingProperty()
        else -> this
    }
}

public val DeclarationDescriptor.fqNameUnsafe: FqNameUnsafe
    get() = DescriptorUtils.getFqName(this)

public val DeclarationDescriptor.fqNameSafe: FqName
    get() = DescriptorUtils.getFqNameSafe(this)

public val DeclarationDescriptor.isExtension: Boolean
    get() = this is CallableDescriptor && getExtensionReceiverParameter() != null

public val DeclarationDescriptor.module: ModuleDescriptor
    get() = DescriptorUtils.getContainingModule(this)

public fun ModuleDescriptor.resolveTopLevelClass(topLevelClassFqName: FqName, location: LookupLocation): ClassDescriptor? {
    assert(!topLevelClassFqName.isRoot())
    return getPackage(topLevelClassFqName.parent()).memberScope.getClassifier(topLevelClassFqName.shortName(), location) as? ClassDescriptor
}

public val ClassDescriptor.classId: ClassId
    get() {
        val owner = getContainingDeclaration()
        if (owner is PackageFragmentDescriptor) {
            return ClassId(owner.fqName, getName())
        }
        else if (owner is ClassDescriptor) {
            return owner.classId.createNestedClassId(getName())
        }
        throw IllegalStateException("Illegal container: $owner")
    }

public val ClassDescriptor.hasClassObjectType: Boolean get() = classObjectType != null

/** If a literal of this class can be used as a value, returns the type of this value */
public val ClassDescriptor.classObjectType: KotlinType?
    get() {
        val correspondingDescriptor = when (this.getKind()) {
            OBJECT -> this
            // enum entry has the type of enum class
            ENUM_ENTRY -> {
                val container = this.getContainingDeclaration()
                assert(container is ClassDescriptor && container.getKind() == ENUM_CLASS)
                container as ClassDescriptor
            }
            else -> getCompanionObjectDescriptor()
        }
        return correspondingDescriptor?.getDefaultType()
    }

public val DeclarationDescriptorWithVisibility.isEffectivelyPublicApi: Boolean
    get() {
        var parent: DeclarationDescriptorWithVisibility? = this

        while (parent != null) {
            if (!parent.getVisibility().isPublicAPI) return false

            parent = DescriptorUtils.getParentOfType(parent, javaClass<DeclarationDescriptorWithVisibility>())
        }

        return true
    }

public fun ClassDescriptor.getSuperClassNotAny(): ClassDescriptor? {
    for (supertype in getDefaultType().getConstructor().getSupertypes()) {
        val superClassifier = supertype.getConstructor().getDeclarationDescriptor()
        if (!KotlinBuiltIns.isAnyOrNullableAny(supertype) &&
            (DescriptorUtils.isClass(superClassifier) || DescriptorUtils.isEnumClass(superClassifier))) {
            return superClassifier as ClassDescriptor
        }
    }
    return null
}

public fun ClassDescriptor.getSuperClassOrAny(): ClassDescriptor = getSuperClassNotAny() ?: builtIns.getAny()

public val ClassDescriptor.secondaryConstructors: List<ConstructorDescriptor>
    get() = getConstructors().filterNot { it.isPrimary() }

public val DeclarationDescriptor.builtIns: KotlinBuiltIns
    get() = module.builtIns

/**
 * Returns containing declaration of dispatch receiver for callable adjusted to fake-overridden cases
 *
 * open class A {
 *   fun foo() = 1
 * }
 * class B : A()
 *
 * for A.foo -> returns A (dispatch receiver parameter is A)
 * for B.foo -> returns B (dispatch receiver parameter is still A, but it's fake-overridden in B, so it's containing declaration is B)
 *
 * class Outer {
 *   inner class Inner()
 * }
 *
 * for constructor of Outer.Inner -> returns Outer (dispatch receiver parameter is Outer, but it's containing declaration is Inner)
 *
 */
public fun CallableDescriptor.getOwnerForEffectiveDispatchReceiverParameter(): DeclarationDescriptor? {
    if (this is CallableMemberDescriptor && getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        return getContainingDeclaration()
    }
    return getDispatchReceiverParameter()?.getContainingDeclaration()
}

/**
 * @return `true` iff the parameter has a default value, i.e. declares it or inherits by overriding a parameter which has a default value.
 */
public fun ValueParameterDescriptor.hasDefaultValue(): Boolean {
    val handler = object : DFS.AbstractNodeHandler<ValueParameterDescriptor, Boolean>() {
        var result = false

        override fun beforeChildren(current: ValueParameterDescriptor): Boolean {
            result = result || current.declaresDefaultValue()
            return !result
        }

        override fun result() = result
    }

    DFS.dfs(
            listOf(this),
            DFS.Neighbors<ValueParameterDescriptor> { current ->
                current.overriddenDescriptors.map(ValueParameterDescriptor::getOriginal)
            },
            handler
    )

    return handler.result()
}

public fun Annotated.isRepeatableAnnotation(): Boolean =
        annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.repeatable) != null

public fun Annotated.isDocumentedAnnotation(): Boolean =
        annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.mustBeDocumented) != null

public fun Annotated.getAnnotationRetention(): KotlinRetention? {
    val annotationEntryDescriptor = annotations.findAnnotation(KotlinBuiltIns.FQ_NAMES.retention) ?: return null
    val retentionArgumentValue = annotationEntryDescriptor.allValueArguments.entrySet().firstOrNull {
        it.key.name.asString() == "value"
    }?.getValue() as? EnumValue ?: return null
    return KotlinRetention.valueOf(retentionArgumentValue.value.name.asString())
}

public val DeclarationDescriptor.parentsWithSelf: Sequence<DeclarationDescriptor>
    get() = sequence(this, { it.containingDeclaration })

public val DeclarationDescriptor.parents: Sequence<DeclarationDescriptor>
    get() = parentsWithSelf.drop(1)

