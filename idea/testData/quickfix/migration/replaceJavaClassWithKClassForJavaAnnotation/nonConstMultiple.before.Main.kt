// "Replace javaClass<T>() with T::class" "true"
// ERROR: An annotation parameter must be a `javaClass<T>()` call
// WITH_RUNTIME

val jClass = javaClass<String>()
Ann(jClass, javaClass<Int>()<caret>) class MyClass1
