// OPTIONS: true, false, false, false, true
// PARAM_DESCRIPTOR: local final fun kotlin.Int.bar(m: kotlin.Int): Int defined in foo
// PARAM_DESCRIPTOR: value-parameter val n: kotlin.Int defined in foo
// PARAM_TYPES: kotlin.Int.(kotlin.Int) -> Int
// PARAM_TYPES: kotlin.Int
fun foo(n: Int): Int {
    fun Int.bar(m: Int) = this * m * n

    return <selection>n bar (n + 1)</selection>
}