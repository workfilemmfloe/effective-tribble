class A {
    fun component1() : Int = 1
    fun component2() : Int = 2
}

fun a(aa : A?, b : Any) {
    if (aa != null) {
        val (<!UNUSED_VARIABLE!>a1<!>, <!UNUSED_VARIABLE!>b1<!>) = aa;
    }

    if (b is A) {
        val (<!UNUSED_VARIABLE!>a1<!>, <!UNUSED_VARIABLE!>b1<!>) = b;
    }
}