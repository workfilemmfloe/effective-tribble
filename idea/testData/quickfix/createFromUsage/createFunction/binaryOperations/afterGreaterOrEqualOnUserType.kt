// "Create function 'compareTo'" "true"

class A<T>(val n: T) {
    fun compareTo(t: T): Int {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

fun test() {
    A(1) >= 2
}