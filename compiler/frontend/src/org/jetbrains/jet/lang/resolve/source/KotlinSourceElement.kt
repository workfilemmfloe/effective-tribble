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

package org.jetbrains.jet.lang.resolve.source

import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.descriptors.SourceElement
import com.intellij.psi.PsiElement

public class KotlinSourceElement(override val psi: JetElement) : PsiSourceElement

public fun JetElement?.toSourceElement(): SourceElement = if (this == null) SourceElement.NO_SOURCE else KotlinSourceElement(this)

public fun SourceElement.getPsi(): PsiElement? = (this as? PsiSourceElement)?.psi