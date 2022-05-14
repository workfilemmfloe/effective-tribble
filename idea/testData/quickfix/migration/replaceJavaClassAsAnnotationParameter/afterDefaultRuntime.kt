// "Replace Class<T> with KClass<T> in whole annotation" "true"
// WITH_RUNTIME

import kotlin.reflect.KClass

annotation class Ann(
    val arg1: Int,
    val arg2: KClass<*> = Int::class,
    val arg3: Array<KClass<out Any?>> = array(String::class),
    vararg val arg4: KClass<out Any?> = array(Double::class)
)

Ann(arg1 = 1) class MyClass1
Ann(arg1 = 2, arg2 = Boolean::class) class MyClass2
Ann(arg1 = 3, arg3 = array(Boolean::class)) class MyClass3
Ann(arg1 = 4, arg4 = String::class) class MyClass4
