// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var result = 0

class Controller {
    suspend fun suspendHere(): String = suspendCoroutineOrReturn { x ->
        result++
        x.resume("OK")
        SUSPENDED_MARKER
    }
}


fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

fun box(): String {

    for (i in 1..3) {
        builder {
            if (suspendHere() != "OK") throw RuntimeException("fail 1")
        }
    }

    if (result != 3) return "fail 2: $result"

    return "OK"
}
