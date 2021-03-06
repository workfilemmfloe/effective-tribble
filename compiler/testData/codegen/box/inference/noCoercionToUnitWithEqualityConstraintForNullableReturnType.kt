
class Inv<T>(val x: T?)

fun <R> foo(f: () -> R?): Inv<R> {
    val r = f()
    if (r != null) throw Exception("fail, result is not null: $r")
    return Inv(r)
}

fun box(): String {
    val r: Inv<Unit> = foo { if (false) Unit else null }
    return "OK"
}
