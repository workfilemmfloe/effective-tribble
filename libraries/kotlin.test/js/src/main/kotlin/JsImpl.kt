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

package kotlin.test

import kotlin.reflect.KClass

/**
 * Comments out a block of test code until it is implemented while keeping a link to the code
 * to implement in your unit test output
 */
impl fun todo(block: () -> Unit) {
    // println("TODO at " + (Exception() as java.lang.Throwable).getStackTrace()?.get(1) + " for " + block)
    println("TODO at " + block)
}


/** Asserts that a [block] fails with a specific exception of type [exceptionClass] being thrown. */
impl fun <T : Throwable> assertFailsWith(exceptionClass: KClass<T>, message: String?, block: () -> Unit): T {
    val exception = assertFails(message, block)
    @Suppress("INVISIBLE_MEMBER")
    assertTrue(exceptionClass.isInstance(exception), messagePrefix(message) + "Expected an exception of $exceptionClass to be thrown, but was $exception")

    @Suppress("UNCHECKED_CAST")
    return exception as T
}


/**
 * Provides the JS implementation of asserter using [QUnit](http://QUnitjs.com/)
 */
internal impl fun lookupAsserter(): Asserter = qunitAsserter

private val qunitAsserter = QUnitAsserter()

internal var assertHook: (result: Boolean, expected: Any?, actual: Any?, lazyMessage: () -> String?) -> Unit = { _, _, _, _ -> }

// TODO: make object in 1.2
class QUnitAsserter : Asserter {

    private var expected: Any? = undefined
    private var actual: Any? = undefined
    private var msg: () -> String? = { null }

    private val innerAsserter = object : Asserter {
        override fun fail(message: String?): Nothing {
            assertHook(false, expected, actual, msg)
            if (message == null)
                throw AssertionError()
            else
                throw AssertionError(message)
        }
    }

    private inline fun <T> withInfo(noinline msgFn: () -> String?,
                                    e: Any? = undefined,
                                    a: Any? = undefined,
                                    fn: Asserter.() -> T): T {
        msg = msgFn
        expected = e
        actual = a
        innerAsserter.fn()
        // If no exception was thrown
        assertHook(true, expected, actual, msg)
        // No need to clean up
    }

    override fun assertEquals(message: String?, expected: Any?, actual: Any?) {
        withInfo({ message }, expected, actual) {
            assertEquals(message, expected, actual)
        }
    }

    override fun assertNotEquals(message: String?, illegal: Any?, actual: Any?) {
        withInfo({ message }, expected, actual) {
            assertNotEquals(message, illegal, actual)
        }
    }

    override fun assertNull(message: String?, actual: Any?) {
        withInfo({ message }, null, actual) {
            assertNull(message, actual)
        }
    }

    override fun assertNotNull(message: String?, actual: Any?) {
        withInfo({ message }, a = actual) {
            assertNotNull(message, actual)
        }
    }

    override fun assertTrue(lazyMessage: () -> String?, actual: Boolean) {
        withInfo(lazyMessage, true, actual) {
            assertTrue(lazyMessage, actual)
        }
    }

    override fun assertTrue(message: String?, actual: Boolean) {
        withInfo({ message }, true, actual) {
            assertTrue(message, actual)
        }
    }

    override fun fail(message: String?): Nothing {
        withInfo({message}) {
            fail(message)
        }
    }
}