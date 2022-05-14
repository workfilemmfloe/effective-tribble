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

package org.jetbrains.kotlin.js.facade;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.PairConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.backend.ast.JsLocationWithSource;
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver;
import org.jetbrains.kotlin.js.sourceMap.SourceMapBuilder;

import java.io.*;
import java.nio.charset.Charset;
import java.util.function.Supplier;

public class SourceMapBuilderConsumer implements PairConsumer<SourceMapBuilder, Object> {
    @NotNull
    private final SourceFilePathResolver pathResolver;

    private final boolean provideCurrentModuleContent;

    private final boolean provideExternalModuleContent;

    public SourceMapBuilderConsumer(
            @NotNull SourceFilePathResolver pathResolver,
            boolean provideCurrentModuleContent, boolean provideExternalModuleContent
    ) {
        this.pathResolver = pathResolver;
        this.provideCurrentModuleContent = provideCurrentModuleContent;
        this.provideExternalModuleContent = provideExternalModuleContent;
    }

    @Override
    public void consume(SourceMapBuilder builder, Object sourceInfo) {
        if (sourceInfo instanceof PsiElement) {
            PsiElement element = (PsiElement) sourceInfo;
            PsiFile psiFile = element.getContainingFile();
            int offset = element.getNode().getStartOffset();
            Document document = psiFile.getViewProvider().getDocument();
            assert document != null;
            int line = document.getLineNumber(offset);
            int column = offset - document.getLineStartOffset(line);

            File file = new File(psiFile.getViewProvider().getVirtualFile().getPath());
            try {
                Supplier<Reader> contentSupplier;
                if (provideCurrentModuleContent) {
                    contentSupplier = () -> {
                        try {
                            return new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8"));
                        }
                        catch (IOException e) {
                            return null;
                        }
                    };
                }
                else {
                    contentSupplier = () -> null;
                }
                builder.addMapping(pathResolver.getPathRelativeToSourceRoots(file), null, contentSupplier, line, column);
            }
            catch (IOException e) {
                throw new RuntimeException("IO error occurred generating source maps", e);
            }
        }
        else if (sourceInfo instanceof JsLocationWithSource) {
            JsLocationWithSource location = (JsLocationWithSource) sourceInfo;
            Supplier<Reader> contentSupplier = provideExternalModuleContent ? location.getSourceProvider()::invoke : () -> null;
            builder.addMapping(location.getFile(), location.getIdentityObject(), contentSupplier,
                               location.getStartLine(), location.getStartChar());
        }
    }
}
