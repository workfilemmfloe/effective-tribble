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

package org.jetbrains.jet.lang.resolve.java.kt;

import com.intellij.psi.PsiAnnotation;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;

public abstract class PsiAnnotationWithAbiVersion extends PsiAnnotationWithFlags {
    protected PsiAnnotationWithAbiVersion(@Nullable PsiAnnotation psiAnnotation) {
        super(psiAnnotation);
    }

    public int getAbiVersion() {
        return getIntAttribute(JvmStdlibNames.ABI_VERSION_NAME, -1);
    }
}
