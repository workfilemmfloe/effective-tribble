// "Create function 'inc' from usage" "true"

class A<T>(val n: T) {
    fun inc(): A<T> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun test() {
    var a = A(1)
    a++
}