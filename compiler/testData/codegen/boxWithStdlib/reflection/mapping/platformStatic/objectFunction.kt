import kotlin.platform.platformStatic as static
import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

object O {
    @static fun foo(s: String): Int = s.length()
}

fun box(): String {
    val foo = O::foo

    val j = foo.javaMethod ?: return "Fail: no Java method found for O::foo"
    assertEquals(3, j.invoke(null, "abc"))

    val k = j.kotlinFunction ?: return "Fail: no Kotlin function found for Java method O::foo"
    assertEquals(3, k.call(O, "def"))

    return "OK"
}
