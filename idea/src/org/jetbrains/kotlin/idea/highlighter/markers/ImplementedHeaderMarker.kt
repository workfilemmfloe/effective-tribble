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

package org.jetbrains.kotlin.idea.highlighter.markers

import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator
import com.intellij.ide.util.DefaultPsiElementCellRenderer
import com.intellij.psi.NavigatablePsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.checkers.HeaderImplDeclarationChecker
import java.awt.event.MouseEvent

fun ModuleDescriptor.hasImplementationsOf(descriptor: MemberDescriptor) =
        implementationsOf(descriptor).isNotEmpty()

private fun ModuleDescriptor.implementationsOf(descriptor: MemberDescriptor) =
        with (HeaderImplDeclarationChecker(this)) {
            when (descriptor) {
                is CallableMemberDescriptor -> descriptor.findNamesakesFromTheSameModule().filter { it.isImpl }
                is ClassDescriptor -> descriptor.findClassifiersFromTheSameModule().filter { it.isImpl }
                else -> emptyList()
            }
        }

fun getPlatformImplementationTooltip(declaration: KtDeclaration): String? {
    val descriptor = declaration.toDescriptor() as? MemberDescriptor ?: return null
    val commonModuleDescriptor = declaration.containingKtFile.findModuleDescriptor()

    val platformModulesWithImplementation = commonModuleDescriptor.allImplementingModules.filter {
        it.hasImplementationsOf(descriptor)
    }
    if (platformModulesWithImplementation.isEmpty()) return null

    return platformModulesWithImplementation.joinToString(prefix = "Has implementations in ") {
        it.platformKind.name
    }
}

fun navigateToPlatformImplementation(e: MouseEvent?, declaration: KtDeclaration) {
    val descriptor = declaration.toDescriptor() as? MemberDescriptor ?: return
    val commonModuleDescriptor = declaration.containingKtFile.findModuleDescriptor()

    val implementations = commonModuleDescriptor.allImplementingModules.flatMap {
        it.implementationsOf(descriptor)
    }.mapNotNull { DescriptorToSourceUtils.descriptorToDeclaration(it) as? NavigatablePsiElement }
    if (implementations.isEmpty()) return

    val renderer = DefaultPsiElementCellRenderer()
    PsiElementListNavigator.openTargets(e,
                                        implementations.toTypedArray(),
                                        "Choose implementation of ${declaration.name}",
                                        "Implementations of ${declaration.name}",
                                        renderer)
}