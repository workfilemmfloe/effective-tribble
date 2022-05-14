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

package org.jetbrains.kotlin.diagnostics.rendering;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters3;
import org.jetbrains.kotlin.renderer.Renderer;

public class DiagnosticWithParameters3Renderer<A, B, C> extends AbstractDiagnosticWithParametersRenderer<DiagnosticWithParameters3<?, A, B, C>> {
    private final Renderer<? super A> rendererForA;
    private final Renderer<? super B> rendererForB;
    private final Renderer<? super C> rendererForC;

    public DiagnosticWithParameters3Renderer(@NotNull String message,
            @Nullable Renderer<? super A> rendererForA,
            @Nullable Renderer<? super B> rendererForB,
            @Nullable Renderer<? super C> rendererForC) {
        super(message);
        this.rendererForA = rendererForA;
        this.rendererForB = rendererForB;
        this.rendererForC = rendererForC;
    }

    @NotNull
    @Override
    public Object[] renderParameters(@NotNull DiagnosticWithParameters3<?, A, B, C> diagnostic) {
        return new Object[]{
                DiagnosticRendererUtilKt.renderParameter(diagnostic.getA(), rendererForA),
                DiagnosticRendererUtilKt.renderParameter(diagnostic.getB(), rendererForB),
                DiagnosticRendererUtilKt.renderParameter(diagnostic.getC(), rendererForC)};
    }
}
