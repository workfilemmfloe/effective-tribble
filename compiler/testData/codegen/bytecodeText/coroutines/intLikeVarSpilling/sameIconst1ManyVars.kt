class Controller {
    suspend fun suspendHere(x: Continuation<Unit>) {
        x.resume(Unit)
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

private var result: String = ""
fun setRes(x: Byte, y: Int) {
    result = "$x#$y"
}

fun foo(): Int = 1

fun box(): String {
    builder {
        val x: Byte = 1
        // No actual cast happens here
        val y: Int = x.toInt()
        suspendHere()
        setRes(x, y)
    }

    if (result != "1#1") return "fail 1"

    return "OK"
}

// 1 PUTFIELD .*\.B\$0 : B
// 1 PUTFIELD .*\.I\$0 : I
