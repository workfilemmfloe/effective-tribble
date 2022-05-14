/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.general;

import com.google.common.collect.Lists;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.util.Collection;
import java.util.List;

/**
 * Helps find functions which are annotated with a @Test annotation from junit
 */
public class JetTestFunctionDetector {
    private JetTestFunctionDetector() {
    }

    private static boolean isTest(@NotNull FunctionDescriptor functionDescriptor) {
        Annotations annotations = functionDescriptor.getAnnotations();
        for (AnnotationDescriptor annotation : annotations) {
            // TODO ideally we should find the fully qualified name here...
            JetType type = annotation.getType();
            String name = type.toString();
            if (name.equals("Test")) {
                return true;
            }
        }

        /*
        if (function.getName().startsWith("test")) {
            List<JetParameter> parameters = function.getValueParameters();
            return parameters.size() == 0;
        }
        */
        return false;
    }

    @NotNull
    public static List<FunctionDescriptor> getTestFunctionDescriptors(
            @NotNull BindingContext bindingContext,
            @NotNull Collection<JetFile> files
    ) {
        List<FunctionDescriptor> answer = Lists.newArrayList();
        for (JetFile file : files) {
            answer.addAll(getTestFunctions(bindingContext, file.getDeclarations()));
        }
        return answer;
    }

    @NotNull
    private static List<FunctionDescriptor> getTestFunctions(
            @NotNull BindingContext bindingContext,
            @NotNull List<JetDeclaration> declarations
    ) {
        List<FunctionDescriptor> answer = Lists.newArrayList();
        for (JetDeclaration declaration : declarations) {
            JetScope scope = null;

            if (declaration instanceof JetClass) {
                JetClass klass = (JetClass) declaration;
                ClassDescriptor classDescriptor = BindingUtils.getClassDescriptor(bindingContext, klass);

                if (classDescriptor.getModality() != Modality.ABSTRACT) {
                    scope = classDescriptor.getDefaultType().getMemberScope();
                }
            }

            if (scope != null) {
                Collection<DeclarationDescriptor> allDescriptors = scope.getAllDescriptors();
                List<FunctionDescriptor> testFunctions = ContainerUtil.mapNotNull(
                        allDescriptors,
                        new Function<DeclarationDescriptor, FunctionDescriptor>() {
                            @Override
                            public FunctionDescriptor fun(DeclarationDescriptor descriptor) {
                                if (descriptor instanceof FunctionDescriptor) {
                                    FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
                                    if (isTest(functionDescriptor)) return functionDescriptor;
                                }

                                return null;
                            }
                        });

                answer.addAll(testFunctions);
            }
        }
        return answer;
    }
}
