// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

open class A
class B : A() {
    fun foo() = Unit
}

fun myRun(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: () -> Unit) = block()

fun typecastsWithClosure() {
    var a: A = B()

    if (a is B) <!DEBUG_INFO_SMARTCAST!>a<!>.foo()

    myRun { if (a is B)  <!DEBUG_INFO_SMARTCAST!>a<!>.foo() else a = B() }

    if (a is B) myRun { <!DEBUG_INFO_SMARTCAST!>a<!>.foo() } else myRun { a = B() }

    myRun { if (a is B) myRun { if (<!USELESS_IS_CHECK!>a is B<!>) <!DEBUG_INFO_SMARTCAST!>a<!>.foo() } else a = A() }
}

