// "Add non-null asserted (!!) call" "true"

interface A<out T>
class B<out T>: A<T>

open class C
open class D: C()

fun test() {
    val s: B<D>? = B()
    other(<caret>s)
}

fun other(s: A<C>) {}