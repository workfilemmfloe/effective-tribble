// WITH_RUNTIME
// WITH_COROUTINES
// WITH_REFLECT
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    suspend fun runInstanceOf(): Boolean = suspendCoroutineOrReturn { x ->
        val y: Any = x
        x.resume(x is Continuation<*>)
        SUSPENDED_MARKER
    }

    suspend fun runCast(): Boolean = suspendCoroutineOrReturn { x ->
        val y: Any = x
        x.resume(Continuation::class.isInstance(y as Continuation<*>))
        SUSPENDED_MARKER
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = runInstanceOf().toString() + "," + runCast().toString()
    }

    if (result != "true,true") return "fail: $result"

    return "OK"
}
