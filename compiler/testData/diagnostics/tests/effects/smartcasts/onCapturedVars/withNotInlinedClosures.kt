// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

fun myRun(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: () -> Unit) = block()

fun unknownClosure(block: () -> Unit) = block()

fun innerSmartcastUnknownClosureBefore() {
    var x: Int? = null

    unknownClosure { x = 42 }

    myRun { if (x != null) <!SMARTCAST_IMPOSSIBLE!>x<!>.inc() }
}

fun innerSmartcastUnknownClosureAfter() {
    var x: Int? = null

    myRun { if (x != null) <!DEBUG_INFO_SMARTCAST!>x<!>.inc() }

    unknownClosure { x = 42 }
}

fun innerSmartcastInNotChangingClosure() {
    var x: Int? = null

    myRun { if (x != null) <!DEBUG_INFO_SMARTCAST!>x<!>.inc() }

    unknownClosure { if (x != null) <!DEBUG_INFO_SMARTCAST!>x<!>.inc() }
}

fun innerSmartcastInChangingClosure() {
    var x: Int? = null

    myRun { if (x != null) <!DEBUG_INFO_SMARTCAST!>x<!>.inc() }

    unknownClosure { if (x != null) <!SMARTCAST_IMPOSSIBLE!>x<!>.inc() else x = 42 }
}

fun complex() {
    var x: Int? = null

    myRun { x = 42 }

    if (x != null) x = <!DEBUG_INFO_SMARTCAST!>x<!>.inc()

    myRun { if (x == null) myRun { x = 1337 } else x = <!DEBUG_INFO_SMARTCAST!>x<!>.dec() }

    unknownClosure { if (x != null) <!SMARTCAST_IMPOSSIBLE!>x<!>.inc() }  // still not changing

    myRun {
        x = x ?: 42
        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
    }

    unknownClosure { if (x == null) x = 1337 else <!SMARTCAST_IMPOSSIBLE!>x<!>.inc() } // Now leaked into changing closure

    if (x != null) <!SMARTCAST_IMPOSSIBLE!>x<!>.inc()

    myRun { if (x != null) <!SMARTCAST_IMPOSSIBLE!>x<!>.inc() }
}


fun differentClosuresInNesting() {
    var x: Int? = null

    myRun {
        unknownClosure {    // changing unknown
            if (x != null)
                myRun { x = <!SMARTCAST_IMPOSSIBLE!>x<!>.inc() }
            else x = 42
        }
    }

    myRun { if (x != null) <!SMARTCAST_IMPOSSIBLE!>x<!>.inc() } // after unknown changing closure
}

fun differentClosuresInNesting2() {
    var x: Int? = null

    myRun {
        if (x != null) <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
        unknownClosure { if (x != null) myRun { <!SMARTCAST_IMPOSSIBLE!>x<!>.inc() } } // not a changing closure
        myRun { if (x != null) <!DEBUG_INFO_SMARTCAST!>x<!>.inc() }

        unknownClosure { myRun { x = 42 } } // changing unknown closure
        if (x != null) <!SMARTCAST_IMPOSSIBLE!>x<!>.inc()
    }

    if (x != null) <!SMARTCAST_IMPOSSIBLE!>x<!>.inc()
}