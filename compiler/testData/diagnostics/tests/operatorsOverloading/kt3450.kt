// !DIAGNOSTICS: -UNUSED_PARAMETER

public class A {
    public fun get(vararg attrs : Pair<String, String>) : A = this
}
fun String.plus() : A = A()
fun A.div(s : String) : A = A()

fun test() {
    (+"node2" / "node3" / "zzz") ["attr" to "value", "a2" to "v2"]
}

//---------
class B {
    public fun get(s : String, q : String) : B = this
    public fun get(s : Pair<String, String>) : B = this
    public fun invoke(q : B.() -> Unit) : B = this
}
val x = B()["a", "v"]["a" to "b"] {} ["q" to "p"] // does not parses around {}

//from library
data class Pair<out A, out B> (val first: A, val second: B)
fun <A,B> A.to(that: B) = Pair(this, that)