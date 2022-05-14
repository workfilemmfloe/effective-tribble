// WITH_REFLECT

import kotlin.reflect.*

class A {
    var String.id: String
        get() = this
        set(value) {}

    fun Int.foo(): Double = toDouble()
}

fun box(): String {
    val p = A::class.java.kotlin.memberExtensionProperties.single()
    return if ("$p" == "var A.(kotlin.String.)id: kotlin.String") "OK" else "Fail $p"

    val q = A::class.java.kotlin.declaredFunctions.single()
    if ("$q" != "fun A.(kotlin.Int.)foo(): kotlin.Double") return "Fail q $q"

    return "OK"
}
