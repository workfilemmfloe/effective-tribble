package foo

@native
class Function(vararg argsAndCode: String) {
    @nativeInvoke
    operator fun invoke(a: Any?): Any? = noImpl

    @nativeInvoke
    fun baz(a: Any?, b: Any?): Any? = noImpl
}

@nativeInvoke
operator fun Function.invoke(a: Any?, b: Any?): Any? = noImpl

@nativeInvoke
fun Function.bar(a: Any?, b: Any?): Any? = noImpl

object t{}

fun box(): String {
    val f = Function("a", "return a")
    val g = Function("a", "b", "return a + b")

    assertEquals(1, f(1))
    assertEquals("ok", f("ok"))
    assertEquals(t, f(t))

    assertEquals(5, g(1, 4))
    assertEquals("ok34", g("ok", 34))

    assertEquals(5, g.baz(1, 4))
    assertEquals("ok34", g.baz("ok", 34))

    assertEquals(5, g.bar(1, 4))
    assertEquals("ok34", g.bar("ok", 34))

    return "OK"
}