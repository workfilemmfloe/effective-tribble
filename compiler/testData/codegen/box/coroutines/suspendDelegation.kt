// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    suspend fun suspendHere(): String = suspendThere()

    suspend fun suspendThere(): String = suspendCoroutineOrReturn { x ->
        x.resume("OK")
        SUSPENDED_MARKER
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }

    return result
}
