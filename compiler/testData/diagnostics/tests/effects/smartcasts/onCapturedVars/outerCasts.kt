// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

fun myRun(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: () -> Unit) = block()


fun closureBefore() {
    var x: Int? = null

    myRun { x = 42 }

    if (x != null) <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
}

fun closureAfter() {
    var x: Int? = null

    if (x != null) <!DEBUG_INFO_SMARTCAST!>x<!>.inc()

    myRun { x = 42 }
}