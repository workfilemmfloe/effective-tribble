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

package org.jetbrains.kotlin.di;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;

public class InjectorForJavaDescriptorResolverUtil {
    @NotNull
    public static InjectorForJavaDescriptorResolver create(
            @NotNull Project project,
            @NotNull BindingTrace bindingTrace,
            boolean dependOnBuiltins
    ) {
        InjectorForJavaDescriptorResolver injector = new InjectorForJavaDescriptorResolver(project, bindingTrace);
        ModuleDescriptorImpl module = injector.getModule();
        module.initialize(injector.getJavaDescriptorResolver().getPackageFragmentProvider());
        module.addDependencyOnModule(module);
        if (dependOnBuiltins) {
            module.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule());
        }
        module.seal();

        return injector;
    }
}
