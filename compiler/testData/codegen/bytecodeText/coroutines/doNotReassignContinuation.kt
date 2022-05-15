// LANGUAGE_VERSION: 1.3
// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
// TREAT_AS_ONE_FILE
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
suspend fun suspendHere(): String = suspendCoroutineOrReturn { x ->
    x.resume("OK")
}

suspend fun suspendThere(param: Int, param2: String, param3: Long): String {
    val a = suspendHere()
    val b = suspendHere()
    return a + b
}

// 1 ASTORE 4
