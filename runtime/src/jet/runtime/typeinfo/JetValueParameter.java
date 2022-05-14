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

package jet.runtime.typeinfo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for parameters
 *
 * @url http://confluence.jetbrains.net/display/JET/Jet+Signatures
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface JetValueParameter {
    /**
     * @return name of parameter
     */
    String name ();

    /**
     * @return if this parameter has default value
     */
    boolean hasDefaultValue() default false;

    /**
     * @return if this parameter is receiver
     */
    boolean receiver() default false;

    /**
     * @return type unless Java type is correct Kotlin type.
     */
    String type() default "";

    /**
     * @return <code>true</code> if this parameter is a vararg
     *
     * NOTE: a method may have a vararg parameter in Kotlin and not be marked as Opcodes.ACC_VARARGS, e.g.
     * fun foo(vararg x: Int, f: () -> Unit)
     */
    boolean vararg() default false;
}
