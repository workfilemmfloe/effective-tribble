package i

val <T> List<T>.length = <!UNRESOLVED_REFERENCE!>size<!>()

val <T> List<T>.length1 : Int get() = size()

val String.bd = <!NO_THIS!>this<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!> "!"

val String.bd1 : String get() = this + "!"


class A {
    val ii : Int = 1
}

val A.foo = <!UNRESOLVED_REFERENCE!>ii<!>

val A.foo1 : Int get() = ii


class C {
    inner class D {}
}

val C.foo : C.D = <!UNRESOLVED_REFERENCE!>D<!>()

val C.bar : C.D = C().D()

val C.foo1 : C.D get() = D()