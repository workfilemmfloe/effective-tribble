// TARGET_BACKEND: JVM

// WITH_STDLIB

var result = "fail 1"
object Foo {
    @JvmStatic
    private val a = "OK"

    fun foo() = run { result = a }
}

fun box(): String {
    Foo.foo()

    return result
}
