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

package org.jetbrains.kotlin.idea.codeInsight.shorten

import com.intellij.openapi.project.Project
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.UserDataProperty
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.psi.NotNullableUserDataProperty
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.SmartPointerManager
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.idea.util.ShortenReferences.Options
import org.jetbrains.kotlin.idea.util.ShortenReferences
import java.util.*

class ShorteningRequest(val pointer: SmartPsiElementPointer<JetElement>, val options: Options)

private var Project.elementsToShorten: MutableSet<ShorteningRequest>?
        by UserDataProperty(Key.create("ELEMENTS_TO_SHORTEN_KEY"))

/*
 * When one refactoring invokes another this value must be set to false so that shortening wait-set is not cleared
 * and previously collected references are processed correctly. Afterwards it must be reset to original value
 */
public var Project.ensureElementsToShortenIsEmptyBeforeRefactoring: Boolean
        by NotNullableUserDataProperty(Key.create("ENSURE_ELEMENTS_TO_SHORTEN_IS_EMPTY"), true)

private fun Project.getOrCreateElementsToShorten(): MutableSet<ShorteningRequest> {
    var elements = elementsToShorten
    if (elements == null) {
        elements = LinkedHashSet()
        elementsToShorten = elements
    }

    return elements!!
}

public fun JetElement.addToShorteningWaitSet(options: Options = Options.DEFAULT) {
    assert (ApplicationManager.getApplication()!!.isWriteAccessAllowed(), "Write access needed")
    val project = getProject()
    val elementPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(this)
    project.getOrCreateElementsToShorten().add(ShorteningRequest(elementPointer, options))
}

public fun performDelayedShortening(project: Project) {
    project.elementsToShorten?.let { requests ->
        project.elementsToShorten = null
        val elements = requests.map { it.pointer.getElement() }
        val options = requests.map { it.options }
        val elementToOptions = (elements zip options).toMap()
        //TODO: this is not correct because it should not shorten deep into the elements!
        ShortenReferences({ elementToOptions[it] ?: ShortenReferences.Options.DEFAULT }).process(elements.filterNotNull())
    }
}

private val LOG = Logger.getInstance(javaClass<Project>().getCanonicalName())

public fun prepareElementsToShorten(project: Project) {
    val elementsToShorten = project.elementsToShorten
    if (project.ensureElementsToShortenIsEmptyBeforeRefactoring && elementsToShorten != null && !elementsToShorten.isEmpty()) {
        LOG.warn("Waiting set for reference shortening is not empty")
        project.elementsToShorten = null
    }
}
