/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.jvm.internal

import kotlin.coroutines.*

private const val INTERCEPT_BIT_SET = 1 shl 31
private const val INTERCEPT_BIT_CLEAR = INTERCEPT_BIT_SET.inv()

abstract class CoroutineImpl : RestrictedCoroutineImpl, DispatchedContinuation<Any?> {
    private val _dispatcher: ContinuationDispatcher?

    override val dispatcher: ContinuationDispatcher?
        get() = _dispatcher

    // this constructor is used to create a continuation instance for coroutine
    constructor(arity: Int, completion: Continuation<Any?>?) : super(arity, completion) {
        _dispatcher = (completion as? DispatchedContinuation<*>)?.dispatcher
    }

    override fun resume(value: Any?) {
        if (_dispatcher != null) {
            if (label and INTERCEPT_BIT_SET == 0) {
                label = label or INTERCEPT_BIT_SET
                if (_dispatcher.dispatchResume(value, this)) return
            }
            label = label and INTERCEPT_BIT_CLEAR
        }
        super.resume(value)
    }

    override fun resumeWithException(exception: Throwable) {
        if (_dispatcher != null) {
            if (label and INTERCEPT_BIT_SET == 0) {
                label = label or INTERCEPT_BIT_SET
                if (_dispatcher.dispatchResumeWithException(exception, this)) return
            }
            label = label and INTERCEPT_BIT_CLEAR
        }
        super.resumeWithException(exception)
    }
}

abstract class RestrictedCoroutineImpl : Lambda, Continuation<Any?> {
    @JvmField
    protected var completion: Continuation<Any?>?

    // label == -1 when coroutine cannot be started (it is just a factory object) or has already finished execution
    // label == 0 in initial part of the coroutine
    @JvmField
    protected var label: Int

    // this constructor is used to create a continuation instance for coroutine
    constructor(arity: Int, completion: Continuation<Any?>?) : super(arity) {
        this.completion = completion
        label = if (completion != null) 0 else -1
    }

    override fun resume(value: Any?) {
        try {
            val result = doResume(value, null)
            if (result != CoroutineIntrinsics.SUSPENDED)
                completion!!.resume(result)
        } catch (e: Throwable) {
            completion!!.resumeWithException(e)
        }
    }

    override fun resumeWithException(exception: Throwable) {
        try {
            val result = doResume(null, exception)
            if (result != CoroutineIntrinsics.SUSPENDED)
                completion!!.resume(result)
        } catch (e: Throwable) {
            completion!!.resumeWithException(e)
        }
    }

    protected abstract fun doResume(data: Any?, exception: Throwable?): Any?
}

internal interface DispatchedContinuation<in T> : Continuation<T> {
    val dispatcher: ContinuationDispatcher?
}
