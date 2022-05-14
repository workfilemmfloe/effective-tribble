//KT-2643 Support multi-declarations in Data-Flow analysis
package n

class C {
    fun component1() = 1
    fun component2() = 2
}

fun test1(c: C) {
    val (<!UNUSED_VARIABLE!>a<!>, <!UNUSED_VARIABLE!>b<!>) = c
}

fun test2(c: C) {
    val (a, <!UNUSED_VARIABLE!>b<!>) = c
    a + 3
}

fun test3(c: C) {
    var (<!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>a<!>, <!UNUSED_VARIABLE!>b<!>) = c
    a = <!UNUSED_VALUE!>3<!>
}

fun test4(c: C) {
    var (<!VARIABLE_WITH_REDUNDANT_INITIALIZER!>a<!>, <!UNUSED_VARIABLE!>b<!>) = c
    a = 3
    a + 1
}