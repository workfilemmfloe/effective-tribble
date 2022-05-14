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

package org.jetbrains.jet.plugin.debugger

import com.intellij.psi.search.FilenameIndex
import org.jetbrains.jet.lang.resolve.kotlin.PackagePartClassUtils
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.descriptors.serialization.JavaProtoBuf
import org.jetbrains.jet.renderer.DescriptorRenderer
import org.jetbrains.jet.plugin.caches.resolve.getLazyResolveSession
import com.intellij.psi.PsiElement
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import com.intellij.openapi.roots.libraries.LibraryUtil
import com.intellij.openapi.module.impl.scopes.LibraryScope
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.JetNamedDeclaration
import org.jetbrains.jet.lang.resolve.BindingContext
import com.intellij.openapi.module.impl.scopes.JdkScope
import com.intellij.openapi.roots.JdkOrderEntry
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.lang.resolve.java.JvmClassName

private val LOG = Logger.getInstance("org.jetbrains.jet.plugin.debugger")

/**
 * This method finds class name for top-level declaration in source file attached to library.
 * The problem is that package-part class file has hash in className and it depends on machine where library was built,
 * so we couldn't predict it.
 * 1. find all .class files with package-part prefix, if there is one - return it
 * 2. find all descriptors with same signature, if there is one - return it
 * 3. else -> return null, because it means that there is more than one function with same signature in project
 */
fun findPackagePartInternalNameForLibraryFile(elementAt: JetElement): String? {
    val topLevelDeclaration = PsiTreeUtil.getTopmostParentOfType(elementAt, javaClass<JetDeclaration>())
    if (topLevelDeclaration == null) {
        reportError(elementAt, null)
        return null
    }

    val packagePartFile = findPackagePartFileNamesForElement(topLevelDeclaration).singleOrNull()
    if (packagePartFile != null) return packagePartFile

    val resolveSession = topLevelDeclaration.getLazyResolveSession()

    val descriptor = resolveSession.resolveToDescriptor(topLevelDeclaration)
    if (descriptor !is CallableDescriptor) return null

    val packageFqName = topLevelDeclaration.getContainingJetFile().getPackageFqName()
    val packageDescriptor = resolveSession.getModuleDescriptor().getPackage(packageFqName)
    if (packageDescriptor == null) {
        reportError(topLevelDeclaration, descriptor)
        return null
    }

    val descFromSourceText = render(descriptor)

    val descriptors: Collection<CallableDescriptor> = when (descriptor) {
        is FunctionDescriptor -> packageDescriptor.getMemberScope().getFunctions(descriptor.getName())
        is PropertyDescriptor -> packageDescriptor.getMemberScope().getProperties(descriptor.getName())
        else -> {
            reportError(topLevelDeclaration, descriptor)
            listOf()
        }
    }

    val deserializedDescriptor = descriptors
            .filterIsInstance(javaClass<DeserializedCallableMemberDescriptor>())
            .filter { render(it) == descFromSourceText }
            .singleOrNull()

    if (deserializedDescriptor == null) {
        reportError(topLevelDeclaration, descriptor)
        return null
    }

    val proto = deserializedDescriptor.proto
    if (proto.hasExtension(JavaProtoBuf.implClassName)) {
        val name = deserializedDescriptor.nameResolver.getName(proto.getExtension(JavaProtoBuf.implClassName)!!)
        return JvmClassName.byFqNameWithoutInnerClasses(packageFqName.child(name)).getInternalName()
    }

    return null
}

private fun findPackagePartFileNamesForElement(elementAt: JetElement): List<String> {
    val project = elementAt.getProject()
    val file = elementAt.getContainingJetFile()

    val packagePartName = PackagePartClassUtils.getPackagePartFqName(file).shortName().asString()
    val packagePartNameWoHash = packagePartName.substring(0, packagePartName.lastIndexOf("-"))

    val libraryEntry = LibraryUtil.findLibraryEntry(file.getVirtualFile(), project)
    val scope = if (libraryEntry is LibraryOrderEntry){
                    LibraryScope(project, libraryEntry.getLibrary() ?: throw AssertionError("Cannot find library for file ${file.getVirtualFile()?.getPath()}"))
                }
                else {
                    JdkScope(project, libraryEntry as JdkOrderEntry)
                }

    val packagePartFiles = FilenameIndex.getAllFilesByExt(project, "class", scope)
            .filter { it.getName().startsWith(packagePartNameWoHash) }
            .map {
                val packageFqName = file.getPackageFqName()
                if (packageFqName.isRoot()) {
                    it.getNameWithoutExtension()
                } else {
                    "${packageFqName.asString()}.${it.getNameWithoutExtension()}"
                }
            }
    return packagePartFiles
}

private fun render(desc: DeclarationDescriptor) = DescriptorRenderer.FQ_NAMES_IN_TYPES.render(desc)

private fun reportError(element: JetElement, descriptor: CallableDescriptor?) {
    LOG.error("Couldn't calculate class name for element in library scope:\n" +
              JetPsiUtil.getElementTextWithContext(element) +
              if (descriptor != null) "\ndescriptor = ${render(descriptor)}" else ""
    )
}
