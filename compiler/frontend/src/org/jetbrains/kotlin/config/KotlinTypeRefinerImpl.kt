/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.builtins.isFunctionOrSuspendFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.checker.NewCapturedTypeConstructor
import org.jetbrains.kotlin.types.checker.REFINER_CAPABILITY
import org.jetbrains.kotlin.types.refinement.TypeRefinement
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

@UseExperimental(TypeRefinement::class)
class KotlinTypeRefinerImpl(
    private val moduleDescriptor: ModuleDescriptor,
    private val storageManager: StorageManager
) : KotlinTypeRefiner() {
    init {
        moduleDescriptor.getCapability(REFINER_CAPABILITY)?.value = this
    }

    private val refinedTypeCache = storageManager.createRecursionTolerantCacheWithNotNullValues<KotlinType, KotlinType>()
    private val _isRefinementNeededForTypeConstructor =
        storageManager.createMemoizedFunction<TypeConstructor, Boolean> { it.areThereExpectSupertypesOrTypeArguments() }
    private val scopes = storageManager.createCacheWithNotNullValues<ClassDescriptor, MemberScope>()

    @TypeRefinement
    override fun refineType(type: KotlinType): KotlinType {
        return when {
            type.hasNotTrivialRefinementFactory -> {
                val cached = refinedTypeCache.computeIfAbsent(
                    key = type,
                    computation = { type.refine(this) },
                    onRecursive = { RefinedSimpleTypeWrapper(storageManager.createLazyValue { refineType(type) as SimpleType }) }
                )
                updateArgumentsAnnotationsIfNeeded(type, cached)
            }

            else -> type.refine(this)
        }
    }

    private fun updateArgumentsAnnotationsIfNeeded(originalType: KotlinType, cachedType: KotlinType): KotlinType {
        if (!originalType.isArgumentsAnnotationsUpdateNeeded()) return cachedType

        fun doReplace(original: KotlinType, cached: KotlinType): KotlinType {
            val newArguments = mutableListOf<TypeProjection>()
            for ((originalArg, cachedArg) in original.arguments zip cached.arguments) {
                if (cachedArg.type.isError || TypeUtils.noExpectedType(cachedArg.type)) {
                    newArguments += cachedArg
                } else {
                    newArguments += cachedArg.replaceType(doReplace(originalArg.type, cachedArg.type))
                }
            }
            return cached.replace(newArguments, original.annotations)
        }

        return doReplace(originalType, cachedType)
    }

    private fun KotlinType.isArgumentsAnnotationsUpdateNeeded(): Boolean = isFunctionOrSuspendFunctionType

    @TypeRefinement
    override fun refineSupertypes(classDescriptor: ClassDescriptor): Collection<KotlinType> {
        // Note that we can't omit refinement even if classDescriptor.module == moduleDescriptor,
        // because such class may have supertypes which need refinement
        return classDescriptor.typeConstructor.supertypes.map { refineType(it) }
    }

    @TypeRefinement
    override fun refineDescriptor(descriptor: DeclarationDescriptor): ClassifierDescriptor? {
        if (descriptor !is ClassifierDescriptorWithTypeParameters) return null
        val classId = descriptor.classId ?: return null
        return moduleDescriptor.findClassifierAcrossModuleDependencies(classId)
    }

    @TypeRefinement
    override fun refineTypeAliasTypeConstructor(typeAliasDescriptor: TypeAliasDescriptor): TypeConstructor? {
        return typeAliasDescriptor.classId?.let { moduleDescriptor.findClassifierAcrossModuleDependencies(it) }?.typeConstructor
    }

    @TypeRefinement
    override fun findClassAcrossModuleDependencies(classId: ClassId): ClassDescriptor? {
        return moduleDescriptor.findClassAcrossModuleDependencies(classId)
    }

    @TypeRefinement
    override fun isRefinementNeededForModule(moduleDescriptor: ModuleDescriptor): Boolean {
        return this.moduleDescriptor !== moduleDescriptor
    }

    @TypeRefinement
    override fun isRefinementNeededForTypeConstructor(typeConstructor: TypeConstructor): Boolean {
        return try {
            _isRefinementNeededForTypeConstructor.invoke(typeConstructor)
        } catch (e: AssertionError) {
            false
        }
    }

    @TypeRefinement
    override fun <S : MemberScope> getOrPutScopeForClass(classDescriptor: ClassDescriptor, compute: () -> S): S {
        @Suppress("UNCHECKED_CAST")
        return scopes.computeIfAbsent(classDescriptor, compute) as S
    }

    private fun TypeConstructor.areThereExpectSupertypesOrTypeArguments(): Boolean {
        var result = false
        DFS.dfs(
            listOf(this),
            DFS.Neighbors(TypeConstructor::allDependentTypeConstructors),
            DFS.VisitedWithSet(),
            object : DFS.AbstractNodeHandler<TypeConstructor, Unit>() {
                override fun beforeChildren(current: TypeConstructor): Boolean {
                    if (current.isExpectClass() && current.declarationDescriptor?.module != moduleDescriptor) {
                        result = true
                        return false
                    }
                    return true
                }

                override fun result() = Unit
            }
        )

        return result
    }
}

val LanguageVersionSettings.isTypeRefinementEnabled: Boolean
    get() = getFlag(AnalysisFlags.useTypeRefinement) && supportsFeature(LanguageFeature.MultiPlatformProjects)

private val TypeConstructor.allDependentTypeConstructors: Collection<TypeConstructor>
    get() = when (this) {
        is NewCapturedTypeConstructor -> {
            supertypes.map { it.constructor } + projection.type.constructor
        }
        else -> supertypes.map { it.constructor } + parameters.map { it.typeConstructor }
    }

private fun TypeConstructor.isExpectClass() =
    declarationDescriptor?.safeAs<ClassDescriptor>()?.isExpect == true

private class RefinedSimpleTypeWrapper(private val _delegate: NotNullLazyValue<SimpleType>) : DelegatingSimpleType() {
    override val delegate: SimpleType
        get() = _delegate()

    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType): DelegatingSimpleType {
        throw IllegalStateException("replaceDelegate should not be called on RefinedSimpleTypeWrapper")
    }

    override fun replaceAnnotations(newAnnotations: Annotations): SimpleType {
        return delegate.replaceAnnotations(newAnnotations)
    }

    override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType {
        return delegate.makeNullableAsSpecified(newNullability)
    }

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): SimpleType {
        return kotlinTypeRefiner.refineType(delegate) as SimpleType
    }
}
