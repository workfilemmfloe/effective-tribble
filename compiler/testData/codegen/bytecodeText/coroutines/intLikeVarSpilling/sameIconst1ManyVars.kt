// WITH_RUNTIME
// WITH_COROUTINES
// TREAT_AS_ONE_FILE
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    suspend fun suspendHere(): Unit = suspendCoroutineOrReturn { x ->
        x.resume(Unit)
        SUSPENDED_MARKER
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
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
