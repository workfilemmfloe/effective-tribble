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

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory0;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory1;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetElement;

import static org.jetbrains.jet.lang.diagnostics.PositioningStrategies.DECLARATION_SIGNATURE;
import static org.jetbrains.jet.lang.diagnostics.PositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT;
import static org.jetbrains.jet.lang.diagnostics.Severity.ERROR;

public interface ErrorsJvm {
    DiagnosticFactory1<PsiElement, ConflictingJvmDeclarationsData> CONFLICTING_JVM_DECLARATIONS =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);

    DiagnosticFactory1<PsiElement, ConflictingJvmDeclarationsData> ACCIDENTAL_OVERRIDE =
            DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE_OR_DEFAULT);

    DiagnosticFactory1<JetDeclaration, DeclarationDescriptor> OVERRIDE_CANNOT_BE_STATIC = DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory1<JetDeclaration, DeclarationDescriptor> PLATFORM_STATIC_NOT_IN_OBJECT = DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE);
    DiagnosticFactory1<JetDeclaration, DeclarationDescriptor> PLATFORM_STATIC_ILLEGAL_USAGE = DiagnosticFactory1.create(ERROR, DECLARATION_SIGNATURE);

    @SuppressWarnings("UnusedDeclaration")
    Object _initializer = new Object() {
        {
            Errors.Initializer.initializeFactoryNames(ErrorsJvm.class);
        }
    };
}
