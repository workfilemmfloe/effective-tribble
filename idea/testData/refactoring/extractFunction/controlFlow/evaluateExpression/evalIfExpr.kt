// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// SIBLING:
fun foo(a: Int): Int {
    val b: Int = 1
    return <selection>if (a + b > 0) 1 else if (a - b < 0) 2 else b </selection>
}