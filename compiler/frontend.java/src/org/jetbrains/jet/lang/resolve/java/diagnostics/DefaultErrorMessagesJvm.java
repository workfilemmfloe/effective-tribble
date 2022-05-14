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

package org.jetbrains.jet.lang.resolve.java.diagnostics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.jet.lang.diagnostics.rendering.DiagnosticFactoryToRendererMap;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.renderer.Renderer;

import static org.jetbrains.jet.lang.diagnostics.PositioningStrategies.DECLARATION_SIGNATURE;

public class DefaultErrorMessagesJvm implements DefaultErrorMessages.Extension {

    private static final Renderer<ConflictingJvmDeclarationsData> CONFLICTING_JVM_DECLARATIONS_DATA = new Renderer<ConflictingJvmDeclarationsData>() {
        @NotNull
        @Override
        public String render(@NotNull ConflictingJvmDeclarationsData data) {
            StringBuilder sb = new StringBuilder();
            for (JvmDeclarationOrigin origin : data.getSignatureOrigins()) {
                DeclarationDescriptor descriptor = origin.getDescriptor();
                if (descriptor != null) {
                    sb.append("    ").append(DescriptorRenderer.COMPACT.render(descriptor)).append("\n");
                }
            }
            return ("The following declarations have the same JVM signature (" + data.getSignature().getName() + data.getSignature().getDesc() + "):\n" + sb).trim();
        }
    };

    public static final DiagnosticFactoryToRendererMap MAP = new DiagnosticFactoryToRendererMap();
    static {
        MAP.put(ErrorsJvm.CONFLICTING_JVM_DECLARATIONS, "Platform declaration clash: {0}", CONFLICTING_JVM_DECLARATIONS_DATA);
        MAP.put(ErrorsJvm.ACCIDENTAL_OVERRIDE, "Accidental override: {0}", CONFLICTING_JVM_DECLARATIONS_DATA);
        MAP.put(ErrorsJvm.PLATFORM_STATIC_NOT_IN_OBJECT, "Only functions in named objects and class objects of classes can be annotated with ''platformStatic''", DescriptorRenderer.SHORT_NAMES_IN_TYPES);
        MAP.put(ErrorsJvm.OVERRIDE_CANNOT_BE_STATIC, "Override cannot be ''platformStatic'' in object", DescriptorRenderer.SHORT_NAMES_IN_TYPES);
        MAP.put(ErrorsJvm.PLATFORM_STATIC_ILLEGAL_USAGE, "This declaration does not support ''platformStatic''", DescriptorRenderer.SHORT_NAMES_IN_TYPES);
    }


    @NotNull
    @Override
    public DiagnosticFactoryToRendererMap getMap() {
        return MAP;
    }
}
