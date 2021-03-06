
fun test(): () -> Throwable {
    return try {
        TODO()
    } catch (e: Throwable) {
        { -> e }
    }
}

fun box(): String {
    val exception = test()()
    return if (exception is NotImplementedError) "OK" else "fail: $exception"
}