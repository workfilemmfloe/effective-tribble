// "Replace 'if' expression with elvis expression" "true"

data class IntPair(val first: Int?, val second: Int?)

fun f(pair: IntPair): Int {
    val (x, y) = pair
    return if (x != <caret>null) x else 5
}