// "Create function 'foo'" "true"

class A<T>(val n: T) {
    fun foo(u: T): A<T> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun test<U>(u: U) {
    val a: A<U> = A(u).foo(u)
}