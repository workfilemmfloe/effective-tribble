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

package org.jetbrains.kotlin.diagnostics.rendering

interface DiagnosticParameterRenderer<in O> {
    fun render(obj: O, renderingContext: RenderingContext): String
}

interface ContextIndependentParameterRenderer<in O> : DiagnosticParameterRenderer<O> {
    override fun render(obj: O, renderingContext: RenderingContext): String = render(obj)

    fun render(obj: O): String
}

fun <O> Renderer(block: (O) -> String) = object : ContextIndependentParameterRenderer<O> {
    override fun render(obj: O): String = block(obj)
}

fun <O> ContextDependentRenderer(block: (O, RenderingContext) -> String) = object : DiagnosticParameterRenderer<O> {
    override fun render(obj: O, renderingContext: RenderingContext): String = block(obj, renderingContext)
}

fun <P> renderParameter(parameter: P, renderer: DiagnosticParameterRenderer<P>?, context: RenderingContext): Any? =
    renderer?.render(parameter, context) ?: parameter
