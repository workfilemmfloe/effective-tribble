@file:JsModule("lib")
@file:JsNonModule
package foo

external class A(val x: Int = noImpl) {
    fun foo(y: Int): Int = noImpl
}

external object B {
    val x: Int = noImpl

    fun foo(y: Int): Int = noImpl
}

external fun foo(y: Int): Int = noImpl

external val bar: Int = noImpl

external var mbar: Int = noImpl

fun box(): String {
    val a = A(23)
    assertEquals(23, a.x)
    assertEquals(65, a.foo(42))

    assertEquals(123, B.x)
    assertEquals(265, B.foo(142))

    assertEquals(365, foo(42))
    assertEquals(423, bar)

    mbar = 523
    assertEquals(523, mbar)

    return "OK"
}