package target

class A(val a: A) {
    val klass = javaClass<A>()
    val aa = A(a)
}