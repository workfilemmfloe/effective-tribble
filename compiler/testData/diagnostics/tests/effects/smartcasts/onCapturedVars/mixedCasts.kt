// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

fun myRun(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: () -> Unit) = block()

fun closureBeforeAndInner() {
    var x: Int? = null

    myRun { x = 42 }

    myRun { if (x != null) <!DEBUG_INFO_SMARTCAST!>x<!>.inc() }
}

fun closureAfterAndInner() {
    var x: Int? = null

    myRun { if (x != null) <!DEBUG_INFO_SMARTCAST!>x<!>.inc() }

    myRun { x = 42 }
}

fun closureBeforeAfterAndInner() {
    var x: Int? = null

    myRun { x = 42 }

    myRun { if (x != null) <!DEBUG_INFO_SMARTCAST!>x<!>.inc() }

    myRun { x = null }
}

fun manyClosures() {
    var x: Int? = null

    myRun { x = 42 }

    myRun { if (x != null) <!DEBUG_INFO_SMARTCAST!>x<!>.inc() }

    myRun { x = null }

    myRun { if (x != null) <!DEBUG_INFO_SMARTCAST!>x<!>.inc() }

    myRun { x = null }

    if (x != null) <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
}