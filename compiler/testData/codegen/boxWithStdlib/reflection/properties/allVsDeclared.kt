import kotlin.reflect.*
import kotlin.test.*

open class Super {
    val a: Int = 1
    val String.b: String get() = this
}

class Sub : Super() {
    val c: Double = 1.0
    val Char.d: Char get() = this
}

fun box(): String {
    val sub = Sub::class

    assertEquals(listOf("a", "c"), sub.memberProperties.map { it.name }.sort())
    assertEquals(listOf("b", "d"), sub.memberExtensionProperties.map { it.name }.sort())
    assertEquals(listOf("c"), sub.declaredMemberProperties.map { it.name })
    assertEquals(listOf("d"), sub.declaredMemberExtensionProperties.map { it.name })

    return "OK"
}
