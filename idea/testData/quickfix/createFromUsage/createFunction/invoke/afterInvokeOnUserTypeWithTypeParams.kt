// "Create function 'invoke' from usage" "true"

class A<T>(val n: T) {
    fun <V> invoke(u: T, s: String): B<V> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class B<T>(val m: T)

fun test<U, V>(u: U): B<V> {
    return A(u)(u, "u")
}