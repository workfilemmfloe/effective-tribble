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

import kotlin.test.FrameworkAdapter
import kotlin.test.assertHook

/**
 * QUnit adapter
 */
internal class QUnit1Adapter: BareAdapter() {

    override fun runTest(testFn: () -> Unit,
                         names: Sequence<String>,
                         ignored: Boolean,
                         focused: Boolean,
                         shouldRun: Boolean) {
        val fn = wrapTest(testFn)
        val name = names.fold("") { acc, name -> acc + " " + name  }
        when {
            focused -> QUnit.only(name, fn)
            ignored -> QUnit.skip(name, fn)
            else -> QUnit.test(name, fn)
        }
    }

    private fun wrapTest(testFn: () -> Unit): (dynamic) -> Unit = { assert ->
        assertHook = { testResult ->
          assert.ok(testResult.result, testResult.lazyMessage())
        }
        testFn()
    }
}