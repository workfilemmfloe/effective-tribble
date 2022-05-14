class Controller {
    suspend fun suspendHere(x: Continuation<Unit>) {
        x.resume(Unit)
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

private var booleanResult = false
fun setBooleanRes(x: Boolean, ignored: Unit) {
    booleanResult = x
}

fun box(): String {
    builder {
        // 'true' value is spilled into variable and saved to field before suspension point
        // It's important that there is no type info about this variable in local var table,
        // so we should infer that ICONST_1 is a boolean value from it's usage
        setBooleanRes(true, suspendHere())
    }

    if (!booleanResult) return "fail 1"

    return "OK"
}
