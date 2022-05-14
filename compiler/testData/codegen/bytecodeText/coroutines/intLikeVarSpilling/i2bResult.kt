class Controller {
    suspend fun suspendHere(): Unit = suspendWithCurrentContinuation { x ->
        x.resume(Unit)
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

private var byteResult: Byte = 0
fun setByteRes(x: Byte) {
    byteResult = x
}

fun foo(): Int = 1

fun box(): String {
    builder {
        val x: Byte = foo().toByte()
        suspendHere()
        setByteRes(x)
    }

    if (byteResult != 1.toByte()) return "fail 1"

    return "OK"
}

// 1 PUTFIELD .*\.B\$0 : B
