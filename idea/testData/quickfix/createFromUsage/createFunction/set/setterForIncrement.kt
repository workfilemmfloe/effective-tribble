// "Create member function 'set'" "true"
class A {
    operator fun get(s: String): Int = 1
}

fun foo() {
    var a = A()
    a<caret>["1"]++
}
