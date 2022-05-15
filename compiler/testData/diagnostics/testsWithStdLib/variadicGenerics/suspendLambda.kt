// !LANGUAGE: +NewInference +VariadicGenerics
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNCHECKED_CAST

class Box<T>(val value: T)

fun <R, vararg Ts> variadic (
    vararg arguments: *Box<Ts>,
    transform: (*Ts) -> R
): Box<R> {
    val args = Tuple<Ts>(arguments.size)
    for (i in 0 until arguments.size) {
        args[i] = arguments.get<Box<Any?>>(i).value
    }
    return Box(transform(args))
}

fun <T1, T2, R> Box<T1>.twoArgs(
    other: Box<T2>,
    transform: (T1, T2) -> R
): Box<R> = variadic(this, other) { a1, a2 -> transform(a1, a2) }

val v1 = Box("foo").twoArgs(Box(42)) { a1, a2 ->
    a1 + a2.inc()
}