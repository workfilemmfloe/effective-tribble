// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int, Number, Comparable<Int>, java.io.Serializable, Any
// PARAM_DESCRIPTOR: value-parameter val a: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: val b: kotlin.Int defined in foo
fun bar(a: Int): Int {
    println(a)
    return a + 10
}

// SIBLING:
fun foo(a: Int) {
    val b: Int = 1

    <selection>if(a > 0) bar(a) else b</selection>
}
