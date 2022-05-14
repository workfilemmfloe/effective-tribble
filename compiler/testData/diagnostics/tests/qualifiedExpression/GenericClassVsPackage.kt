// MODULE: m1
// FILE: a.kt
package a.b

class c {
    fun ab_c() {}
}

// MODULE: m2
// FILE: b.kt
package a

class b<T> {
    class c {
        fun a_bc() {}
    }
}

// MODULE: m3(m1, m2)
// FILE: c.kt
import a.b.c

fun test(ab_c: c) {
    ab_c.ab_c()

    val ab_c2: a.b.c = a.b.c()
    ab_c2.ab_c()

    val ab_c3 = a.b.c()
    ab_c3.ab_c()
}

fun test2(a_bc: a.b<Int>.c) {
    a_bc.<!UNRESOLVED_REFERENCE!>a_bc<!>() // todo
    a_bc.ab_c() // todo
}



//---------------------------TOP LEVEL----------
// MODULE: top_m1
// FILE: top_a.kt
package a

class b {
    fun a_b() {}
}

// MODULE: top_m2
// FILE: top_b.kt
class a<T> {
    class b {
        fun _ab() {}
    }
    fun _a() {}
}

// MODULE: top_m3(top_m1, top_m2)
// FILE: top_c.kt
import a.b

fun test(a_b: b) {
    a_b.a_b()

    val _ab: a.b = a.b()
    _ab._ab()

    val _ab2 = a.b()
    _ab2.<!UNRESOLVED_REFERENCE!>_ab<!>() // todo
}

fun test2(_ab: a<Int>.b) {
    _ab._ab()
    _ab.<!UNRESOLVED_REFERENCE!>a_b<!>()
}