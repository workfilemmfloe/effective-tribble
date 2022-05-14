package foo

internal external class A(val v: String)

internal class B {
    fun bar(a: A, extLambda: A.(Int, String) -> String): String = a.extLambda(7, "_rr_")
}


internal external fun nativeBox(b: B): String = noImpl

fun box(): String {
    val r = nativeBox(B())
    if (r != "foo_rr_7") return r

    return "OK"
}