// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    var log = ""
    var resumeIndex = 0

    suspend fun <T> suspendWithValue(value: T): T = suspendCoroutineOrReturn { continuation ->
        log += "suspend($value);"
        continuation.resume(value)
        SUSPENDED_MARKER
    }

    suspend fun suspendWithException(value: String): Unit = suspendCoroutineOrReturn { continuation ->
        log += "error($value);"
        continuation.resumeWithException(RuntimeException(value))
        SUSPENDED_MARKER
    }
}

abstract class ContinuationDispatcher : ContinuationInterceptor {
    override val contextKey: CoroutineContextKey<*> = ContinuationInterceptor
    abstract fun <T> dispatchResume(value: T, continuation: Continuation<T>): Boolean
    abstract fun dispatchResumeWithException(exception: Throwable, continuation: Continuation<*>): Boolean
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> = DispatchedContinuation(this, continuation)
    override operator fun <E : CoroutineContextElement> get(key: CoroutineContextKey<E>): E? = if (this.contextKey == key) this as E else null
    override fun <R> fold(initial: R, operation: (R, CoroutineContextElement) -> R): R = operation(initial, this)
    override operator fun plus(context: CoroutineContext): CoroutineContext = this
    override fun minusKey(key: CoroutineContextKey<*>): CoroutineContext = if (this.contextKey == key) EmptyCoroutineContext else this
}

private class DispatchedContinuation<T>(
        val dispatcher: ContinuationDispatcher,
        val continuation: Continuation<T>
): Continuation<T> {
    override val context: CoroutineContext = continuation.context

    override fun resume(value: T) {
        if (!dispatcher.dispatchResume(value, continuation))
            continuation.resume(value)
    }

    override fun resumeWithException(exception: Throwable) {
        if (!dispatcher.dispatchResumeWithException(exception, continuation))
            continuation.resumeWithException(exception)
    }
}

fun test(c: suspend Controller.() -> Unit): String {
    val controller = Controller()
    c.startCoroutine(controller, EmptyContinuation(object: ContinuationDispatcher() {
        private fun dispatchResume(block: () -> Unit) {
            val id = controller.resumeIndex++
            controller.log += "before $id;"
            block()
            controller.log += "after $id;"
        }

        override fun <P> dispatchResume(data: P, continuation: Continuation<P>): Boolean {
            dispatchResume {
                continuation.resume(data)
            }
            return true
        }

        override fun dispatchResumeWithException(exception: Throwable, continuation: Continuation<*>): Boolean {
            dispatchResume {
                continuation.resumeWithException(exception)
            }
            return true
        }
    }))
    return controller.log
}

fun box(): String {
    var result = test {
        val o = suspendWithValue("O")
        val k = suspendWithValue("K")
        log += "$o$k;"
    }
    if (result != "before 0;suspend(O);before 1;suspend(K);before 2;OK;after 2;after 1;after 0;") return "fail1: $result"

    result = test {
        try {
            suspendWithException("OK")
            log += "ignore;"
        }
        catch (e: RuntimeException) {
            log += "${e.message};"
        }
    }
    if (result != "before 0;error(OK);before 1;OK;after 1;after 0;") return "fail2: $result"

    return "OK"
}
