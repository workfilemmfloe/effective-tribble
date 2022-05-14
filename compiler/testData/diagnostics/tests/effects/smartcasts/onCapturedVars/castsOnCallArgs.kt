// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

fun <T> myRun(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: () -> T): T = block()

fun foo(<!UNUSED_PARAMETER!>x<!>: Int, <!UNUSED_PARAMETER!>y<!>: Int?, <!UNUSED_PARAMETER!>z<!>: Int): Int? = null

fun castOnCallArguments() {
    var x: Int? = null

    myRun { x = 42 }

    foo(if (x != null) <!DEBUG_INFO_SMARTCAST!>x<!> else 42, x!!, <!DEBUG_INFO_SMARTCAST!>x<!>)

    myRun { x = null }
}