fun Int.component1() = "a"

fun foo(a: Number) {
    val (x) = a as Int
    a : Int
    x : String
}
