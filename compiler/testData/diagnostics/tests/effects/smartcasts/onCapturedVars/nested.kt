// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

fun myRun(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: () -> Unit) = block()


fun innerNestedSmartcast() {
    var x: Int? = null

    myRun { if (x == null) myRun { x = 1337 } else x = <!DEBUG_INFO_SMARTCAST!>x<!>.dec() }
}

fun nestedClosureOuterCast() {
    var x: Int? = null
    myRun { myRun { x = 42 } }

    if (x != null) <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
}

fun nestedClosureInnerCast() {
    var x: Int? = null

    myRun { myRun { if (x != null) <!DEBUG_INFO_SMARTCAST!>x<!>.inc() else x = 42 } }
}

fun nestedClosureBothCasts() {
    var x: Int? = null

    myRun { myRun { if (x != null) <!DEBUG_INFO_SMARTCAST!>x<!>.inc() else x = 42 } }

    if (x != null) <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
}

fun nestedComplex1() {
    var x: Int? = null

    myRun {
        if (x != null)
            myRun { x = <!DEBUG_INFO_SMARTCAST!>x<!>.inc() }
        else
            myRun { x = <!DEBUG_INFO_CONSTANT!>x<!><!UNSAFE_CALL!>.<!>inc() }
    }
}

fun nestedComplex2() {
    var x: Int? = null

    myRun {
        x = 42
        myRun { if (<!SENSELESS_COMPARISON!>x == null<!>) x = 42 }
    }

    myRun {
        if (x != null)
            myRun { x = <!DEBUG_INFO_SMARTCAST!>x<!>.inc() }
        else x = 42
    }

    myRun { x = null }
}