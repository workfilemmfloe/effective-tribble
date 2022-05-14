fun foo(s: String) {}

class A {
    fun bar(): String = ""
}

fun A.baz() {}


fun box(): String {
    val f = "${::foo}"
    if (f != "jet.KFunctionImpl1<java.lang.String, jet.Unit>") return "Fail foo: $f"

    val nameOfA = (A() as java.lang.Object).getClass().getName()

    val g = "${A::bar}"
    if (g != "jet.KMemberFunctionImpl0<$nameOfA, java.lang.String>") return "Fail bar: $g"

    val h = "${A::baz}"
    if (h != "jet.KExtensionFunctionImpl0<$nameOfA, jet.Unit>") return "Fail baz: $h"

    return "OK"
}
