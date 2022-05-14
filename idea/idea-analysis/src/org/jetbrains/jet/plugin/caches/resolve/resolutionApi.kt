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

package org.jetbrains.jet.plugin.caches.resolve

import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.analyzer.AnalysisResult
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor

public fun JetElement.getResolutionFacade(): ResolutionFacade {
    return KotlinCacheService.getInstance(getProject()).getResolutionFacade(listOf(this))
}

public fun JetDeclaration.resolveToDescriptor(): DeclarationDescriptor {
    return getResolutionFacade().resolveToDescriptor(this)
}

//NOTE: the difference between analyze and analyzeFully is 'intentionally' unclear
// in theory they do the same thing via different code
// analyze - see ResolveSessionForBodies, ResolveElementCache
// analyzeFully - see KotlinResolveCache, KotlinResolveDataProvider
// In the future these two approaches should be unified
public fun JetElement.analyze(): BindingContext {
    return getResolutionFacade().analyze(this)
}

public fun JetElement.analyzeAndGetResult(): AnalysisResult {
    val resolutionFacade = getResolutionFacade()
    return AnalysisResult.success(resolutionFacade.analyze(this), resolutionFacade.findModuleDescriptor(this))
}

public fun JetElement.findModuleDescriptor(): ModuleDescriptor {
    return getResolutionFacade().findModuleDescriptor(this)
}

public fun JetElement.analyzeFully(): BindingContext {
    return analyzeFullyAndGetResult().bindingContext
}

public fun JetElement.analyzeFullyAndGetResult(vararg extraFiles: JetFile): AnalysisResult {
    return KotlinCacheService.getInstance(getProject()).getResolutionFacade(listOf(this) + extraFiles.toList()).analyzeFullyAndGetResult(listOf(this))
}
