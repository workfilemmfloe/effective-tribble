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

package org.jetbrains.kotlin.idea.refactoring.safeDelete

import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.targetDescriptors
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetImportDirective

public class SafeDeleteImportDirectiveUsageInfo(
        importDirective: JetImportDirective, declaration: JetDeclaration
) : SafeDeleteReferenceSimpleDeleteUsageInfo(importDirective, declaration, importDirective.isSafeToDelete(declaration))

private fun JetImportDirective.isSafeToDelete(declaration: JetDeclaration): Boolean {
    val referencedDescriptor = targetDescriptors().singleOrNull() ?: return false
    return referencedDescriptor == declaration.resolveToDescriptor()
}
