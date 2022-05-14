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

package org.jetbrains.kotlin.load.kotlin

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.util.ModuleVisibilityHelper
import java.io.File

class ModuleVisibilityHelperImpl: ModuleVisibilityHelper {
    override fun isInFriendModule(what: DeclarationDescriptor, from: DeclarationDescriptor): Boolean {
        val fromSource = getSourceElement(from)
        // We should check accessibility of 'from' in current module (some set of source files, which are compiled together),
        // so we can assume that 'from' should have sources or is a LazyPackageDescriptor with some package files.
        val project: Project = if (fromSource is KotlinSourceElement) {
            fromSource.psi.project
        }
        else {
            (from as? LazyPackageDescriptor)?.declarationProvider?.getPackageFiles()?.firstOrNull()?.project ?: return true
        }
        return ModuleVisibilityManager.SERVICE.getInstance(project).isInFriendModule(what, from)
    }
}

interface ModuleVisibilityManager {
    fun addModule(module: Module)
    fun addFriendPath(path: String)

    fun isInFriendModule(what: DeclarationDescriptor, from: DeclarationDescriptor): Boolean

    object SERVICE {
        val EP_NAME = ExtensionPointName.create<ModuleVisibilityManager>("org.jetbrains.kotlin.moduleVisibilityManager")

        @JvmStatic fun getInstance(project: Project): ModuleVisibilityManager = Extensions.getArea(project).getExtensionPoint(EP_NAME).extensions.single()
    }

    class Default: ModuleVisibilityManager {
        override fun addModule(module: Module) {}

        override fun addFriendPath(path: String) {}

        override fun isInFriendModule(what: DeclarationDescriptor, from: DeclarationDescriptor): Boolean = true
    }
}


class CliModuleVisibilityManagerImpl : ModuleVisibilityManager, Disposable {
    val modules: MutableList<Module> = arrayListOf()
    val friendPaths: MutableList <String> = arrayListOf()

    override fun addModule(module: Module) {
        modules.add(module)
    }

    override fun addFriendPath(path: String) {
        friendPaths.add(path)
    }

    override fun isInFriendModule(what: DeclarationDescriptor, from: DeclarationDescriptor): Boolean {
        friendPaths.forEach {
            if (isContainedByCompiledPartOfOurModule(what, File(it))) return true
        }

        val whatSource = getSourceElement(what)
        if (whatSource is KotlinSourceElement) {
            if (modules.size > 1 && getSourceElement(from) is KotlinSourceElement) {
                return findModule(what, modules) === findModule(from, modules)
            }

            return true
        }

        if (modules.isEmpty()) return false

        if (modules.size == 1 && isContainedByCompiledPartOfOurModule(what, File(modules.single().getOutputDirectory()))) return true

        return findModule(from, modules) === findModule(what, modules)
    }


    override fun dispose() {
        modules.clear()
    }
}


private fun findModule(descriptor: DeclarationDescriptor, modules: Collection<Module>): Module? {
    val sourceElement = getSourceElement(descriptor)
    if (sourceElement is KotlinSourceElement) {
        return modules.singleOrNull() ?: modules.firstOrNull { sourceElement.psi.containingKtFile.virtualFile.path in it.getSourceFiles() }
    }
    else {
        return modules.firstOrNull { module ->
            isContainedByCompiledPartOfOurModule(descriptor, File(module.getOutputDirectory())) ||
            module.getFriendPaths().any { isContainedByCompiledPartOfOurModule(descriptor, File(it)) }
        }
    }
}

fun isContainedByCompiledPartOfOurModule(descriptor: DeclarationDescriptor, outDirectory: File?): Boolean {
    if (outDirectory == null) return false

    val packageFragment = DescriptorUtils.getParentOfType(descriptor, PackageFragmentDescriptor::class.java, false)
    if (packageFragment !is LazyJavaPackageFragment) return false

    val source = getSourceElement(descriptor)

    val binaryClass = when (source) {
        is KotlinJvmBinarySourceElement ->
            source.binaryClass
        is KotlinJvmBinaryPackageSourceElement ->
            if (descriptor is DeserializedCallableMemberDescriptor) {
                source.getContainingBinaryClass(descriptor) ?: source.getRepresentativeBinaryClass()
            }
            else {
                source.getRepresentativeBinaryClass()
            }
        else ->
            null
    }

    if (binaryClass is VirtualFileKotlinClass) {
        val file = binaryClass.file
        if (file.fileSystem.protocol == StandardFileSystems.FILE_PROTOCOL) {
            val ioFile = VfsUtilCore.virtualToIoFile(file)
            return ioFile.absolutePath.startsWith(outDirectory.absolutePath + File.separator)
        }
    }

    return false
}

fun getSourceElement(descriptor: DeclarationDescriptor): SourceElement =
        if (descriptor is CallableMemberDescriptor && descriptor.source === SourceElement.NO_SOURCE) {
            descriptor.containingDeclaration.toSourceElement
        }
        else {
            descriptor.toSourceElement
        }

private val DeclarationDescriptor.toSourceElement: SourceElement
    get() = if (this is DeclarationDescriptorWithSource) source else SourceElement.NO_SOURCE
