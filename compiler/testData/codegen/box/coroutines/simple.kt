class Controller {
    suspend fun suspendHere(): String = suspendWithCurrentContinuation { x ->
        x.resume("OK")
        Suspend
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }

    return result
}
