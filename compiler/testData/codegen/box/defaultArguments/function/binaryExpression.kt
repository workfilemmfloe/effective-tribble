class A

operator fun A.plus(i: A, ok: String = "OK") = ok

fun box() = A() + A()
