// KT-10671 Functions must be explicitly cast as Serializable

import java.io.Serializable

fun foo(bar: Serializable): Any? = bar

fun test() {
    foo({})
    foo(fun(s: String): String = s)
    foo(::foo)
}
