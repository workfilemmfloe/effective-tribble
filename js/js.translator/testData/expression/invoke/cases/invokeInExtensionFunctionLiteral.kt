package foo

fun box(): Boolean {
    val v1 = 1.{ Int.(x: Int) -> this + x }(2)

    val f = { Int.(x: Int) -> this + x }
    val v2 = 1.(f)(2)

    return v1 == 3 && v2 == 3
}