/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package kotlin.test.adapters

/**
 * The [QUnit](http://qunitjs.com/) API
 */
internal external object QUnit {
    fun test(name: String, testFn: (dynamic) -> Unit): Unit
    fun skip(name: String, testFn: (dynamic) -> Unit): Unit
    fun only(name: String, testFn: (dynamic) -> Unit): Unit
}

/**
 * Jasmine/Mocha API
 */
internal external fun describe(name: String, fn: () -> Unit)
internal external fun xdescribe(name: String, fn: () -> Unit)
internal external fun it(name: String, fn: () -> Unit)
internal external fun xit(name: String, fn: () -> Unit)
/**
 * Jasmine-only syntax for focused spec's. Mocha uses the 'it.only' and 'describe.only' syntax
 */
internal external fun fit(name: String, fn: () -> Unit)
internal external fun fdescribe(name: String, fn: () -> Unit)

internal fun isQUnit() = jsTypeOf(QUnit) !== "undefined"

internal fun isJasmine() = js("typeof describe === 'function' && typeof it === 'function' && typeof fit === 'function'")

internal fun isMocha() = js("typeof describe === 'function' && typeof it === 'function' && typeof it.only === 'function'")
