// LANGUAGE_VERSION: 1.3
// IGNORE_BACKEND: JS_IR
// TARGET_BACKEND: JVM
// WITH_REFLECT
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class A<T : String> {
    suspend fun foo() {}

    suspend fun bar(): T {
        foo()
        return suspendCoroutineOrReturn { x ->
            x.resume(x.toString() as T)
            COROUTINE_SUSPENDED
        }
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = A<String>().bar()
    }

    return if (result == "(kotlin.coroutines.Continuation<T>) -> kotlin.Any?") "OK" else "Fail: $result"
}
