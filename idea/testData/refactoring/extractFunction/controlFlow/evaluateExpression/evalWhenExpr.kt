// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// SIBLING:
fun foo(a: Int): Int {
    val b: Int = 1
    return <selection>when (a + b) {
        0 -> b
        1 -> -b
        else -> a - b
    }</selection>
}