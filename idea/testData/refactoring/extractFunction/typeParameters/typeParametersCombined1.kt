// PARAM_TYPES: A<T>
// PARAM_TYPES: A.B<U>
// PARAM_TYPES: V, Data
// PARAM_DESCRIPTOR: internal final class A<T : Data> defined in root package
// PARAM_DESCRIPTOR: internal final inner class B<U : Data> defined in A
// PARAM_DESCRIPTOR: value-parameter val v: V defined in A.B.foo
open class Data(val x: Int)

// SIBLING:
class A<T: Data>(val t: T) {
    inner class B<U: Data>(val u: U) {
        fun foo<V: Data>(v: V): Int {
            return <selection>t.x + u.x + v.x</selection>
        }
    }
}