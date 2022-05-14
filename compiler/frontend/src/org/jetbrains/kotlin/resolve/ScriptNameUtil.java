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

package org.jetbrains.kotlin.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.parsing.KotlinScriptDefinition;
import org.jetbrains.kotlin.parsing.KotlinScriptDefinitionProvider;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPackageDirective;
import org.jetbrains.kotlin.psi.KtScript;

public class ScriptNameUtil {
    private ScriptNameUtil() {
    }

    @NotNull
    public static FqName classNameForScript(KtScript script) {
        KtFile file = script.getContainingKtFile();
        KotlinScriptDefinition scriptDefinition = KotlinScriptDefinitionProvider.getInstance(file.getProject()).findScriptDefinition(file);

        String name = file.getName();
        int index = name.lastIndexOf('/');
        if(index != -1)
            name = name.substring(index+1);
        if(name.endsWith(scriptDefinition.getExtension()))
            name = name.substring(0, name.length()-scriptDefinition.getExtension().length());
        else {
            index = name.indexOf('.');
            if(index != -1)
                name = name.substring(0,index);
        }
        name = Character.toUpperCase(name.charAt(0)) + (name.length() == 0 ? "" : name.substring(1));
        name = name.replace('.', '_');
        KtPackageDirective directive = file.getPackageDirective();
        if(directive != null && directive.getQualifiedName().length() > 0) {
            name = directive.getQualifiedName() + "." + name;
        }
        return new FqName(name);
    }
}
