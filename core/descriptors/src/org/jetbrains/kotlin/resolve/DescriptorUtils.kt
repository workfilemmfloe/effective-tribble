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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.descriptors.ClassKind.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.builtins.KotlinBuiltIns

public fun ClassDescriptor.getClassObjectReferenceTarget(): ClassDescriptor = getCompanionObjectDescriptor() ?: this

public fun DeclarationDescriptor.getImportableDescriptor(): DeclarationDescriptor {
    return when {
        this is ConstructorDescriptor -> getContainingDeclaration()
        this is PropertyAccessorDescriptor -> getCorrespondingProperty()
        else -> this
    }
}

public val DeclarationDescriptor.isExtension: Boolean
    get() = this is CallableDescriptor && getExtensionReceiverParameter() != null

public val DeclarationDescriptor.module: ModuleDescriptor
    get() = DescriptorUtils.getContainingModule(this)

public fun ModuleDescriptor.resolveTopLevelClass(topLevelClassFqName: FqName): ClassDescriptor? {
    assert(!topLevelClassFqName.isRoot())
    return getPackage(topLevelClassFqName.parent())?.getMemberScope()
            ?.getClassifier(topLevelClassFqName.shortName()) as? ClassDescriptor
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

/** If a literal of this class can be used as a value returns the class which represents the type of this value */
public val ClassDescriptor.classObjectDescriptor: ClassDescriptor?
    get() {
        return when (this.getKind()) {
            OBJECT -> this
            ENUM_ENTRY -> {
                val container = this.getContainingDeclaration()
                assert(container is ClassDescriptor && container.getKind() == ENUM_CLASS)
                container as ClassDescriptor
            }
            else -> getCompanionObjectDescriptor()
        }
    }

public val DeclarationDescriptorWithVisibility.isEffectivelyPublicApi: Boolean
    get() {
        var parent: DeclarationDescriptorWithVisibility? = this

        while (parent != null) {
            if (!parent!!.getVisibility().isPublicAPI()) return false

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

public fun ClassDescriptor.getSuperClassOrAny(): ClassDescriptor = getSuperClassNotAny() ?: KotlinBuiltIns.getInstance().getAny()

public val ClassDescriptor.secondaryConstructors: List<ConstructorDescriptor>
    get() = getConstructors().filterNot { it.isPrimary() }

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
