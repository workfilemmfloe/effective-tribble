// "Create function 'foo'" "true"

class A<T>(val n: T) {
    fun foo(t: T?): A<Int> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun test() {
    val a: A<Int> = A(true).foo(false as Boolean?)
}