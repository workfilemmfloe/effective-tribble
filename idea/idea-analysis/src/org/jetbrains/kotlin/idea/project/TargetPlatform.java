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

package org.jetbrains.kotlin.idea.project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.resolve.KotlinJsDeclarationCheckerProvider;
import org.jetbrains.kotlin.load.kotlin.JavaDeclarationCheckerProvider;
import org.jetbrains.kotlin.resolve.AdditionalCheckerProvider;
import org.jetbrains.kotlin.types.DynamicTypesAllowed;
import org.jetbrains.kotlin.types.DynamicTypesSettings;

public interface TargetPlatform {
    @NotNull
    AdditionalCheckerProvider getAdditionalCheckerProvider();

    @NotNull
    DynamicTypesSettings getDynamicTypesSettings();

    TargetPlatform JVM = new TargetPlatformImpl("JVM", JavaDeclarationCheckerProvider.INSTANCE$, new DynamicTypesSettings());
    TargetPlatform JS = new TargetPlatformImpl("JS", KotlinJsDeclarationCheckerProvider.INSTANCE$, new DynamicTypesAllowed());
}
