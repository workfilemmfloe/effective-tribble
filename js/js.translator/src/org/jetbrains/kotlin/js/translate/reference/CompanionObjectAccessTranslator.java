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

package org.jetbrains.kotlin.js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TemporaryVariable;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.intrinsic.objects.ObjectIntrinsic;
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils;
import org.jetbrains.kotlin.psi.JetReferenceExpression;
import org.jetbrains.kotlin.psi.JetSimpleNameExpression;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.js.translate.reference.ReferenceTranslator.translateAsFQReference;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumEntry;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isNonCompanionObject;

public class CompanionObjectAccessTranslator extends AbstractTranslator implements CachedAccessTranslator {
    @NotNull
    /*package*/ static CompanionObjectAccessTranslator newInstance(
            @NotNull JetSimpleNameExpression expression,
            @NotNull TranslationContext context
    ) {
        DeclarationDescriptor referenceDescriptor = getDescriptorForReferenceExpression(context.bindingContext(), expression);
        assert referenceDescriptor != null : "JetSimpleName expression must reference a descriptor " + expression.getText();
        return new CompanionObjectAccessTranslator(referenceDescriptor, context);
    }

    /*package*/ static boolean isCompanionObjectReference(
            @NotNull JetReferenceExpression expression,
            @NotNull TranslationContext context
    ) {
        DeclarationDescriptor descriptor = getDescriptorForReferenceExpression(context.bindingContext(), expression);
        return descriptor instanceof ClassDescriptor && !AnnotationsUtils.isNativeObject(descriptor);
    }

    @NotNull
    private final JsExpression referenceToCompanionObject;

    private CompanionObjectAccessTranslator(@NotNull DeclarationDescriptor descriptor, @NotNull TranslationContext context) {
        super(context);
        this.referenceToCompanionObject = generateReferenceToCompanionObject(descriptor, context);
    }

    @NotNull
    private static JsExpression generateReferenceToCompanionObject(@NotNull DeclarationDescriptor descriptor, @NotNull TranslationContext context) {
        if (descriptor instanceof ClassDescriptor) {
            ObjectIntrinsic objectIntrinsic = context.intrinsics().getObjectIntrinsic((ClassDescriptor) descriptor);
            if (objectIntrinsic.exists()) {
                return objectIntrinsic.apply(context);
            }
        }

        JsExpression fqReference = translateAsFQReference(descriptor, context);
        if (isNonCompanionObject(descriptor) || isEnumEntry(descriptor)) {
            return fqReference;
        }

        return Namer.getCompanionObjectAccessor(fqReference);
    }

    @Override
    @NotNull
    public JsExpression translateAsGet() {
        return referenceToCompanionObject;
    }

    @Override
    @NotNull
    public JsExpression translateAsSet(@NotNull JsExpression toSetTo) {
        throw new IllegalStateException("companion object can't be set");
    }

    @NotNull
    @Override
    public CachedAccessTranslator getCached() {
        return this;
    }

    @NotNull
    @Override
    public List<TemporaryVariable> declaredTemporaries() {
        return Collections.emptyList();
    }
}
