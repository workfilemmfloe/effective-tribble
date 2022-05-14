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

package kotlin.jvm.internal;

import kotlin.jvm.KotlinReflectionNotSupportedError;
import kotlin.reflect.KCallable;
import kotlin.reflect.KDeclarationContainer;
import kotlin.reflect.KParameter;
import kotlin.reflect.KType;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

/**
 * A superclass for all classes generated by Kotlin compiler for callable references.
 *
 * All methods from reflection API should be implemented here to throw informative exceptions (see KotlinReflectionNotSupportedError)
 */
public abstract class CallableReference implements KCallable {

    // The following methods provide the information identifying this callable, which is used by the reflection implementation.
    // They are supposed to be overridden in each subclass (each anonymous class generated for a callable reference).

    /**
     * @return the class or package where the callable should be located, usually specified on the LHS of the '::' operator
     */
    public KDeclarationContainer getOwner() {
        throw error();
    }

    /**
     * @return Kotlin name of the callable, the one which was declared in the source code (@platformName doesn't change it)
     */
    @Override
    public String getName() {
        throw error();
    }

    /**
     * @return JVM signature of the callable, e.g. "println(Ljava/lang/Object;)V". If this is a property reference,
     * returns the JVM signature of its getter, e.g. "getFoo(Ljava/lang/String;)I". If the property has no getter in the bytecode
     * (e.g. private property in a class), it's still the signature of the imaginary default getter that would be generated otherwise.
     *
     * Note that technically the signature itself is not even used as a signature per se in reflection implementation,
     * but only as a unique and unambiguous way to map a function/property descriptor to a string.
     */
    public String getSignature() {
        throw error();
    }

    // The following methods are the stub implementations of reflection functions.
    // They are called when you're using reflection on a property reference without the reflection implementation in the classpath.

    @Override
    public List<KParameter> getParameters() {
        throw error();
    }

    @Override
    public KType getReturnType() {
        throw error();
    }

    @Override
    public List<Annotation> getAnnotations() {
        throw error();
    }

    @Override
    public Object call(@NotNull Object... args) {
        throw error();
    }

    @Override
    public Object callBy(@NotNull Map args) {
        throw error();
    }

    protected static Error error() {
        throw new KotlinReflectionNotSupportedError();
    }
}