// "Create function 'contains'" "true"

class A<T>(val n: T) {
    fun contains(t: T): Boolean {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun test() {
    2 !in A(1)
}