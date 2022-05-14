trait A
trait B : A {
    fun foo()
}

fun bar1(a: A, var b: B) {
    b = a as B
    a.foo()
    b.foo()
}

fun id(b: B) = b
fun bar2(a: A, var b: B) {
    b = id(a as B)
    a.foo()
    b.foo()
}
