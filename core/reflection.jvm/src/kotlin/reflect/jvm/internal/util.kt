/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package kotlin.reflect.jvm.internal

import java.lang.reflect.Method
import kotlin.jvm.internal.Intrinsic

// TODO: use stdlib?
suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
private fun String.capitalizeWithJavaBeanConvention(): String {
    // The code is a bit crooked because otherwise there are overload resolution ambiguities caused by the fact
    // that we compile it with the built-ins both in source and as a compiled library
    val l = length
    if (l > 1 && Character.isUpperCase(get(1))) return this
    val first = get(0)
    this as java.lang.String
    return "" + Character.toUpperCase(first) + substring(1, l)
}

private fun getterName(propertyName: String): String = "get" + propertyName.capitalizeWithJavaBeanConvention()
private fun setterName(propertyName: String): String = "set" + propertyName.capitalizeWithJavaBeanConvention()


// A local copy of javaClass() from stdlib is needed because there's no dependency on stdlib in runtime.jvm
[Intrinsic("kotlin.javaClass.function")]
fun <reified T> javaClassOf(): Class<T> = throw UnsupportedOperationException()


private fun Class<*>.getMaybeDeclaredMethod(name: String, vararg parameterTypes: Class<*>): Method {
    try {
        return getMethod(name, *parameterTypes)
    }
    catch (e: NoSuchMethodException) {
        // This is needed to support private methods
        return getDeclaredMethod(name, *parameterTypes)
    }
}
