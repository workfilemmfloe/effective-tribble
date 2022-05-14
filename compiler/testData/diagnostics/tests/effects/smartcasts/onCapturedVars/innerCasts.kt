// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

fun myRun(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: () -> Unit) = block()

fun basicInnerCast() {
    var x: Int? = null

    myRun { if (x == null) x = 42 else x = <!DEBUG_INFO_SMARTCAST!>x<!>.inc() }
}

fun inControlStructure() {
    var x: Int? = null

    if (x != null) myRun { x = <!DEBUG_INFO_SMARTCAST!>x<!>.inc() } else myRun { x = 42 }

    if (x != null) x = <!DEBUG_INFO_SMARTCAST!>x<!>.inc() else myRun { x = 42 }

    if (x != null) myRun { x = <!DEBUG_INFO_SMARTCAST!>x<!>.inc() } else x = 42

    myRun { if (<!SENSELESS_COMPARISON!>x != null<!>)  x = <!DEBUG_INFO_SMARTCAST!>x<!>.inc() else x = 42 }
}