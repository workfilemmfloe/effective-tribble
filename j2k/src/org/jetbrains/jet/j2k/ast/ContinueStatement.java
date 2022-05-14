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

package org.jetbrains.jet.j2k.ast;

import org.jetbrains.annotations.NotNull;

public class ContinueStatement extends Statement {
    private Identifier myLabel = Identifier.EMPTY_IDENTIFIER;

    public ContinueStatement(Identifier label) {
        myLabel = label;
    }

    public ContinueStatement() {
    }

    @NotNull
    @Override
    public Kind getKind() {
        return Kind.CONTINUE;
    }

    @NotNull
    @Override
    public String toKotlin() {
        if (myLabel.isEmpty()) {
            return "continue";
        }
        return "continue" + AT + myLabel.toKotlin();
    }
}
