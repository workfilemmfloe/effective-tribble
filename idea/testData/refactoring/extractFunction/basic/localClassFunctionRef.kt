// SIBLING:
fun foo(a: Int): Int {
    class A {
        fun bar(): Int = a + 10
    }

    return <selection>A().bar()</selection>
}