// "Create function 'foo'" "true"

import kotlin.properties.ReadWriteProperty

class A<T>(val t: T) {
    var x: A<Int> by foo(t, "")

    fun foo(t: T, s: String): ReadWriteProperty<A<T>, A<Int>> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
