// "Remove initializer from property" "true"
package a

public fun emptyList<T>(): List<T> = null!!

class M {
    interface A {
        val l = <caret>emptyList<Int>()
    }
}