// !DIAGNOSTICS: -UNUSED_PARAMETER
import kotlin.coroutines.*

fun nonSuspend() {}

suspend fun baz(): Int = 1

suspend fun tryCatch(): Int {
    return try {
        CoroutineIntrinsics.<!SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE!>suspendCoroutineOrReturn { x: Continuation<Int> -> }<!>
    } catch (e: Exception) {
        <!SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE!>baz()<!> // another suspend function
    }
}

suspend fun tryFinally(): Int {
    return try {
        CoroutineIntrinsics.<!SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE!>suspendCoroutineOrReturn { x: Continuation<Int> -> }<!>
    } finally {
        nonSuspend()
    }
}

suspend fun returnInFinally(): Int {
    try {
    } finally {
        // Probably this is too restrictive, but it does not matter much
        return <!SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE, SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE!>baz()<!>
    }
}

suspend fun tryCatchFinally(): Int {
    return try {
        CoroutineIntrinsics.<!SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE!>suspendCoroutineOrReturn { x: Continuation<Int> -> }<!>
    } catch (e: Exception) {
        <!SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE!>baz()<!> // another suspend function
    } finally {
        <!SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE, SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE!>baz()<!>
    }
}
