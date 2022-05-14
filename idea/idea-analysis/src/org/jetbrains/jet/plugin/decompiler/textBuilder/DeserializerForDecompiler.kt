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

package org.jetbrains.jet.plugin.decompiler.textBuilder

import org.jetbrains.jet.lang.resolve.name.ClassId
import org.jetbrains.jet.descriptors.serialization.ClassDataFinder
import org.jetbrains.jet.descriptors.serialization.JavaProtoBufUtil
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.descriptors.impl.MutablePackageFragmentDescriptor
import org.jetbrains.jet.lang.resolve.kotlin.*
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.storage.LockBasedStorageManager
import java.util.Collections
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.jet.lang.resolve.java.resolver.ErrorReporter
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.descriptors.serialization.context.DeserializationGlobalContext
import org.jetbrains.jet.lang.PlatformToKotlinClassMap
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.descriptors.serialization.ClassData
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.jet.plugin.decompiler.isKotlinCompiledFile

public fun DeserializerForDecompiler(classFile: VirtualFile): DeserializerForDecompiler {
    val kotlinClass = KotlinBinaryClassCache.getKotlinBinaryClass(classFile)
    assert(kotlinClass != null) { "Decompiled data factory shouldn't be called on an unsupported file: " + classFile }
    val packageFqName = kotlinClass!!.getClassId().getPackageFqName()
    return DeserializerForDecompiler(classFile.getParent()!!, packageFqName)
}

public class DeserializerForDecompiler(val packageDirectory: VirtualFile, val directoryPackageFqName: FqName) : ResolverForDecompiler {

    private val moduleDescriptor =
            ModuleDescriptorImpl(Name.special("<module for building decompiled sources>"), listOf(), PlatformToKotlinClassMap.EMPTY)

    private fun createDummyModule(name: String) = ModuleDescriptorImpl(Name.special("<$name>"), listOf(), PlatformToKotlinClassMap.EMPTY)

    override fun resolveTopLevelClass(classId: ClassId) = deserializationContext.classDeserializer.deserializeClass(classId)

    override fun resolveDeclarationsInPackage(packageFqName: FqName): Collection<DeclarationDescriptor> {
        assert(packageFqName == directoryPackageFqName, "Was called for $packageFqName but only $directoryPackageFqName is expected.")
        val binaryClassForPackageClass = localClassFinder.findKotlinClass(PackageClassUtils.getPackageClassId(packageFqName))
        val annotationData = binaryClassForPackageClass?.getClassHeader()?.annotationData
        if (annotationData == null) {
            LOG.error("Could not read annotation data for $packageFqName from ${binaryClassForPackageClass?.getClassId()}")
            return Collections.emptyList()
        }
        val membersScope = DeserializedPackageMemberScope(
                createDummyPackageFragment(packageFqName),
                JavaProtoBufUtil.readPackageDataFrom(annotationData),
                deserializationContext
        ) { listOf() }
        return membersScope.getAllDescriptors()
    }

    private val localClassFinder = object : KotlinClassFinder {
        override fun findKotlinClass(javaClass: JavaClass) = findKotlinClass(javaClass.classId)

        override fun findKotlinClass(classId: ClassId): KotlinJvmBinaryClass? {
            if (classId.getPackageFqName() != directoryPackageFqName) {
                return null
            }
            val segments = DeserializedResolverUtils.kotlinFqNameToJavaFqName(classId.getRelativeClassName()).pathSegments()
            val targetName = segments.joinToString("$", postfix = ".class")
            val virtualFile = packageDirectory.findChild(targetName)
            if (virtualFile != null && isKotlinCompiledFile(virtualFile)) {
                return KotlinBinaryClassCache.getKotlinBinaryClass(virtualFile)
            }
            return null
        }
    }
    private val storageManager = LockBasedStorageManager.NO_LOCKS

    private val loadersStorage = DescriptorLoadersStorage(storageManager);
    {
        loadersStorage.setModule(moduleDescriptor)
        loadersStorage.setErrorReporter(LOGGING_REPORTER)
    }

    private val annotationLoader = AnnotationDescriptorLoader();
    {
        annotationLoader.setModule(moduleDescriptor)
        annotationLoader.setKotlinClassFinder(localClassFinder)
        annotationLoader.setErrorReporter(LOGGING_REPORTER)
        annotationLoader.setStorage(loadersStorage)
    }

    private val constantLoader = ConstantDescriptorLoader();
    {
        constantLoader.setKotlinClassFinder(localClassFinder)
        constantLoader.setErrorReporter(LOGGING_REPORTER)
        constantLoader.setStorage(loadersStorage)
    }

    private val classDataFinder = object : ClassDataFinder {
        override fun findClassData(classId: ClassId): ClassData? {
            val binaryClass = localClassFinder.findKotlinClass(classId) ?: return null
            val data = binaryClass.getClassHeader().annotationData
            if (data == null) {
                LOG.error("Annotation data missing for ${binaryClass.getClassId()}")
                return null
            }
            return JavaProtoBufUtil.readClassDataFrom(data)
        }
    }

    private val packageFragmentProvider = object : PackageFragmentProvider {
        override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
            return listOf(createDummyPackageFragment(fqName))
        }

        override fun getSubPackagesOf(fqName: FqName): Collection<FqName> {
            throw UnsupportedOperationException("This method is not supposed to be called.")
        }
    }

    {
        moduleDescriptor.initialize(packageFragmentProvider)
        moduleDescriptor.addDependencyOnModule(moduleDescriptor)
        moduleDescriptor.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule())
        val moduleContainingMissingDependencies = createDummyModule("module containing missing dependencies for decompiled sources")
        moduleContainingMissingDependencies.addDependencyOnModule(moduleContainingMissingDependencies)
        moduleContainingMissingDependencies.initialize(
                PackageFragmentProviderForMissingDependencies(moduleContainingMissingDependencies)
        )
        moduleDescriptor.addDependencyOnModule(moduleContainingMissingDependencies)
        moduleDescriptor.seal()
        moduleContainingMissingDependencies.seal()
    }
    val deserializationContext = DeserializationGlobalContext(storageManager, moduleDescriptor, classDataFinder, annotationLoader,
                                                              constantLoader, packageFragmentProvider, JavaFlexibleTypeCapabilitiesDeserializer)

    private fun createDummyPackageFragment(fqName: FqName): MutablePackageFragmentDescriptor {
        return MutablePackageFragmentDescriptor(moduleDescriptor, fqName)
    }

    private val JavaClass.classId: ClassId
        get() {
            val outer = getOuterClass()
            return if (outer == null) ClassId.topLevel(getFqName()!!) else outer.classId.createNestedClassId(getName())
        }

    class object {
        private val LOG = Logger.getInstance(javaClass<DeserializerForDecompiler>())

        private object LOGGING_REPORTER: ErrorReporter {
            override fun reportLoadingError(message: String, exception: Exception?) {
                LOG.error(message, exception)
            }
            override fun reportCannotInferVisibility(descriptor: CallableMemberDescriptor) {
                LOG.error("Could not infer visibility for $descriptor")
            }
            override fun reportIncompatibleAbiVersion(kotlinClass: KotlinJvmBinaryClass, actualVersion: Int) {
                LOG.error("Incompatible ABI version for class ${kotlinClass.getClassId()}, actual version: $actualVersion")
            }
        }
    }
}
