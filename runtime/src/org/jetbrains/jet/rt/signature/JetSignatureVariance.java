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

package org.jetbrains.jet.rt.signature;

/**
 * @author Stepan Koltsov
 */
public enum JetSignatureVariance {
    INVARIANT('='),
    IN('-'),
    OUT('+'),
    ;
    
    private final char c;

    private JetSignatureVariance(char c) {
        this.c = c;
    }

    public char getC() {
        return c;
    }
    
    public static JetSignatureVariance parseVariance(char c) {
        switch (c) {
            case '=': return INVARIANT;
            case '+': return OUT;
            case '-': return IN;
            default: throw new IllegalStateException();
        }
    }
}
