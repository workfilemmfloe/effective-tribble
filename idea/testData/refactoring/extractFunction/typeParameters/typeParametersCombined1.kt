// PARAM_TYPES: A<T>
// PARAM_TYPES: A.B<U>
// PARAM_TYPES: V, Data
open class Data(val x: Int)

// SIBLING:
class A<T: Data>(val t: T) {
    inner class B<U: Data>(val u: U) {
        fun foo<V: Data>(v: V): Int {
            return <selection>t.x + u.x + v.x</selection>
        }
    }
}