// WITH_STDLIB
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendHere(ctx: CoroutineContext) = suspendCoroutineUninterceptedOrReturn<String> { x ->
    if (x.context == ctx) x.resume("OK") else x.resume("FAIL")
}

suspend fun mustBeTailCall(): String {
    return suspendHere(coroutineContext)
}

suspend fun retrieveCoroutineContext(): CoroutineContext =
    suspendCoroutineUninterceptedOrReturn { cont -> cont.context }

suspend fun notTailCall(): String {
    return suspendHere(retrieveCoroutineContext())
}
