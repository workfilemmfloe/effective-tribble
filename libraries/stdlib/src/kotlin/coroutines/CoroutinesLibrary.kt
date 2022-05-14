@file:kotlin.jvm.JvmName("CoroutinesLibraryKt")
@file:kotlin.jvm.JvmVersion
package kotlin.coroutines

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import java.lang.IllegalStateException
import kotlin.coroutines.CoroutineIntrinsics.SUSPENDED

/**
 * Creates coroutine with receiver type [R] and result type [T].
 * This function creates a new, fresh instance of suspendable computation every time it is invoked.
 * To start executing the created coroutine, invoke `resume(Unit)` on the returned [Continuation] instance.
 * The [completion] continuation is invoked when coroutine completes with result of exception.
 * An optional [dispatcher] may be specified to customise dispatch of continuations between suspension points inside the coroutine.
 */
@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
public fun <R, T> (suspend R.() -> T).createCoroutine(
        receiver: R,
        completion: Continuation<T>,
        dispatcher: ContinuationDispatcher? = null
): Continuation<Unit> = (this as (R, Continuation<T>) -> Continuation<Unit>).invoke(receiver, withDispatcher(completion, dispatcher))

/**
 * Starts coroutine with receiver type [R] and result type [T].
 * This function creates and start a new, fresh instance of suspendable computation every time it is invoked.
 * The [completion] continuation is invoked when coroutine completes with result of exception.
 * An optional [dispatcher] may be specified to customise dispatch of continuations between suspension points inside the coroutine.
 */
@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
public fun <R, T> (suspend R.() -> T).startCoroutine(
        receiver: R,
        completion: Continuation<T>,
        dispatcher: ContinuationDispatcher? = null
) {
    createCoroutine(receiver, completion, dispatcher).resume(Unit)
}

/**
 * Creates coroutine without receiver and with result type [T].
 * This function creates a new, fresh instance of suspendable computation every time it is invoked.
 * To start executing the created coroutine, invoke `resume(Unit)` on the returned [Continuation] instance.
 * The [completion] continuation is invoked when coroutine completes with result of exception.
 * An optional [dispatcher] may be specified to customise dispatch of continuations between suspension points inside the coroutine.
 */
@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
public fun <T> (suspend () -> T).createCoroutine(
        completion: Continuation<T>,
        dispatcher: ContinuationDispatcher? = null
): Continuation<Unit> = (this as (Continuation<T>) -> Continuation<Unit>).invoke(withDispatcher(completion, dispatcher))

/**
 * Starts coroutine without receiver and with result type [T].
 * This function creates and start a new, fresh instance of suspendable computation every time it is invoked.
 * The [completion] continuation is invoked when coroutine completes with result of exception.
 * An optional [dispatcher] may be specified to customise dispatch of continuations between suspension points inside the coroutine.
 */
@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
public fun <T> (suspend  () -> T).startCoroutine(
        completion: Continuation<T>,
        dispatcher: ContinuationDispatcher? = null
) {
    createCoroutine(completion, dispatcher).resume(Unit)
}

/**
 * Obtains the current continuation instance inside suspend functions and suspends
 * currently running coroutine.
 *
 * In this function both [Continuation.resume] and [Continuation.resumeWithException] can be used either synchronously in
 * the same stack-frame where suspension function is run or asynchronously later in the same thread or
 * from a different thread of execution. Repeated invocation of any resume function produces [IllegalStateException].
 */
@SinceKotlin("1.1")
public inline suspend fun <T> suspendCoroutine(crossinline block: (Continuation<T>) -> Unit): T =
        CoroutineIntrinsics.suspendCoroutineOrReturn { c: Continuation<T> ->
            val safe = SafeContinuation(c)
            block(safe)
            safe.getResult()
        }

// INTERNAL DECLARATIONS

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")
private fun <T> withDispatcher(completion: Continuation<T>, dispatcher: ContinuationDispatcher?): Continuation<T> {
    return if (dispatcher == null) completion else
        object : kotlin.jvm.internal.DispatchedContinuation<T>, Continuation<T> by completion {
            override val dispatcher: ContinuationDispatcher? = dispatcher
        }
}

private val UNDECIDED: Any? = Any()
private val RESUMED: Any? = Any()
private class Fail(val exception: Throwable)

@PublishedApi
internal class SafeContinuation<in T> @PublishedApi internal constructor(private val delegate: Continuation<T>) : Continuation<T> {
    @Volatile
    private var result: Any? = UNDECIDED

    companion object {
        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        private val RESULT_UPDATER = AtomicReferenceFieldUpdater.newUpdater<SafeContinuation<*>, Any?>(
                SafeContinuation::class.java, Any::class.java as Class<Any?>, "result")
    }

    private fun cas(expect: Any?, update: Any?): Boolean =
            RESULT_UPDATER.compareAndSet(this, expect, update)

    override fun resume(value: T) {
        while (true) { // lock-free loop
            val result = this.result // atomic read
            when (result) {
                UNDECIDED -> if (cas(UNDECIDED, value)) return
                SUSPENDED -> if (cas(SUSPENDED, RESUMED)) {
                    delegate.resume(value)
                    return
                }
                else -> throw IllegalStateException("Already resumed")
            }
        }
    }

    override fun resumeWithException(exception: Throwable) {
        while (true) { // lock-free loop
            val result = this.result // atomic read
            when (result) {
                UNDECIDED -> if (cas(UNDECIDED, Fail(exception))) return
                SUSPENDED -> if (cas(SUSPENDED, RESUMED)) {
                    delegate.resumeWithException(exception)
                    return
                }
                else -> throw IllegalStateException("Already resumed")
            }
        }
    }

    @PublishedApi
    internal fun getResult(): Any? {
        var result = this.result // atomic read
        if (result == UNDECIDED) {
            if (cas(UNDECIDED, SUSPENDED)) return SUSPENDED
            result = this.result // reread volatile var
        }
        when (result) {
            RESUMED -> return SUSPENDED // already called continuation, indicate SUSPENDED upstream
            is Fail -> throw result.exception
            else -> return result // either SUSPENDED or data
        }
    }
}
