// "Replace Class<T> with KClass<T> in whole annotation" "true"
// WITH_RUNTIME

import kotlin.reflect.KClass

annotation class Ann(val arg1: KClass<*>, val arg2: KClass<out Any?>)

Ann(String::class, Int::class) class MyClass
