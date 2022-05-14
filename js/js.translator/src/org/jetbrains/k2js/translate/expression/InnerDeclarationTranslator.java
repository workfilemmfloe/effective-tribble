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

package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.context.UsageTracker;

import java.util.List;

abstract class InnerDeclarationTranslator {
    protected final TranslationContext context;
    protected final JsFunction fun;

    public InnerDeclarationTranslator(
            @NotNull TranslationContext context,
            @NotNull JsFunction fun
    ) {
        this.context = context;
        this.fun = fun;
    }

    @NotNull
    public JsExpression translate(@NotNull JsNameRef nameRef, @Nullable JsExpression self) {
        UsageTracker usageTracker = context.usageTracker();
        assert usageTracker != null : "Usage tracker should not be null for InnerDeclarationTranslator";

        boolean hasCaptured = usageTracker.hasCaptured();
        if (!hasCaptured && self == JsLiteral.NULL) {
            return createExpression(nameRef, self);
        }

        JsInvocation invocation = createInvocation(nameRef, self);
        if (hasCaptured) {
            final List<JsExpression> invocationArguments = invocation.getArguments();
            usageTracker.forEachCaptured(new Consumer<CallableDescriptor>() {
                @Override
                public void consume(CallableDescriptor descriptor) {
                    fun.getParameters().add(new JsParameter(getParameterNameFor(descriptor)));
                    invocationArguments.add(getParameterNameRefFor(descriptor));
                }
            });
        }
        return invocation;
    }

    @NotNull
    protected JsName getParameterNameFor(@NotNull CallableDescriptor descriptor) {
        return context.getNameForDescriptor(descriptor);
    }

    @NotNull
    protected JsNameRef getParameterNameRefFor(@NotNull CallableDescriptor descriptor) {
        return getParameterNameFor(descriptor).makeRef();
    }

    @NotNull
    protected abstract JsExpression createExpression(@NotNull JsNameRef nameRef, @Nullable JsExpression self);

    @NotNull
    protected abstract JsInvocation createInvocation(@NotNull JsNameRef nameRef, @Nullable JsExpression self);
}
