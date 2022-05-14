/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.presentation;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProvider;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.psi.JetTypeReference;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.util.QualifiedNamesUtil;

import java.util.Collection;

public class JetFunctionPresenter implements ItemPresentationProvider<JetNamedFunction> {
    @Override
    public ItemPresentation getPresentation(final JetNamedFunction function) {
        return new JetDefaultNamedDeclarationPresentation(function) {
            @Override
            public String getPresentableText() {
                StringBuilder presentation = new StringBuilder(function.getName());

                Collection<String> paramsStrings = Collections2.transform(function.getValueParameters(), new Function<JetParameter, String>() {
                    @Override
                    public String apply(JetParameter parameter) {
                        if (parameter != null) {
                            JetTypeReference reference = parameter.getTypeReference();
                            if (reference != null) {
                                String text = reference.getText();
                                if (text != null) {
                                    return text;
                                }
                            }
                        }

                        return "?";
                    }
                });

                presentation.append("(").append(StringUtils.join(paramsStrings, ",")).append(")");
                return presentation.toString();
            }

            @Override
            public String getLocationString() {
                FqName name = JetPsiUtil.getFQName(function);
                if (name != null) {
                    JetTypeReference receiverTypeRef = function.getReceiverTypeRef();
                    String extensionLocation = receiverTypeRef != null ? "for " + receiverTypeRef.getText() + " " : "";
                    return String.format("(%sin %s)", extensionLocation, QualifiedNamesUtil.withoutLastSegment(name));
                }

                return "";
            }
        };
    }
}
