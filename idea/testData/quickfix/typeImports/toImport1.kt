// "Remove initializer from property" "true"
package a

public fun <T> emptyList(): List<T> = null!!

class M {
    interface A {
        abstract val l = <caret>emptyList<Int>()
    }
}